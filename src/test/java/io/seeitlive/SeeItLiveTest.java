package io.seeitlive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full behavioral test matrix for the Java SDK, mirroring
 * {@code packages/client-node/src/client.test.ts} against the frozen Node
 * reference. Uses a real {@link HttpServer} stub on an ephemeral port
 * instead of mocking {@code java.net.http.HttpClient} — no live network.
 */
class SeeItLiveTest {

    private static final String KEY = "sil_live_" + "a".repeat(32);

    private HttpServer server;
    private RecordingHandler handler;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Starts a stub server that always answers with the given status/body/headers, returns the client baseUrl. */
    private String start(int status, String responseBody, Map<String, String> responseHeaders) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        handler = new RecordingHandler(status, responseBody, responseHeaders);
        server.createContext("/", handler);
        server.start();
        // baseUrl includes /v1, same as production, so /sessions resolves to /v1/sessions.
        return "http://localhost:" + server.getAddress().getPort() + "/v1";
    }

    // (a) env inference + exactly-one-credential enforcement
    @Test
    void inferEnvFromPrefixAndEnforceExactlyOneCredential() {
        SeeItLive testEnvClient = new SeeItLive(SeeItLiveConfig.builder().apiKey("sil_test_" + "a".repeat(32)).build());
        assertEquals(Env.TEST, testEnvClient.getEnv());

        SeeItLive liveEnvClient = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).build());
        assertEquals(Env.LIVE, liveEnvClient.getEnv());

        assertThrows(IllegalArgumentException.class, () -> new SeeItLive(SeeItLiveConfig.builder().build()));
        assertThrows(IllegalArgumentException.class,
                () -> new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).oauthToken("x").build()));
    }

    // (a2) empty-string credential is treated as absent (symmetric parity across SDKs)
    @Test
    void emptyStringCredentialIsTreatedAsAbsent() {
        // apiKey("") alone is equivalent to no credential at all -> throws.
        assertThrows(IllegalArgumentException.class,
                () -> new SeeItLive(SeeItLiveConfig.builder().apiKey("").build()));

        // apiKey("") plus a real oauthToken must NOT be treated as "both provided";
        // the empty apiKey is absent, so this builds successfully using the oauth token.
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey("").oauthToken(KEY).build());
        assertEquals(Env.LIVE, client.getEnv());
    }

    // (a3) same as above, but verifies the oauth token is actually what's sent as the bearer token
    @Test
    void emptyStringApiKeyWithRealOauthTokenSendsOauthBearer() throws IOException {
        String baseUrl = start(200, "{\"valid\":true,\"client_id\":\"c\",\"team_id\":\"p\",\"scopes\":[],"
                + "\"plan\":{\"planKey\":\"free\",\"planName\":\"Free\",\"sessionLimit\":null,\"seatLimit\":null,"
                + "\"concurrentLimit\":null},\"expires_at\":\"2026-01-01\"}", Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey("").oauthToken(KEY).baseUrl(baseUrl).build());

        client.oauth().verify();

        assertEquals("Bearer " + KEY, handler.authHeader);
    }

    // (b) create request shape (method/path/bearer/json body, no teamId) + response parse
    @Test
    void sessionsCreatePostsWithBearerAndJsonBodyAndParsesCamelCase() throws IOException {
        String body = "{\"id\":\"s1\",\"status\":\"pending\",\"inviteCode\":\"abc\",\"inviteUrl\":\"u\","
                + "\"inviteShortUrl\":\"https://sitl.cc/s/aB3xK9\","
                + "\"agentViewUrl\":\"a\",\"expiresAt\":\"2026-01-01T00:00:00Z\"}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        Session res = client.sessions().create(CreateSessionParams.builder(Channel.LINK).build());

        assertEquals("abc", res.inviteCode());
        assertEquals("https://sitl.cc/s/aB3xK9", res.inviteShortUrl());
        assertEquals("POST", handler.method);
        assertEquals("/v1/sessions", handler.path);
        assertEquals("Bearer " + KEY, handler.authHeader);

        Map<?, ?> parsedBody = new ObjectMapper().readValue(handler.requestBody, Map.class);
        assertEquals(Map.of("channel", "link"), parsedBody); // NO teamId, no null-optional fields
    }

    // (b2) signs requests when signingSecret is configured, and omits signature headers otherwise
    @Test
    void signsRequestsWhenSigningSecretIsConfiguredAndOmitsHeadersOtherwise() throws IOException {
        String body = "{\"id\":\"s1\",\"status\":\"pending\",\"inviteCode\":\"abc\",\"inviteUrl\":\"u\","
                + "\"agentViewUrl\":\"a\",\"expiresAt\":\"2026-01-01T00:00:00Z\"}";

        String baseUrl = start(200, body, Map.of());
        SeeItLive signed = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl)
                .signingSecret("wh_secret_" + "b".repeat(32)).build());
        signed.sessions().create(CreateSessionParams.builder(Channel.LINK).build());
        assertTrue(handler.signatureHeader != null && handler.signatureHeader.matches("sha256=[0-9a-f]{64}"));
        assertTrue(handler.timestampHeader != null && handler.timestampHeader.matches("\\d+"));
        server.stop(0);

        baseUrl = start(200, body, Map.of());
        SeeItLive unsigned = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());
        unsigned.sessions().create(CreateSessionParams.builder(Channel.LINK).build());
        assertNull(handler.signatureHeader);
        assertNull(handler.timestampHeader);
    }

    // (c) status path + parse
    @Test
    void sessionsStatusGetsStatusPathAndParses() throws IOException {
        String body = "{\"id\":\"s1\",\"status\":\"live\",\"startedAt\":\"t\",\"endedAt\":null,"
                + "\"producerCount\":1,\"hasVideo\":true}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        SessionStatus res = client.sessions().status("s1");

        assertEquals(1, res.producerCount());
        assertTrue(res.hasVideo());
        assertEquals("GET", handler.method);
        assertEquals("/v1/sessions/s1/status", handler.path);
    }

    // (d) oauth.verify snake_case -> idiomatic camelCase
    @Test
    void oauthVerifyMapsSnakeCaseWireFieldsToCamelCase() throws IOException {
        String body = "{\"valid\":true,\"client_id\":\"c\",\"team_id\":\"p\",\"scopes\":[\"sessions:write\"],"
                + "\"plan\":{\"planKey\":\"free\",\"planName\":\"Free\",\"sessionLimit\":null,\"seatLimit\":null,"
                + "\"concurrentLimit\":null},\"expires_at\":\"2026-01-01\"}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().oauthToken("sil_live_oauth_" + "a".repeat(40)).baseUrl(baseUrl).build());

        TokenInfo info = client.oauth().verify();

        assertEquals("c", info.clientId());
        assertEquals("p", info.teamId());
        assertTrue(info.scopes().contains("sessions:write"));
        assertEquals("free", info.plan().planKey());
        assertEquals("GET", handler.method);
        assertEquals("/v1/oauth/verify", handler.path);
        assertEquals("Bearer sil_live_oauth_" + "a".repeat(40), handler.authHeader);
    }

    // (e) status -> exception mapping, incl. 429 Retry-After: 12, and a legitimate 0 preserved
    @Test
    void mapsHttpStatusCodesToTypedExceptions() throws IOException {
        String baseUrl = start(401, "{\"message\":\"x\"}", Map.of());
        SeeItLive client401 = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());
        assertThrows(AuthenticationException.class, () -> client401.sessions().status("s"));
        server.stop(0);

        baseUrl = start(404, "{\"message\":\"x\"}", Map.of());
        SeeItLive client404 = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());
        assertThrows(NotFoundException.class, () -> client404.sessions().status("s"));
        server.stop(0);

        baseUrl = start(400, "{\"message\":\"x\",\"field\":\"channel\"}", Map.of());
        SeeItLive client400 = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());
        ValidationException ve = assertThrows(ValidationException.class, () -> client400.sessions().status("s"));
        assertEquals("channel", ve.getField());
        server.stop(0);

        baseUrl = start(403, "{\"message\":\"x\"}", Map.of());
        SeeItLive client403 = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());
        assertThrows(PermissionException.class, () -> client403.sessions().status("s"));
        server.stop(0);

        baseUrl = start(429, "{\"message\":\"slow\"}", Map.of("Retry-After", "12"));
        SeeItLive client429 = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());
        RateLimitException rle = assertThrows(RateLimitException.class, () -> client429.sessions().status("s"));
        assertEquals(Integer.valueOf(12), rle.getRetryAfter());
        server.stop(0);

        // A legitimate Retry-After: 0 ("retry now") must be preserved, not coerced to absent.
        baseUrl = start(429, "{\"message\":\"slow\"}", Map.of("Retry-After", "0"));
        SeeItLive client4290 = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());
        RateLimitException rleZero = assertThrows(RateLimitException.class, () -> client4290.sessions().status("s"));
        assertEquals(Integer.valueOf(0), rleZero.getRetryAfter());
        assertFalse(rleZero.getRetryAfter() == null);
    }

    // (f) exchangeCode: standalone, no auth header, correct grant type, token mapped
    @Test
    void exchangeCodePostsOauthTokenWithoutAuthHeaderAndMapsToken() throws IOException {
        String body = "{\"access_token\":\"sil_live_oauth_x\",\"token_type\":\"Bearer\",\"client_id\":\"c\","
                + "\"team_id\":\"p\",\"scopes\":[],\"plan\":{\"planKey\":\"free\",\"planName\":\"Free\","
                + "\"sessionLimit\":null,\"seatLimit\":null,\"concurrentLimit\":null}}";
        String baseUrl = start(200, body, Map.of());

        TokenResponse tok = SeeItLive.exchangeCode(ExchangeCodeParams.builder()
                .code("code123").clientId("c").clientSecret("s").redirectUri("https://x/cb").baseUrl(baseUrl).build());

        assertEquals("sil_live_oauth_x", tok.accessToken());
        assertEquals("POST", handler.method);
        assertEquals("/v1/oauth/token", handler.path);
        assertNull(handler.authHeader);

        Map<?, ?> parsedBody = new ObjectMapper().readValue(handler.requestBody, Map.class);
        assertEquals("authorization_code", parsedBody.get("grant_type"));
        assertFalse(parsedBody.containsKey("code_verifier")); // omitted when null
        server.stop(0);

        // An empty-string codeVerifier must also be omitted, not sent as "".
        baseUrl = start(200, body, Map.of());
        SeeItLive.exchangeCode(ExchangeCodeParams.builder()
                .code("code123").clientId("c").clientSecret("s").redirectUri("https://x/cb")
                .codeVerifier("").baseUrl(baseUrl).build());
        Map<?, ?> parsedEmptyBody = new ObjectMapper().readValue(handler.requestBody, Map.class);
        assertFalse(parsedEmptyBody.containsKey("code_verifier")); // omitted when empty string
        server.stop(0);

        // A present codeVerifier must be included.
        baseUrl = start(200, body, Map.of());
        SeeItLive.exchangeCode(ExchangeCodeParams.builder()
                .code("code123").clientId("c").clientSecret("s").redirectUri("https://x/cb")
                .codeVerifier("verifier123").baseUrl(baseUrl).build());
        Map<?, ?> parsedPresentBody = new ObjectMapper().readValue(handler.requestBody, Map.class);
        assertEquals("verifier123", parsedPresentBody.get("code_verifier"));
    }

    /** Records the last request handled and answers every request with a fixed status/body/headers. */
    private static final class RecordingHandler implements HttpHandler {
        volatile String method;
        volatile String path;
        volatile String query;
        volatile String authHeader;
        volatile String requestBody;
        volatile String signatureHeader;
        volatile String timestampHeader;

        private final int status;
        private final String responseBody;
        private final Map<String, String> responseHeaders;

        RecordingHandler(int status, String responseBody, Map<String, String> responseHeaders) {
            this.status = status;
            this.responseBody = responseBody;
            this.responseHeaders = responseHeaders;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            method = exchange.getRequestMethod();
            // getRawPath(), NOT getPath() — the latter percent-decodes, which would
            // silently collapse an encoded "%2F" back into "/" and hide exactly the
            // kind of path-injection bug an id-escaping test exists to catch.
            path = exchange.getRequestURI().getRawPath();
            query = exchange.getRequestURI().getQuery();
            authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            signatureHeader = exchange.getRequestHeaders().getFirst("X-SeeItLive-Signature");
            timestampHeader = exchange.getRequestHeaders().getFirst("X-SeeItLive-Timestamp");
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            responseHeaders.forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length == 0 ? -1 : out.length);
            if (out.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(out);
                }
            } else {
                exchange.getResponseBody().close();
            }
        }
    }
    // (g) guest protection
    @Test
    void getProtectionGetsExpectedPathAndParses() throws IOException {
        String body = "{\"type\":\"totp\",\"password\":null,\"code\":\"418205\","
                + "\"secondsRemaining\":17,\"locked\":false,\"failedAttempts\":0}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        ProtectionInfo info = client.sessions().getProtection("s1");

        assertEquals("totp", info.type());
        assertNull(info.password());
        assertEquals("418205", info.code());
        assertEquals(17, info.secondsRemaining());
        assertFalse(info.locked());
        assertEquals(0, info.failedAttempts());
        assertEquals("GET", handler.method);
        assertEquals("/v1/sessions/s1/protection", handler.path);
    }

    @Test
    void setProtectionPutsExpectedPathAndBody() throws IOException {
        String body = "{\"type\":\"password\",\"password\":\"K7QMX2RT\",\"code\":null,"
                + "\"secondsRemaining\":null,\"locked\":false,\"failedAttempts\":0}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        ProtectionInfo info = client.sessions().setProtection("s1", new SetProtectionParams("password", "K7QMX2RT"));

        assertEquals("K7QMX2RT", info.password());
        assertEquals("PUT", handler.method);
        assertEquals("/v1/sessions/s1/protection", handler.path);
        assertTrue(handler.requestBody.contains("\"protectionSecret\":\"K7QMX2RT\""));
    }

    // NON_NULL inclusion is load-bearing: absence is what tells the API to
    // generate a secret, so a null must not be serialized as an explicit null.
    @Test
    void setProtectionOmitsTheSecretWhenNull() throws IOException {
        String body = "{\"type\":\"password\",\"password\":\"GENERATED\",\"code\":null,"
                + "\"secondsRemaining\":null,\"locked\":false,\"failedAttempts\":0}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        client.sessions().setProtection("s1", new SetProtectionParams("password"));

        assertFalse(handler.requestBody.contains("protectionSecret"));
        assertTrue(handler.requestBody.contains("\"protection\":\"password\""));
    }

    // (h) session read, lockout reset, agent-view token, snapshots

    @Test
    void sessionsGetReadsASessionWithItsInvitation() throws IOException {
        String body = "{\"id\":\"s1\",\"teamId\":\"t1\",\"env\":\"live\",\"status\":\"active\","
                + "\"createdByClientId\":null,\"createdByApiKeyId\":\"k1\",\"note\":null,"
                + "\"metadata\":{\"ticket\":\"42\"},\"startedAt\":\"2026-01-01T00:00:00Z\","
                + "\"endedAt\":null,\"createdAt\":\"2026-01-01T00:00:00Z\","
                + "\"invitation\":{\"code\":\"abc123\",\"channel\":\"sms\",\"recipient\":\"+15145551234\","
                + "\"expiresAt\":\"2026-01-01T01:00:00Z\",\"openedAt\":null}}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        SessionDetail session = client.sessions().get("s1");

        assertEquals("t1", session.teamId());
        assertEquals("k1", session.createdByApiKeyId());
        assertEquals("42", session.metadata().get("ticket"));
        assertEquals("abc123", session.invitation().code());
        assertEquals("sms", session.invitation().channel());
        assertEquals("+15145551234", session.invitation().recipient());
        assertNull(session.invitation().openedAt());
        assertEquals("GET", handler.method);
        assertEquals("/v1/sessions/s1", handler.path);
    }

    // A session read returns a whole stored row, so a column added server-side
    // later must not break older clients.
    @Test
    void sessionsGetIgnoresFieldsItDoesNotKnow() throws IOException {
        String body = "{\"id\":\"s1\",\"teamId\":\"t1\",\"env\":\"live\",\"status\":\"ended\","
                + "\"invitation\":null,\"somethingAddedLater\":\"surprise\"}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        SessionDetail session = client.sessions().get("s1");

        assertEquals("s1", session.id());
        assertNull(session.invitation());
    }

    @Test
    void resetProtectionPostsAndRotatesWhenAsked() throws IOException {
        String body = "{\"type\":\"password\",\"password\":\"NEWSECRET\",\"code\":null,"
                + "\"secondsRemaining\":null,\"locked\":false,\"failedAttempts\":0}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        ProtectionInfo info = client.sessions().resetProtection("s1", new ResetProtectionParams(true));

        assertEquals("NEWSECRET", info.password());
        assertFalse(info.locked());
        assertEquals("POST", handler.method);
        assertEquals("/v1/sessions/s1/protection/reset", handler.path);
        assertTrue(handler.requestBody.contains("\"rotate\":true"));
    }

    // Clearing a lockout is the common case and must not rotate by accident.
    @Test
    void resetProtectionOmitsRotateWhenNull() throws IOException {
        String body = "{\"type\":\"password\",\"password\":\"K7QMX2RT\",\"code\":null,"
                + "\"secondsRemaining\":null,\"locked\":false,\"failedAttempts\":0}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        client.sessions().resetProtection("s1", new ResetProtectionParams());

        assertFalse(handler.requestBody.contains("rotate"));
    }

    @Test
    void createAgentTokenPostsTheLabelAndParsesTheViewerUrl() throws IOException {
        String body = "{\"code\":\"vw1\",\"token\":\"tok\","
                + "\"agentViewUrl\":\"https://app.seeitlive.io/v/vw1#tok\"}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        AgentToken token = client.sessions().createAgentToken("s1", new CreateAgentTokenParams("Odoo - crm.lead 42"));

        assertEquals("vw1", token.code());
        assertEquals("tok", token.token());
        assertEquals("https://app.seeitlive.io/v/vw1#tok", token.agentViewUrl());
        assertEquals("POST", handler.method);
        assertEquals("/v1/sessions/s1/agent-token", handler.path);
        assertTrue(handler.requestBody.contains("\"label\":\"Odoo - crm.lead 42\""));
    }

    @Test
    void createAgentTokenOmitsTheLabelSoTheApiDefaultsIt() throws IOException {
        String baseUrl = start(200, "{\"code\":\"vw1\",\"token\":\"tok\",\"agentViewUrl\":\"u\"}", Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        client.sessions().createAgentToken("s1", new CreateAgentTokenParams());

        assertFalse(handler.requestBody.contains("label"));
    }

    @Test
    void snapshotsListGetsASessionsSnapshots() throws IOException {
        String body = "[{\"id\":\"sn1\",\"width\":1280,\"height\":720,"
                + "\"capturedAt\":\"2026-01-01T00:00:00Z\",\"url\":\"/api/v1/snapshots/sn1\"}]";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        List<SnapshotSummary> snapshots = client.snapshots().list("s1");

        assertEquals(1, snapshots.size());
        assertEquals("sn1", snapshots.get(0).id());
        assertEquals(1280, snapshots.get(0).width());
        assertEquals("/api/v1/snapshots/sn1", snapshots.get(0).url());
        assertEquals("/v1/sessions/s1/snapshots", handler.path);
    }

    @Test
    void snapshotsGetAsksForBase64JsonRatherThanRawPngBytes() throws IOException {
        String body = "{\"id\":\"sn1\",\"contentType\":\"image/png\",\"width\":1280,\"height\":720,"
                + "\"capturedAt\":\"2026-01-01T00:00:00Z\",\"data\":\"aVZCT1J3MEs=\"}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        SnapshotImage image = client.snapshots().get("sn1");

        assertEquals("image/png", image.contentType());
        assertEquals("aVZCT1J3MEs=", image.data());
        assertEquals(720, image.height());
        assertEquals("/v1/snapshots/sn1", handler.path);
        assertEquals("format=base64", handler.query);
    }

    @Test
    void resharePostsExpectedPathAndBody() throws IOException {
        String body = "{\"expiresAt\":\"2026-01-02T00:00:00Z\",\"inviteShortUrl\":\"https://sitl.cc/s/aB3xK9\","
                + "\"resend\":\"sent\",\"rotated\":true}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        ReshareResult result = client.sessions().reshare("s1",
                new ReshareParams(ReshareMode.NEW, Channel.SMS, "+15145550199", true));

        assertTrue(result.rotated());
        assertEquals("sent", result.resend());
        assertEquals("https://sitl.cc/s/aB3xK9", result.inviteShortUrl());
        assertEquals("POST", handler.method);
        assertEquals("/v1/sessions/s1/reshare", handler.path);

        Map<?, ?> parsedBody = new ObjectMapper().readValue(handler.requestBody, Map.class);
        assertEquals(Map.of("mode", "new", "channel", "sms", "recipient", "+15145550199", "smsConsent", true), parsedBody);
    }

    // NON_NULL inclusion is load-bearing: an omitted option must be ABSENT
    // from the body, not sent as null — absence means "reuse what was recorded".
    @Test
    void reshareWithNoParamsSendsAnEmptyBody() throws IOException {
        String body = "{\"expiresAt\":\"2026-01-02T00:00:00Z\",\"inviteShortUrl\":null,"
                + "\"resend\":\"not_requested\",\"rotated\":false}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        ReshareResult result = client.sessions().reshare("s1");

        assertNull(result.inviteShortUrl());
        Map<?, ?> parsedBody = new ObjectMapper().readValue(handler.requestBody, Map.class);
        assertEquals(Map.of(), parsedBody);
    }

    @Test
    void reshareUrlEncodesTheSessionId() throws IOException {
        String body = "{\"expiresAt\":\"2026-01-02T00:00:00Z\",\"inviteShortUrl\":null,"
                + "\"resend\":\"not_requested\",\"rotated\":false}";
        String baseUrl = start(200, body, Map.of());
        SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey(KEY).baseUrl(baseUrl).build());

        client.sessions().reshare("sess/../evil");

        assertEquals("/v1/sessions/sess%2F..%2Fevil/reshare", handler.path);
    }

}
