package io.seeitlive;

/**
 * One entry of {@link Snapshots#list(String)}. {@code url} is the API path the
 * bytes are served from. Width/height are boxed: the API reports null for a
 * snapshot captured without dimensions.
 */
public record SnapshotSummary(String id, Integer width, Integer height, String capturedAt, String url) {
}
