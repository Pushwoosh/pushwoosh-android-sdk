package com.pushwoosh.baidu.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;

public class BaiduPrefs {
    private static final String PREFERENCE_KEY = "com.pushwoosh.baidu";
    private static final String PROPERTY_FIRST_START = "pw_first_start";

    private final PreferenceBooleanValue firstStart;

    public BaiduPrefs(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        firstStart = new PreferenceBooleanValue(preferences, PROPERTY_FIRST_START, true);
    }

    public PreferenceBooleanValue isFirstStart() {
        return firstStart;
    }
}
