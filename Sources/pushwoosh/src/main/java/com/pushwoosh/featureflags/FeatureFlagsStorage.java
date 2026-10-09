package com.pushwoosh.featureflags;

import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.preference.PreferenceLongValue;
import com.pushwoosh.internal.preference.PreferenceStringValue;

final class FeatureFlagsStorage {
    static final String PREFS_NAME = "pw_feature_flags";

    static final class Snapshot {
        @NonNull final String flagsJson;

        @NonNull final String etag;

        final long fetchedAt;

        @NonNull final String userId;

        Snapshot(@NonNull String flagsJson, @NonNull String etag, long fetchedAt, @NonNull String userId) {
            this.flagsJson = flagsJson;
            this.etag = etag;
            this.fetchedAt = fetchedAt;
            this.userId = userId;
        }
    }

    private final PreferenceStringValue flags;
    private final PreferenceStringValue etag;
    private final PreferenceLongValue fetchedAt;
    private final PreferenceStringValue userId;
    private final PreferenceLongValue lastRequestAt;

    FeatureFlagsStorage(@Nullable SharedPreferences prefs) {
        flags = new PreferenceStringValue(prefs, "flags", "");
        etag = new PreferenceStringValue(prefs, "etag", "");
        fetchedAt = new PreferenceLongValue(prefs, "fetched_at", 0L);
        userId = new PreferenceStringValue(prefs, "user_id", "");
        lastRequestAt = new PreferenceLongValue(prefs, "last_request_at", 0L);
    }

    @Nullable Snapshot load() {
        String storedFlags = flags.get();
        String storedUser = userId.get();
        if (TextUtils.isEmpty(storedFlags) || TextUtils.isEmpty(storedUser)) {
            return null;
        }
        String storedEtag = etag.get();
        return new Snapshot(storedFlags, storedEtag != null ? storedEtag : "", fetchedAt.get(), storedUser);
    }

    void save(@NonNull String flagsJson, @NonNull String etagValue, long fetchedAtValue, @NonNull String userIdValue) {
        flags.set(flagsJson);
        etag.set(etagValue);
        fetchedAt.set(fetchedAtValue);
        userId.set(userIdValue);
    }

    void saveEtag(@NonNull String value) {
        etag.set(value);
    }

    void saveFetchedAt(long value) {
        fetchedAt.set(value);
    }

    long lastRequestAt() {
        return lastRequestAt.get();
    }

    void saveLastRequestAt(long value) {
        lastRequestAt.set(value);
    }

    void clear() {
        flags.set("");
        etag.set("");
        fetchedAt.set(0L);
        userId.set("");
    }
}
