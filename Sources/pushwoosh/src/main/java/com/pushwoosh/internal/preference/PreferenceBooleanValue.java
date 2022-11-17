package com.pushwoosh.internal.preference;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

public class PreferenceBooleanValue implements PreferenceValue {
	@Nullable
	private final SharedPreferences preferences;
	private final String key;
	private boolean value;

	public PreferenceBooleanValue(@Nullable SharedPreferences preferences, String key, boolean defaultValue) {
		this.key = key;
		try {
			this.value = preferences == null ? defaultValue : preferences.getBoolean(key, defaultValue);
		} catch (Exception e) {
			PWLog.exception(e);
			this.value = defaultValue;
		}
		this.preferences = preferences;
	}

	public boolean get() {
		return value;
	}

	public void set(boolean value) {
		this.value = value;
		if (preferences == null) {
			PWLog.error("Incorrect state of the app preferences is null");
			return;
		}
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(key, value);
		editor.apply();
	}
}
