package io.seeitlive;

/**
 * The agent's view of a session's guest challenge, as returned by
 * {@code GET /sessions/:id/protection} and {@code PUT /sessions/:id/protection}.
 *
 * <p>For {@code totp}, {@code code} is the CURRENT code, never the seed — the
 * seed leaves the server only once, in the create response.
 */
public record ProtectionInfo(
        String type,
        String password,
        String code,
        Integer secondsRemaining,
        boolean locked,
        int failedAttempts) {
}
