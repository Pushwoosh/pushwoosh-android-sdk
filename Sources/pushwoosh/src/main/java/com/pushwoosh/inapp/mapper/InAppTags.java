package com.pushwoosh.inapp.mapper;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.NotificationPrefs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds the tag dictionary that {@link PlaceholderSubstitutor}'s tag passes consume. The source is the
 * local tag cache — the same source, with the same additions, that Rich Media HTML has used for years.
 *
 * <p>The push payload's {@code rm.tags} are deliberately NOT consulted: in-apps that arrive through
 * {@code postEvent} / {@code getInApps} carry no tags on the resource, so a payload-first rule would give
 * them defaults where HTML gives them values.
 *
 * <p>Five facts live here so that no caller has to know them: which pref holds the cache, which two
 * privacy flags gate the device tags, the exact device-tag key names, geo normalization, and the
 * value-to-String coercion.
 */
public final class InAppTags {
    private static final String TAG = "[InApp]InAppTags";

    private static final String KEY_OS_VERSION = "OS Version";
    private static final String KEY_DEVICE_MODEL = "Device Model";

    private InAppTags() {}

    @NonNull @WorkerThread
    public static Map<String, String> collect(@Nullable NotificationPrefs prefs) {
        if (prefs == null) {
            PWLog.warn(TAG, "notification preferences are not initialized, substituting placeholder defaults");
            return Collections.emptyMap();
        }

        try {
            Map<String, Object> raw = JsonUtils.jsonToMap(prefs.tags().get());
            // getTags never returns these two, so Rich Media has always added them locally.
            if (prefs.isCollectingDeviceOsVersionAllowed().get()) {
                raw.put(KEY_OS_VERSION, Build.VERSION.RELEASE);
            }
            if (prefs.isCollectingDeviceModelAllowed().get()) {
                raw.put(KEY_DEVICE_MODEL, DeviceUtils.getDeviceName());
            }
            InAppTagFormatModifier.convertGeoTags(raw);
            return toStringValues(raw);
        } catch (Throwable t) {
            // Throwable, not Exception: tags must never cost a show. Worst case is placeholder defaults.
            PWLog.warn(TAG, "failed to collect tags, substituting placeholder defaults", t);
            return Collections.emptyMap();
        }
    }

    // Same rule as ResourceParseUtils.convertTags (toString(), drop nulls), duplicated on purpose: that
    // class is package-private in inapp.network.model, and publishing it to reuse eight lines would cost
    // more than the copy. Change both or neither.
    @NonNull private static Map<String, String> toStringValues(@NonNull Map<String, Object> raw) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return result;
    }
}
