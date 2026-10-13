package io.seeitlive;

import java.util.Map;

/**
 * Request body for {@code sessions().create(...)}. Only {@link #channel()} is required;
 * {@code null} optional fields are omitted from the JSON body entirely (never sent as
 * {@code null}), and there is deliberately no team id — the team is derived from
 * the bearer token.
 */
public record CreateSessionParams(Channel channel, String recipientPhone, String recipientEmail, String note, Integer ttlMinutes, Boolean smsConsent, Map<String, String> metadata, Boolean shortLink, Protection protection, String protectionSecret) {

    public static Builder builder(Channel channel) {
        return new Builder(channel);
    }

    public static final class Builder {
        private final Channel channel;
        private String recipientPhone;
        private String recipientEmail;
        private String note;
        private Integer ttlMinutes;
        private Boolean smsConsent;
        private Map<String, String> metadata;
        private Boolean shortLink;
        private Protection protection;
        private String protectionSecret;

        private Builder(Channel channel) {
            this.channel = channel;
        }

        public Builder recipientPhone(String recipientPhone) {
            this.recipientPhone = recipientPhone;
            return this;
        }

        public Builder recipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
            return this;
        }

        public Builder note(String note) {
            this.note = note;
            return this;
        }

        public Builder ttlMinutes(Integer ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
            return this;
        }

        /**
         * Attests the recipient agreed to receive a text. Required to be
         * {@code true} for the {@code SMS} channel (carrier fees may apply).
         */
        public Builder smsConsent(Boolean smsConsent) {
            this.smsConsent = smsConsent;
            return this;
        }

        /**
         * Optional flat map of string keys to string values attached to the
         * session. Omitted from the JSON body entirely when {@code null}.
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Requests that the API also mint a {@code sitl.cc/s/<code>} short
         * link for the session invite.
         */
        public Builder shortLink(Boolean shortLink) {
            this.shortLink = shortLink;
            return this;
        }

        /**
         * Requires the guest to answer a challenge before sharing camera:
         * {@code PASSWORD} (a static secret) or {@code TOTP} (a rotating
         * 6-digit code the agent reads aloud).
         */
        public Builder protection(Protection protection) {
            this.protection = protection;
            return this;
        }

        /**
         * The password or base32 TOTP seed. Leave {@code null} and the API
         * generates one, returned once on the {@link Session}.
         */
        public Builder protectionSecret(String protectionSecret) {
            this.protectionSecret = protectionSecret;
            return this;
        }

        public CreateSessionParams build() {
            return new CreateSessionParams(channel, recipientPhone, recipientEmail, note, ttlMinutes, smsConsent, metadata, shortLink, protection, protectionSecret);
        }
    }
}
