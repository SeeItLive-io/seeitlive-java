package io.seeitlive;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Known-answer vector tests for {@link Signing#signRequest}, mirroring
 * {@code packages/client-node/src/signing.test.ts} against the frozen Node reference —
 * these vectors MUST match byte-for-byte across every SDK.
 */
class SigningTest {

    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void matchesKnownAnswerVectorForPostWithBody() {
        Map<String, String> headers = Signing.signRequest(SECRET, "POST", "/sessions", "{\"channel\":\"demo\"}", 1800000000L);

        assertEquals("sha256=a59401f3e80ae2e50f4b2517c8e4708de856bb93eadb1f4cfc59100db2b6e7f7",
                headers.get("X-SeeItLive-Signature"));
        assertEquals("1800000000", headers.get("X-SeeItLive-Timestamp"));
    }

    @Test
    void matchesKnownAnswerVectorForGetWithNoBody() {
        Map<String, String> headers = Signing.signRequest(SECRET, "GET", "/sessions/x/status", null, 1800000000L);

        assertEquals("sha256=3499dfb7f9581f996d537dfb108f775cb329f1f3f490f3bcb791820b4d81d5ea",
                headers.get("X-SeeItLive-Signature"));
        assertEquals("1800000000", headers.get("X-SeeItLive-Timestamp"));
    }

    @Test
    void upperCasesTheMethodSoLowerCaseInputYieldsTheSameSignature() {
        Map<String, String> upper = Signing.signRequest(SECRET, "POST", "/sessions", "{\"channel\":\"demo\"}", 1800000000L);
        Map<String, String> lower = Signing.signRequest(SECRET, "post", "/sessions", "{\"channel\":\"demo\"}", 1800000000L);

        assertEquals(upper, lower);
    }
}
