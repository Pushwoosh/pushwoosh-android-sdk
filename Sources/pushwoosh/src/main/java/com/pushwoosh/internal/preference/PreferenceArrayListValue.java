package com.pushwoosh.internal.preference;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.ObjectSerializer;
import com.pushwoosh.internal.utils.PWLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class PreferenceArrayListValue<T extends Serializable> implements PreferenceValue {
	@Nullable
	private final SharedPreferences preferences;
	private final String key;
	private ArrayList<T> value;
	private final int capacity;

	@Deprecated
	public PreferenceArrayListValue(@Nullable SharedPreferences preferences, String key, int capacity) {
		this(preferences, key, capacity, null);
	}

	public PreferenceArrayListValue(@Nullable SharedPreferences preferences, String key, int capacity, Class<T> clazz) {
		this.key = key;
		this.capacity = capacity;
		try {
			String rawValue = preferences == null ? null : preferences.getString(key, null);
            if (rawValue == null) {
                this.value = new ArrayList<>();
            } else if (clazz == null) {
                this.value = ObjectSerializer.deserialize(rawValue, ArrayList.class);
            } else {
                this.value = ObjectSerializer.deserialize(rawValue, ArrayList.class, clazz);
            }
		} catch (Exception e) {
			PWLog.exception(e);
			this.value = new ArrayList<>();
		}
		this.preferences = preferences;
	}

	public ArrayList<T> get() {
		return new ArrayList<>(value);
	}

	public void add(T value) {
		try {
			this.value.add(value);
			if (this.value.size() > capacity) {
				this.value.remove(0);
			}
			save();
		} catch (Exception e) {
			PWLog.exception(e);
		}
	}

	public void replaceAll(Collection<T> values) {
		try {
			this.value.clear();
			this.value.addAll(values);

			save();
		} catch (Exception e) {
			PWLog.exception(e);
		}
	}

	private void save() throws java.io.IOException {
		if (preferences == null) {
			PWLog.error("Incorrect state of the app preferences is null");
			return;
		}

		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(key, ObjectSerializer.serialize(this.value));
		editor.apply();
	}

	public void clear() {
		try {
			this.value.clear();
			save();
		} catch (Exception e) {
			PWLog.exception(e);
		}
	}

	public void remove(T object) {
		try {
			this.value.remove(object);
			save();
		} catch (Exception e) {
			PWLog.exception(e);
		}
	}
}
