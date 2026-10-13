package io.seeitlive;

/**
 * Options for reviving (or replacing) an expired invitation.
 *
 * <p>Every field is optional — the mapper is configured NON_NULL, so a
 * {@code null} field is omitted from the body rather than sent as an
 * explicit {@code null}, and absence is what tells the API to reuse what
 * the invitation already recorded. {@code smsConsent} is required to be
 * {@code true} when {@code channel} is {@link Channel#SMS}.
 */
public record ReshareParams(ReshareMode mode, Channel channel, String recipient, Boolean smsConsent) {

    /** Convenience for the common case: no changes, just revive in place. */
    public ReshareParams() {
        this(null, null, null, null);
    }
}
