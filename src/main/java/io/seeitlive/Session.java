package io.seeitlive;

import java.util.Map;

/** A created session, as returned by {@code POST /sessions}. */
public record Session(String id, String status, String inviteCode, String inviteUrl, String inviteShortUrl, String agentViewUrl, Map<String, String> metadata, String expiresAt, String protection, String protectionSecret, String protectionOtpauthUrl) {
}
