package com.pushwoosh.liveupdates.internal;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.liveupdates.LiveUpdateOperation;
import com.pushwoosh.liveupdates.LiveUpdateSegment;
import com.pushwoosh.liveupdates.LiveUpdateState;
import com.pushwoosh.notification.PushBundleDataProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a live-update push into a {@link LiveUpdateState}.
 * <p>
 * The whole payload arrives in one FCM data key, {@code pw_live}, whose value is a JSON object
 * serialized to a string. This parser reads that string into a {@link JSONObject} and pulls each
 * field with {@code org.json} {@code opt*} accessors: {@code op}/{@code id} are required;
 * {@code progress}/{@code when} arrive as numeric strings and are read only when present (absent →
 * {@code null}, never {@code 0}); bool fields read directly with their defaults; {@code segments}
 * is an array whose malformed elements are skipped individually. Title, subtitle, icon and actions
 * are NOT in {@code pw_live} — they come from the standard push bundle via
 * {@link PushBundleDataProvider}.
 * <p>
 * Parsing fails (returns {@code null}) when the bundle is {@code null}, {@code pw_live} is missing
 * or not valid JSON, or the required {@code op}/{@code id} are missing or invalid.
 */
public final class LiveUpdateStateParser {

    private static final String TAG = "LiveUpdateStateParser";

    private LiveUpdateStateParser() {}

    /** Cheap check used by the push handler to recognize a live-update push by its marker key. */
    public static boolean isLiveUpdatePush(@Nullable Bundle bundle) {
        return bundle != null && !TextUtils.isEmpty(bundle.getString(LiveUpdateBundleKeys.PW_LIVE));
    }

    /**
     * Parses the bundle's {@code pw_live} JSON object into a state.
     *
     * @param bundle the raw push payload
     * @return the parsed state, or {@code null} if {@code bundle} is {@code null}, {@code pw_live}
     *         is absent / malformed, or the required {@code op} / {@code id} are missing or invalid
     */
    @Nullable public static LiveUpdateState parse(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }

        String raw = bundle.getString(LiveUpdateBundleKeys.PW_LIVE);
        if (TextUtils.isEmpty(raw)) {
            return null;
        }

        JSONObject json;
        try {
            json = new JSONObject(raw);
        } catch (JSONException e) {
            PWLog.error(TAG, "pw_live is not valid JSON, ignoring: " + e.getMessage());
            return null;
        }

        LiveUpdateOperation op = LiveUpdateOperation.fromString(optStringOrNull(json, LiveUpdateBundleKeys.OP));
        String id = optStringOrNull(json, LiveUpdateBundleKeys.ID);

        if (op == null || TextUtils.isEmpty(id)) {
            PWLog.error(TAG, "missing or invalid op / id in pw_live");
            return null;
        }

        return new LiveUpdateState.Builder(id, op)
                .title(PushBundleDataProvider.getHeader(bundle))
                .subtitle(PushBundleDataProvider.getMessage(bundle))
                .iconUrl(PushBundleDataProvider.getLargeIcon(bundle))
                .progress(optInteger(json, LiveUpdateBundleKeys.PROGRESS))
                .progressIndeterminate(json.optBoolean(LiveUpdateBundleKeys.PROGRESS_INDETERMINATE, false))
                .showProgressBar(json.optBoolean(LiveUpdateBundleKeys.PROGRESS_BAR, true))
                .segments(parseSegments(json.optJSONArray(LiveUpdateBundleKeys.SEGMENTS)))
                .actions(new ArrayList<>(PushBundleDataProvider.getActions(bundle)))
                .extras(json.optJSONObject(LiveUpdateBundleKeys.EXTRAS))
                .when(optLongOrNull(json, LiveUpdateBundleKeys.WHEN))
                .chronometer(json.optBoolean(LiveUpdateBundleKeys.CHRONOMETER, false))
                .chronometerCountDown(json.optBoolean(LiveUpdateBundleKeys.CHRONOMETER_COUNT_DOWN, false))
                .showWhen(json.optBoolean(LiveUpdateBundleKeys.SHOW_WHEN, true))
                .build();
    }

    /**
     * Reads a string field, treating a literal JSON {@code null} the same as an absent key (both →
     * {@code null}). {@code optString(key, null)} alone is not enough: org.json coerces a JSON null
     * to the string {@code "null"}, which would slip past an {@code isEmpty} check downstream.
     */
    @Nullable private static String optStringOrNull(@NonNull JSONObject json, @NonNull String key) {
        return json.isNull(key) ? null : json.optString(key, null);
    }

    /**
     * Reads an int-valued field that arrives as a numeric string. Absent or literal {@code null} →
     * {@code null} (never 0); present → {@code optInt} coerces the numeric string. A present-but-
     * non-numeric value falls back to {@code 0} via {@code optInt} — acceptable, the backend never
     * emits such a value.
     */
    @Nullable private static Integer optInteger(@NonNull JSONObject json, @NonNull String key) {
        return json.has(key) && !json.isNull(key) ? json.optInt(key) : null;
    }

    /** Like {@link #optInteger}, for the {@code when} epoch-ms field. Absent / {@code null} → {@code null}. */
    @Nullable private static Long optLongOrNull(@NonNull JSONObject json, @NonNull String key) {
        return json.has(key) && !json.isNull(key) ? json.optLong(key) : null;
    }

    @NonNull private static List<LiveUpdateSegment> parseSegments(@Nullable JSONArray arr) {
        if (arr == null) {
            return new ArrayList<>();
        }
        List<LiveUpdateSegment> result = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj == null) {
                PWLog.warn(TAG, "skipping non-object segment at " + i);
                continue;
            }
            try {
                result.add(LiveUpdateSegment.fromJson(obj));
            } catch (Throwable t) {
                PWLog.warn(TAG, "skipping malformed segment at " + i + ": " + t.getMessage());
            }
        }
        return result;
    }
}
