package io.seeitlive;

/**
 * Parameters for the standalone {@link SeeItLive#exchangeCode(ExchangeCodeParams)} call
 * (confidential-client authorization-code exchange; no bearer token involved).
 */
public record ExchangeCodeParams(String code, String clientId, String clientSecret, String redirectUri, String codeVerifier, String baseUrl) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String code;
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String codeVerifier;
        private String baseUrl;

        private Builder() {
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public Builder codeVerifier(String codeVerifier) {
            this.codeVerifier = codeVerifier;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public ExchangeCodeParams build() {
            return new ExchangeCodeParams(code, clientId, clientSecret, redirectUri, codeVerifier, baseUrl);
        }
    }
}
