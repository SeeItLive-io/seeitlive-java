package io.seeitlive;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Computes the HMAC request-signature headers per the SeeItLive wire contract, identical
 * across every SDK (mirrors the Node reference's {@code signing.ts}):
 *
 * <pre>
 *   signature = HMAC_SHA256(secret, "{timestamp}.{METHOD}.{path}.{sha256hex(rawBody)}")
 * </pre>
 *
 * <ul>
 *   <li>{@code timestamp} is unix seconds.</li>
 *   <li>{@code method} is upper-cased.</li>
 *   <li>{@code path} is exactly the request path (no base URL, no {@code /v1} prefix).</li>
 *   <li>{@code body} must be the exact serialized bytes sent as the request body; pass
 *       {@code null} when the request has no body, which hashes as the empty string.</li>
 * </ul>
 */
final class Signing {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DIGEST_ALGORITHM = "SHA-256";

    private Signing() {
    }

    static Map<String, String> signRequest(String secret, String method, String path, String body, long timestamp) {
        String bodyHash = sha256Hex(body == null ? "" : body);
        String message = timestamp + "." + method.toUpperCase() + "." + path + "." + bodyHash;
        String signature = hmacSha256Hex(secret, message);
        return Map.of(
                "X-SeeItLive-Signature", "sha256=" + signature,
                "X-SeeItLive-Timestamp", Long.toString(timestamp));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(DIGEST_ALGORITHM + " is not available", e);
        }
    }

    private static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HMAC_ALGORITHM + " is not available", e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid signing secret", e);
        }
    }
}
