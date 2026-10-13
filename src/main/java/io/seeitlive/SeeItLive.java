package io.seeitlive;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Entry point for the SeeItLive Java SDK.
 *
 * <pre>{@code
 * SeeItLive client = new SeeItLive(SeeItLiveConfig.builder().apiKey("sil_live_...").build());
 * Session session = client.sessions().create(CreateSessionParams.builder(Channel.LINK).build());
 * SessionStatus status = client.sessions().status(session.id());
 * }</pre>
 *
 * Authenticate with exactly one of an API key or an OAuth access token (both are
 * {@code sil_<env>_...} bearer tokens); the environment is inferred from the
 * {@code sil_test_} prefix unless {@link SeeItLiveConfig#env()} overrides it.
 */
public final class SeeItLive {

    private static final String DEFAULT_BASE_URL = "https://api.seeitlive.io/v1";

    private final Env env;
    private final Sessions sessions;
    private final Snapshots snapshots;
    private final OAuth oauth;

    public SeeItLive(SeeItLiveConfig config) {
        boolean hasApiKey = config.apiKey() != null && !config.apiKey().isEmpty();
        boolean hasOauthToken = config.oauthToken() != null && !config.oauthToken().isEmpty();
        if (!hasApiKey && !hasOauthToken) {
            throw new IllegalArgumentException("Either apiKey or oauthToken must be provided");
        }
        if (hasApiKey && hasOauthToken) {
            throw new IllegalArgumentException("Provide only one of apiKey or oauthToken");
        }
        String token = hasApiKey ? config.apiKey() : config.oauthToken();

        this.env = config.env() != null ? config.env() : (token.startsWith("sil_test_") ? Env.TEST : Env.LIVE);
        String baseUrl = (config.baseUrl() != null ? config.baseUrl() : DEFAULT_BASE_URL).replaceAll("/+$", "");
        SeeItLiveHttpClient http = new SeeItLiveHttpClient(baseUrl, "Bearer " + token, config.signingSecret());
        this.sessions = new Sessions(http);
        this.snapshots = new Snapshots(http);
        this.oauth = new OAuth(http);
    }

    /** The environment this client targets ({@link Env#LIVE} or {@link Env#TEST}). */
    public Env getEnv() {
        return env;
    }

    /** The {@code sessions} resource: create + status. */
    public Sessions sessions() {
        return sessions;
    }

    /** The {@code snapshots} resource: list a session's snapshots, fetch one. */
    public Snapshots snapshots() {
        return snapshots;
    }

    /** The {@code oauth} resource: verify the current bearer token. */
    public OAuth oauth() {
        return oauth;
    }

    /**
     * Standalone confidential-client authorization-code exchange, {@code POST /oauth/token}.
     * Sent with NO bearer/Authorization header — the client is authenticated via
     * {@code client_id}/{@code client_secret} in the request body.
     */
    public static TokenResponse exchangeCode(ExchangeCodeParams params) {
        String baseUrl = (params.baseUrl() != null ? params.baseUrl() : DEFAULT_BASE_URL).replaceAll("/+$", "");
        SeeItLiveHttpClient http = new SeeItLiveHttpClient(baseUrl, null);
        String codeVerifier = params.codeVerifier();
        if (codeVerifier != null && codeVerifier.isEmpty()) {
            codeVerifier = null;
        }
        TokenExchangeRequest body = new TokenExchangeRequest(
                "authorization_code", params.code(), params.clientId(), params.clientSecret(),
                params.redirectUri(), codeVerifier);
        RawTokenResponse r = http.post("/oauth/token", body, RawTokenResponse.class);
        return new TokenResponse(r.accessToken(), r.tokenType(), r.clientId(), r.teamId(), r.scopes(), r.plan());
    }

    private record TokenExchangeRequest(
            @JsonProperty("grant_type") String grantType,
            String code,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            @JsonProperty("redirect_uri") String redirectUri,
            @JsonProperty("code_verifier") String codeVerifier) {
    }

    private record RawTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("team_id") String teamId,
            List<String> scopes,
            Plan plan) {
    }
}
