package io.seeitlive;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** How {@link Sessions#reshare(String, ReshareParams)} revives (or replaces) an expired invitation. */
public enum ReshareMode {
    REVIVE,
    NEW;

    @JsonValue
    public String toWireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
