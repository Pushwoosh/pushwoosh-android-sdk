package com.pushwoosh.internal.preference;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.VibrateType;

public class PreferenceVibrateTypeValue implements PreferenceValue {
	@Nullable
	private final SharedPreferences preferences;
	private final String key;
	private int value;

	public PreferenceVibrateTypeValue(@Nullable SharedPreferences preferences, String key, VibrateType defaultValue) {
		this.key = key;

		try {
			this.value = preferences == null ? defaultValue.getValue() : preferences.getInt(key, defaultValue.getValue());
		} catch (Exception e) {
			PWLog.exception(e);
			this.value = defaultValue.getValue();
		}

		this.preferences = preferences;
	}

	public VibrateType get() {
		return VibrateType.fromInt(value);
	}

	public void set(VibrateType value) {
		this.value = value.getValue();
		if (preferences == null) {
			PWLog.error("Incorrect state of the app preferences is null");
			return;
		}

		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(key, value.getValue());
		editor.apply();
	}
}
