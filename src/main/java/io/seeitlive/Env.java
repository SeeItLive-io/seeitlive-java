package io.seeitlive;

import java.util.Locale;

/** The API environment a client targets, inferred from the {@code sil_test_}/{@code sil_live_} token prefix. */
public enum Env {
    LIVE,
    TEST;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
