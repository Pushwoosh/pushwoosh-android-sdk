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

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main thread preference change listener bridge
 */
public final class MainThreadEventBridge implements EventBridge {

	private final List<OnSharedPreferenceChangeListener> currentListeners;

	private final Handler handler = new Handler();

	public MainThreadEventBridge(String prefName, Map<String, List<OnSharedPreferenceChangeListener>> allListeners) {
		this.currentListeners = putIfAbsentListeners(prefName, allListeners);
	}

	private List<OnSharedPreferenceChangeListener> putIfAbsentListeners(String prefName, Map<String, List<OnSharedPreferenceChangeListener>> allListeners) {
		if (allListeners.containsKey(prefName)) {
			return allListeners.get(prefName);
		}
		List<OnSharedPreferenceChangeListener> listeners = new ArrayList<>();
		allListeners.put(prefName, listeners);
		return listeners;
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		currentListeners.add(listener);
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		currentListeners.remove(listener);
	}

	@Override
	public void notifyListenersUpdate(final String key, byte[] bytes) {
		notifyListeners(key);
	}

	@Override
	public void notifyListenersRemove(String key) {
		notifyListeners(key);
	}

	private void notifyListeners(final String key) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				List<OnSharedPreferenceChangeListener> temp = new ArrayList<>(currentListeners);
				for (OnSharedPreferenceChangeListener listener : temp) {
					listener.onSharedPreferenceChanged(null, key);
				}
			}
		});
	}
}