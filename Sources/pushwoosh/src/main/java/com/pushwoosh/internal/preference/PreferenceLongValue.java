package com.pushwoosh.internal.preference;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;


public class PreferenceLongValue implements PreferenceValue {
	@Nullable
	private final SharedPreferences preferences;
	private final String key;
	private long value;

	public PreferenceLongValue(@Nullable SharedPreferences preferences, String key, long defaultValue) {
		this.key = key;
		this.preferences = preferences;

		try {
			this.value = preferences == null ? defaultValue : preferences.getLong(key, defaultValue);
		} catch (Exception e) {
			PWLog.exception(e);
			this.value = defaultValue;
		}
	}

	public long get() {
		return value;
	}

	public void set(long value) {
		this.value = value;
		if (preferences == null) {
			PWLog.error("Incorrect state of the app preferences is null");
			return;
		}
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(key, value);
		editor.apply();
	}
}
