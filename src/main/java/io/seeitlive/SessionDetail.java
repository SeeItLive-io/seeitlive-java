package io.seeitlive;

import java.util.Map;

/**
 * A stored session read back by {@code GET /sessions/:id}, unlike
 * {@link Session} which is the one-time create response. {@code invitation} is
 * null for a session whose invitation is gone.
 */
public record SessionDetail(
        String id,
        String teamId,
        String env,
        String status,
        String createdByClientId,
        String createdByApiKeyId,
        String note,
        Map<String, String> metadata,
        String startedAt,
        String endedAt,
        String createdAt,
        SessionInvitation invitation) {
}
