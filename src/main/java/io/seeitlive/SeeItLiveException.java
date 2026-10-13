package io.seeitlive;

/** Base exception for all SeeItLive API errors. Carries the HTTP status code and an optional machine-readable code. */
public class SeeItLiveException extends RuntimeException {

    private final int statusCode;
    private final String code;

    public SeeItLiveException(int statusCode, String message, String code) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }
}
