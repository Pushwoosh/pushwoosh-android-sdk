package com.pushwoosh.repository.config;

import android.content.SharedPreferences;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceIntValue;

public final class ConfigPrefs {
    private static final String PREFERENCE_KEY = "com.pushwoosh.config";
    private static final String PROPERTY_IS_LOGGER_ON = "pw_is_logger_on";

    private final PreferenceIntValue logger;

    public ConfigPrefs() {
        SharedPreferences preferences = AndroidPlatformModule.getPrefsProvider().providePrefs(PREFERENCE_KEY);
        logger = new PreferenceIntValue(preferences, PROPERTY_IS_LOGGER_ON, 0);
    }

    public PreferenceIntValue logger() {
        return logger;
    }
}
