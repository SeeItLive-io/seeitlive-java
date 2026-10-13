package io.seeitlive;

/** Thrown for HTTP 403 responses. */
public class PermissionException extends SeeItLiveException {

    public PermissionException(String message, String code) {
        super(403, message == null ? "Forbidden" : message, code);
    }
}
