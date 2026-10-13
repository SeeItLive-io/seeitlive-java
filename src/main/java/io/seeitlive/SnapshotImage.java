package io.seeitlive;

/**
 * One snapshot fetched by {@link Snapshots#get(String)}. {@code data} is the
 * base64-encoded PNG — decode it with {@code java.util.Base64} to get bytes.
 */
public record SnapshotImage(String id, String contentType, Integer width, Integer height, String capturedAt, String data) {
}
