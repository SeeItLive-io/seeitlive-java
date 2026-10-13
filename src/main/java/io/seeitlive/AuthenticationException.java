package io.seeitlive;

/** Thrown for HTTP 401 responses. */
public class AuthenticationException extends SeeItLiveException {

    public AuthenticationException(String message, String code) {
        super(401, message == null ? "Authentication failed" : message, code);
    }
}
