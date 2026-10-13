package io.seeitlive;

/** The plan associated with an API key or OAuth token's team. Limits are {@code null} when unlimited. */
public record Plan(String planKey, String planName, Integer sessionLimit, Integer seatLimit, Integer concurrentLimit) {
}
