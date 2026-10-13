package io.seeitlive;

/** Thrown for HTTP 400 responses. {@link #getField()} names the offending field, when the API supplies one. */
public class ValidationException extends SeeItLiveException {

    private final String field;

    public ValidationException(String message, String field, String code) {
        super(400, message, code);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
