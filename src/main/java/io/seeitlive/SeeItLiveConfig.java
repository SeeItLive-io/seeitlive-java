package io.seeitlive;

/**
 * Configuration for {@link SeeItLive}. Exactly one of {@link #apiKey()} / {@link #oauthToken()}
 * must be supplied.
 *
 * <p>{@link #signingSecret()} is optional; when set (a non-empty string), every outgoing
 * request is HMAC-signed via {@code X-SeeItLive-Signature} / {@code X-SeeItLive-Timestamp}
 * headers. Leaving it unset (or empty) is fully backwards-compatible — no signature headers
 * are sent.
 */
public record SeeItLiveConfig(String apiKey, String oauthToken, String baseUrl, Env env, String signingSecret) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String apiKey;
        private String oauthToken;
        private String baseUrl;
        private Env env;
        private String signingSecret;

        private Builder() {
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder oauthToken(String oauthToken) {
            this.oauthToken = oauthToken;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder env(Env env) {
            this.env = env;
            return this;
        }

        public Builder signingSecret(String signingSecret) {
            this.signingSecret = signingSecret;
            return this;
        }

        public SeeItLiveConfig build() {
            return new SeeItLiveConfig(apiKey, oauthToken, baseUrl, env, signingSecret);
        }
    }
}
