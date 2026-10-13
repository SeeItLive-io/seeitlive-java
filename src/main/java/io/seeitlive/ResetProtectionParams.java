package io.seeitlive;

/**
 * Clears a guest lockout. Rotation is opt-in: the usual cause of a lockout is
 * the customer mistyping, and a new secret would just make the agent read it
 * out again for no security gain.
 *
 * <p>Leave {@code rotate} null and the key is omitted from the body entirely
 * (the mapper is configured NON_NULL), keeping the existing secret.
 */
public record ResetProtectionParams(Boolean rotate) {

    /** Convenience for the common case: clear the lockout, keep the secret. */
    public ResetProtectionParams() {
        this(null);
    }
}
