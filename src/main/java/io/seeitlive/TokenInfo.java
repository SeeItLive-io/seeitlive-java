package io.seeitlive;

import java.util.List;

/** The idiomatic (camelCase) result of {@code oauth().verify()}, mapped from the snake_case wire response. */
public record TokenInfo(boolean valid, String clientId, String teamId, List<String> scopes, Plan plan, String expiresAt) {
}
