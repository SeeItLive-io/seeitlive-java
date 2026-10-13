package io.seeitlive;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Optional challenge a guest must answer before sharing their camera. */
public enum Protection {
    PASSWORD,
    TOTP;

    @JsonValue
    public String toWireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
