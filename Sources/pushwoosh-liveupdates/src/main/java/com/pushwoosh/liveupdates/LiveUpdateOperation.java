package com.pushwoosh.liveupdates;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * The lifecycle operation carried by a Live Update push, available via
 * {@link LiveUpdateState#getOperation()}.
 * <p>
 * The Pushwoosh backend drives the full lifecycle of a Live Update through these three
 * operations; the SDK reacts to each one when rendering:
 * <ul>
 *   <li>{@link #START} — first push for an {@code activityId}; posts the ongoing notification</li>
 *   <li>{@link #UPDATE} — a later push for the same {@code activityId}; refreshes it in place</li>
 *   <li>{@link #END} — terminal push; dismisses the notification</li>
 * </ul>
 * A {@link LiveUpdateProgressStyleProvider} is consulted on {@code START} and {@code UPDATE} only;
 * {@code END} dismisses without building a style.
 */
public enum LiveUpdateOperation {
    /** First push for an {@code activityId}; the live update is posted. */
    START,
    /** Subsequent push for an existing {@code activityId}; the live update is refreshed in place. */
    UPDATE,
    /** Terminal push; the live update is dismissed. */
    END;

    /**
     * Parses the raw {@code pw_live_op} payload value into an operation, case-insensitively.
     *
     * @param raw the raw operation string from the push payload (e.g. {@code "start"})
     * @return the matching operation, or {@code null} if {@code raw} is {@code null} or unrecognized
     */
    @Nullable public static LiveUpdateOperation fromString(@Nullable String raw) {
        if (raw == null) return null;
        switch (raw.toLowerCase(Locale.ROOT)) {
            case "start":
                return START;
            case "update":
                return UPDATE;
            case "end":
                return END;
            default:
                return null;
        }
    }
}
