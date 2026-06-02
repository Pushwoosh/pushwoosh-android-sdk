package com.pushwoosh.liveupdates;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A single phase in a Live Update progress bar.
 *
 * <p>{@link LiveUpdateState#getProgress()} is interpreted against the sum of segment
 * {@link #getLength() lengths}. With the default length of {@code 1} per segment this matches
 * {@code segments.size()}, so callers that do not set a length keep the original behaviour.
 */
public final class LiveUpdateSegment {
    private static final String TAG = "LiveUpdateSegment";

    private final int color;
    private final int length;

    /**
     * @param color  ARGB color of the segment
     * @param length relative weight of the segment; values below {@code 1} are clamped to {@code 1}
     *               with a warning
     */
    public LiveUpdateSegment(int color, int length) {
        this.color = color;
        if (length < 1) {
            PWLog.warn(TAG, "segment length must be >= 1, got " + length + ", falling back to 1");
            this.length = 1;
        } else {
            this.length = length;
        }
    }

    /** ARGB color of this segment. */
    public int getColor() {
        return color;
    }

    /** Relative weight of this segment within the progress bar; always {@code >= 1}. */
    public int getLength() {
        return length;
    }

    /**
     * Parses a segment from its JSON object representation.
     * <p>
     * Expects a required {@code "color"} string ({@code "#RRGGBB"} or {@code "#AARRGGBB"}) and an
     * optional {@code "length"} (defaults to {@code 1}).
     *
     * @param json the segment JSON object
     * @return the parsed segment
     * @throws JSONException if the required {@code "color"} field is missing (an unparseable color
     *                       value raises an unchecked {@link IllegalArgumentException})
     */
    @NonNull public static LiveUpdateSegment fromJson(@NonNull JSONObject json) throws JSONException {
        // "color" : "#RRGGBB" or "#AARRGGBB"; required.
        int color = android.graphics.Color.parseColor(json.getString("color"));
        // optInt accepts JSON numbers and numeric strings, falls back to 1 on missing key or
        // wrong type. The constructor enforces length >= 1 with a warn.
        int length = json.optInt("length", 1);
        return new LiveUpdateSegment(color, length);
    }
}
