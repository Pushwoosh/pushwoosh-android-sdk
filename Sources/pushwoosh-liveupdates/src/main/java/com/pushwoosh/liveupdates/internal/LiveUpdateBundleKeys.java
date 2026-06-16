package com.pushwoosh.liveupdates.internal;

/**
 * Keys for the live-update fields carried flat in a push {@link android.os.Bundle}.
 * <p>
 * On Android these values arrive in the bundle root (sent via {@code android_root_params}), each as
 * a string; {@link LiveUpdateStateParser} reads them by these keys and coerces them into a
 * {@link com.pushwoosh.liveupdates.LiveUpdateState}.
 */
public final class LiveUpdateBundleKeys {
    public static final String PW_LIVE_OP = "pw_live_op";
    public static final String PW_LIVE_ID = "pw_live_id";
    public static final String PROGRESS = "pw_live_progress";
    public static final String PROGRESS_INDETERMINATE = "pw_live_progress_indeterminate";
    public static final String PROGRESS_BAR = "pw_live_progress_bar";
    public static final String SEGMENTS = "pw_live_segments";
    public static final String EXTRAS = "pw_live_extras";
    public static final String WHEN = "pw_live_when";
    public static final String CHRONOMETER = "pw_live_chronometer";
    public static final String CHRONOMETER_COUNT_DOWN = "pw_live_chronometer_count_down";
    public static final String SHOW_WHEN = "pw_live_show_when";

    private LiveUpdateBundleKeys() {}
}
