package io.seeitlive;

/** Thrown for HTTP 404 responses. */
public class NotFoundException extends SeeItLiveException {

    public NotFoundException(String message, String code) {
        super(404, message == null ? "Not found" : message, code);
    }
}
