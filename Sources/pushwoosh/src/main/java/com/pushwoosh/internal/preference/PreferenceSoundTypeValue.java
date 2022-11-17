package com.pushwoosh.internal.preference;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.SoundType;

public class PreferenceSoundTypeValue implements PreferenceValue {
	@Nullable
	private final SharedPreferences preferences;
	private final String key;
	private int value;

	public PreferenceSoundTypeValue(@Nullable SharedPreferences preferences, String key, SoundType defaultValue) {
		this.key = key;

		try {
			this.value = preferences == null ? defaultValue.getValue() : preferences.getInt(key, defaultValue.getValue());
		} catch (Exception e) {
			PWLog.exception(e);
			this.value = defaultValue.getValue();
		}

		this.preferences = preferences;
	}

	public SoundType get() {
		return SoundType.fromInt(value);
	}

	public void set(SoundType value) {
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
