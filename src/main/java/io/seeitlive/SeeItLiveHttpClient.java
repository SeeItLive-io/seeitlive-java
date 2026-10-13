package io.seeitlive;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Internal HTTP transport. Wraps {@link java.net.http.HttpClient}, handles the bearer
 * {@code Authorization} header (or its absence, for the standalone code exchange),
 * JSON (de)serialization via Jackson, optional HMAC request signing (see {@link Signing}),
 * and maps non-2xx responses to the typed {@link SeeItLiveException} hierarchy —
 * behavior-identical to the Node reference SDK's {@code http.ts}.
 */
final class SeeItLiveHttpClient {

    private final String baseUrl;
    private final String authHeader; // null => no Authorization header sent
    private final String signingSecret; // null/empty => requests are not signed
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    SeeItLiveHttpClient(String baseUrl, String authHeader) {
        this(baseUrl, authHeader, null);
    }

    SeeItLiveHttpClient(String baseUrl, String authHeader, String signingSecret) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.authHeader = authHeader;
        this.signingSecret = (signingSecret == null || signingSecret.isEmpty()) ? null : signingSecret;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // Responses carry whole stored rows; a field added server-side
                // later must not break an older client.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    static String encodePathSegment(String segment) {
        // URLEncoder targets form-encoding (space -> '+'); path segments need '%20'.
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    <T> T get(String path, Class<T> responseType) {
        return request("GET", path, null, responseType);
    }

    <T> T post(String path, Object body, Class<T> responseType) {
        return request("POST", path, body, responseType);
    }

    <T> T put(String path, Object body, Class<T> responseType) {
        return request("PUT", path, body, responseType);
    }

    private <T> T request(String method, String path, Object body, Class<T> responseType) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path));
            if (authHeader != null) {
                builder.header("Authorization", authHeader);
            }
            String serializedBody = null;
            if (body != null) {
                serializedBody = mapper.writeValueAsString(body);
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(serializedBody, StandardCharsets.UTF_8));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            if (signingSecret != null) {
                Map<String, String> signatureHeaders = Signing.signRequest(
                        signingSecret, method, path, serializedBody, Instant.now().getEpochSecond());
                signatureHeaders.forEach(builder::header);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw mapError(response);
            }
            if (status == 204 || response.body() == null || response.body().isEmpty()) {
                return null;
            }
            return mapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SeeItLiveException(0, "HTTP request interrupted", null);
        }
    }

    private SeeItLiveException mapError(HttpResponse<String> response) {
        int status = response.statusCode();
        String message = "HTTP " + status;
        String code = null;
        String field = null;
        try {
            ErrorBody body = mapper.readValue(response.body(), ErrorBody.class);
            if (body.message() != null) {
                message = body.message();
            }
            code = body.code();
            field = body.field();
        } catch (Exception ignored) {
            // non-JSON or empty error body — fall back to the generic "HTTP <status>" message
        }

        switch (status) {
            case 400:
                return new ValidationException(message, field, code);
            case 401:
                return new AuthenticationException(message, code);
            case 403:
                return new PermissionException(message, code);
            case 404:
                return new NotFoundException(message, code);
            case 429: {
                Integer retryAfter = null;
                String header = response.headers().firstValue("Retry-After").orElse(null);
                if (header != null) {
                    try {
                        retryAfter = Integer.parseInt(header.trim());
                    } catch (NumberFormatException ignored) {
                        // leave retryAfter null — matches Node reference's Number.isNaN guard
                    }
                }
                return new RateLimitException(message, retryAfter, code);
            }
            default:
                return new SeeItLiveException(status, message, code);
        }
    }
}
