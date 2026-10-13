package io.seeitlive;

/** A session's current status, as returned by {@code GET /sessions/:id/status}. */
public record SessionStatus(String id, String status, String startedAt, String endedAt, int producerCount, boolean hasVideo) {
}
