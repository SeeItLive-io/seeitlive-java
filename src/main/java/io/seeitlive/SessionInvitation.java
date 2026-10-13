package io.seeitlive;

/**
 * A session's invitation as the agent side sees it — never the token hash and
 * never the guest secret. Nested in {@link SessionDetail}.
 *
 * <p>{@code channel} is {@code "sms"}, {@code "email"} or {@code "link"}: a
 * plain String rather than {@link Channel} so a channel added server-side later
 * cannot fail deserialization of the whole session.
 */
public record SessionInvitation(String code, String channel, String recipient, String expiresAt, String openedAt) {
}
