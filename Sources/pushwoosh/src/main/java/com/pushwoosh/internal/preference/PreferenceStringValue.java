package com.pushwoosh.internal.preference;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

public class PreferenceStringValue implements PreferenceValue {
	@Nullable
	private final SharedPreferences preferences;
	private final String key;
	private String value;

	public PreferenceStringValue(@Nullable SharedPreferences preferences, String key, String defaultValue) {
		this.key = key;
		try {
			this.value = preferences == null ? defaultValue : preferences.getString(key, defaultValue);
		} catch (Exception e) {
			PWLog.exception(e);
			this.value = defaultValue;
		}
		this.preferences = preferences;
	}

	public String get() {
		return value;
	}

	public void set(String value) {
		this.value = value;
		if (preferences == null) {
			PWLog.error("Incorrect state of the app preferences is null");
			return;
		}
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(key, value);
		editor.apply();
	}
}
