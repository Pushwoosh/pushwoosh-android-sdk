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

package com.pushwoosh.internal.platform.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.BinaryPreferencesBuilder;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.event.ExceptionHandler;

/**
 * Binary prefs provider doesn't used started with 5.5.0 needed to correct migration to early versions
 * @see <a href="https://github.com/iamironz/binaryprefs">In github</a>
 */
class BinaryPrefsProvider implements PrefsProvider{
	private final Context context;
	private static final String DEFAULT_PUSHWOOSH_PREFS = "pushwoosh_default";

	private final ExceptionHandler exceptionHandler = e -> PWLog.debug("Prefs", e);

	BinaryPrefsProvider(final Context context) {
		this.context = context;
	}

	@Override
	public SharedPreferences providePrefs(final String tag) {
		return new BinaryPreferencesBuilder(context)
				.name(tag)
				.supportInterProcess(false)
				.exceptionHandler(exceptionHandler)
				.build();
	}

	@Override
	public SharedPreferences provideDefault() {
		return new BinaryPreferencesBuilder(context)
				.name(DEFAULT_PUSHWOOSH_PREFS)
				.supportInterProcess(false)
				.exceptionHandler(exceptionHandler)
				.build();
	}
}
