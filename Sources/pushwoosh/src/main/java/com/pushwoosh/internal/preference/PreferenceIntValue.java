package com.pushwoosh.internal.preference;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

public class PreferenceIntValue implements PreferenceValue {
	@Nullable
	private final SharedPreferences preferences;
	private final String key;
	private int value;

	public PreferenceIntValue(@Nullable SharedPreferences preferences, String key, int defaultValue) {
		this.key = key;

		try {
			this.value = preferences == null ? defaultValue : preferences.getInt(key, defaultValue);
		} catch (Exception e) {
			PWLog.exception(e);
			this.value = defaultValue;
		}

		this.preferences = preferences;
	}

	public int get() {
		return value;
	}

	public void set(int value) {
		this.value = value;
		if (preferences == null) {
			PWLog.error("Incorrect state of the app preferences is null");
			return;
		}

		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(key, value);
		editor.apply();
	}
}
