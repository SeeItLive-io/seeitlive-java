package io.seeitlive;

/**
 * Changes a session's guest challenge after creation.
 *
 * <p>Leave {@code protectionSecret} null and the API generates one — the mapper
 * is configured NON_NULL, so the key is omitted from the body rather than sent
 * as an explicit null, and absence is exactly what triggers generation.
 * {@code protection} of {@code "none"} clears the challenge and any lockout.
 */
public record SetProtectionParams(String protection, String protectionSecret) {

    /** Convenience for the common case: let the API generate the secret. */
    public SetProtectionParams(String protection) {
        this(protection, null);
    }
}
