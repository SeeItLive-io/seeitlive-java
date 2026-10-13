package io.seeitlive;

import java.util.List;

/** The idiomatic (camelCase) result of {@link SeeItLive#exchangeCode(ExchangeCodeParams)}. */
public record TokenResponse(String accessToken, String tokenType, String clientId, String teamId, List<String> scopes, Plan plan) {
}
