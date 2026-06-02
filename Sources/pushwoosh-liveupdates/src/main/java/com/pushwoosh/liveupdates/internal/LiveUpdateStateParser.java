package com.pushwoosh.liveupdates.internal;

import android.os.Bundle;
import android.text.TextUtils;

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
import java.util.Locale;

/**
 * Tolerant parser that turns a raw push {@link Bundle} into a {@link LiveUpdateState}.
 * <p>
 * Live-update payload values arrive flattened into the bundle as strings (they travel in
 * {@code android_root_params}), so every field is read by key and coerced leniently: numbers and
 * booleans accept multiple textual forms, malformed segments are skipped individually, and absent
 * optional fields fall back to defaults. Parsing fails (returns {@code null}) only when a required
 * field is missing or invalid — the operation ({@code pw_live_op}) or the id ({@code pw_live_id}).
 */
public final class LiveUpdateStateParser {

    private static final String TAG = "LiveUpdateStateParser";

    private LiveUpdateStateParser() {}

    /** Cheap check used by the push handler to recognize a live-update push by its marker key. */
    public static boolean isLiveUpdatePush(@Nullable Bundle bundle) {
        return bundle != null && !TextUtils.isEmpty(bundle.getString(LiveUpdateBundleKeys.PW_LIVE_OP));
    }

    /**
     * Parses the bundle into a state, coercing each field tolerantly.
     *
     * @param bundle the raw push payload
     * @return the parsed state, or {@code null} if {@code bundle} is {@code null} or the required
     *         {@code pw_live_op} / {@code pw_live_id} are missing or invalid
     */
    @Nullable public static LiveUpdateState parse(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }

        LiveUpdateOperation op = LiveUpdateOperation.fromString(bundle.getString(LiveUpdateBundleKeys.PW_LIVE_OP));
        String id = bundle.getString(LiveUpdateBundleKeys.PW_LIVE_ID);

        if (op == null || TextUtils.isEmpty(id)) {
            PWLog.error(TAG, "missing or invalid pw_live_op / pw_live_id");
            return null;
        }

        return new LiveUpdateState.Builder(id, op)
                .title(PushBundleDataProvider.getHeader(bundle))
                .subtitle(PushBundleDataProvider.getMessage(bundle))
                .iconUrl(PushBundleDataProvider.getLargeIcon(bundle))
                .progress(parseInt(bundle.getString(LiveUpdateBundleKeys.PROGRESS)))
                .progressIndeterminate(parseBool(bundle.getString(LiveUpdateBundleKeys.PROGRESS_INDETERMINATE), false))
                .segments(parseSegments(bundle.getString(LiveUpdateBundleKeys.SEGMENTS)))
                .actions(new ArrayList<>(PushBundleDataProvider.getActions(bundle)))
                .extras(parseObject(bundle.getString(LiveUpdateBundleKeys.EXTRAS)))
                .when(parseLong(bundle.getString(LiveUpdateBundleKeys.WHEN)))
                .chronometer(parseBool(bundle.getString(LiveUpdateBundleKeys.CHRONOMETER), false))
                .chronometerCountDown(parseBool(bundle.getString(LiveUpdateBundleKeys.CHRONOMETER_COUNT_DOWN), false))
                .showWhen(parseBool(bundle.getString(LiveUpdateBundleKeys.SHOW_WHEN), true))
                .build();
    }

    @Nullable private static Integer parseInt(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable private static Long parseLong(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Accepts {@code true/1/yes} and {@code false/0/no} (case-insensitive); else {@code defaultValue}. */
    private static boolean parseBool(@Nullable String raw, boolean defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "true":
            case "1":
            case "yes":
                return true;
            case "false":
            case "0":
            case "no":
                return false;
            default:
                return defaultValue;
        }
    }

    private static List<LiveUpdateSegment> parseSegments(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return new ArrayList<>();
        }
        try {
            JSONArray arr = new JSONArray(raw);
            List<LiveUpdateSegment> result = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                try {
                    result.add(LiveUpdateSegment.fromJson(arr.getJSONObject(i)));
                } catch (Throwable t) {
                    PWLog.warn(TAG, "skipping malformed segment at " + i + ": " + t.getMessage());
                }
            }
            return result;
        } catch (JSONException e) {
            PWLog.warn(TAG, "segments not a valid JSON array, ignoring");
            return new ArrayList<>();
        }
    }

    @Nullable private static JSONObject parseObject(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return null;
        }
    }
}
