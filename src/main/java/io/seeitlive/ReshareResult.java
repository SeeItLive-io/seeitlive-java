package io.seeitlive;

/**
 * The result of {@code Sessions.reshare(...)}, reporting how a revived
 * invitation was (or wasn't) delivered.
 *
 * <p>{@code inviteShortUrl} is null when the session has no short link — it
 * can be extended but not re-sent. {@code rotated} is true when a new
 * code/link was issued ({@code mode} of {@link ReshareMode#NEW}).
 */
public record ReshareResult(String expiresAt, String inviteShortUrl, String resend, boolean rotated) {
}
