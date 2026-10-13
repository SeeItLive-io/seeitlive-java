package io.seeitlive;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** The {@code oauth} resource: introspection of the bearer token currently in use. */
public final class OAuth {

    private final SeeItLiveHttpClient http;

    OAuth(SeeItLiveHttpClient http) {
        this.http = http;
    }

    /** {@code GET /oauth/verify} — maps the snake_case wire response to the idiomatic {@link TokenInfo}. */
    public TokenInfo verify() {
        RawTokenInfo r = http.get("/oauth/verify", RawTokenInfo.class);
        return new TokenInfo(r.valid(), r.clientId(), r.teamId(), r.scopes(), r.plan(), r.expiresAt());
    }

    private record RawTokenInfo(
            boolean valid,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("team_id") String teamId,
            List<String> scopes,
            Plan plan,
            @JsonProperty("expires_at") String expiresAt) {
    }
}
