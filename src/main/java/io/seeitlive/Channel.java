package io.seeitlive;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** How a session invite is delivered to the recipient. */
public enum Channel {
    SMS,
    EMAIL,
    LINK;

    @JsonValue
    public String toWireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
