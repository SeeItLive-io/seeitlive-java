package io.seeitlive;

import java.util.Arrays;
import java.util.List;

/**
 * The {@code snapshots} resource: list a session's captured frames and fetch
 * one. Both calls require the team's plan to include snapshot history.
 */
public final class Snapshots {

    private final SeeItLiveHttpClient http;

    Snapshots(SeeItLiveHttpClient http) {
        this.http = http;
    }

    /** {@code GET /sessions/:id/snapshots} — a session's snapshots, newest first. */
    public List<SnapshotSummary> list(String sessionId) {
        SnapshotSummary[] items = http.get(
                "/sessions/" + SeeItLiveHttpClient.encodePathSegment(sessionId) + "/snapshots",
                SnapshotSummary[].class);
        return items == null ? List.of() : Arrays.asList(items);
    }

    /**
     * {@code GET /snapshots/:id?format=base64} — the endpoint serves raw PNG
     * bytes by default; the SDK asks for base64 JSON so the response stays JSON,
     * and signs the query along with the path, which is what the server verifies.
     */
    public SnapshotImage get(String id) {
        return http.get(
                "/snapshots/" + SeeItLiveHttpClient.encodePathSegment(id) + "?format=base64",
                SnapshotImage.class);
    }
}
