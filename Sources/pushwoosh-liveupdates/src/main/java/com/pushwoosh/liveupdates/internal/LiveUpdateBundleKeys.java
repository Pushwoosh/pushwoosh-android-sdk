package com.pushwoosh.liveupdates.internal;

/**
 * Keys for a live-update push.
 * <p>
 * The whole live-update payload arrives in a single FCM data key, {@link #PW_LIVE}, whose value is
 * a JSON object serialized to a string (modeled on iOS Live Activity). {@link LiveUpdateStateParser}
 * reads that string, parses it into a {@link org.json.JSONObject}, and pulls each field by the
 * field-name constants below into a {@link com.pushwoosh.liveupdates.LiveUpdateState}.
 * <p>
 * Title, subtitle and icon are NOT part of {@code pw_live}; they come from the standard push payload
 * ({@code header} / {@code title} / {@code ci}) via {@code PushBundleDataProvider}.
 */
public final class LiveUpdateBundleKeys {
    /** The single FCM data bundle key carrying the live-update JSON object as a string. */
    public static final String PW_LIVE = "pw_live";

    // Field names inside the pw_live JSON object.
    public static final String OP = "op";
    public static final String ID = "id";
    public static final String PROGRESS = "progress";
    public static final String PROGRESS_INDETERMINATE = "progress_indeterminate";
    public static final String PROGRESS_BAR = "progress_bar";
    public static final String SEGMENTS = "segments";
    public static final String EXTRAS = "extras";
    public static final String WHEN = "when";
    public static final String CHRONOMETER = "chronometer";
    public static final String CHRONOMETER_COUNT_DOWN = "chronometer_count_down";
    public static final String SHOW_WHEN = "show_when";

    private LiveUpdateBundleKeys() {}
}
