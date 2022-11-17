/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.event;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

/**
 * Wrapped {@link OnSharedPreferenceChangeListener} class which holds current preferences instance for delivering
 * correct {@link com.pushwoosh.thirdpart.com.ironz.binaryprefs.Preferences} instance to 2'st argument in
 * {@link OnSharedPreferenceChangeListener#onSharedPreferenceChanged(SharedPreferences, String)} method.
 */
public final class OnSharedPreferenceChangeListenerWrapper implements OnSharedPreferenceChangeListener {

	private final SharedPreferences currentPreferences;
	private final OnSharedPreferenceChangeListener listener;

	public OnSharedPreferenceChangeListenerWrapper(SharedPreferences currentPreferences, OnSharedPreferenceChangeListener listener) {
		this.currentPreferences = currentPreferences;
		this.listener = listener;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences unused, String key) {
		listener.onSharedPreferenceChanged(currentPreferences, key);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		OnSharedPreferenceChangeListenerWrapper that = (OnSharedPreferenceChangeListenerWrapper) o;

		if (listener != null ? !listener.equals(that.listener) : that.listener != null) {
			return false;
		}
		return currentPreferences != null ? currentPreferences.equals(that.currentPreferences) : that.currentPreferences == null;
	}

	@Override
	public int hashCode() {
		int result = listener != null ? listener.hashCode() : 0;
		result = 31 * result + (currentPreferences != null ? currentPreferences.hashCode() : 0);
		return result;
	}
}