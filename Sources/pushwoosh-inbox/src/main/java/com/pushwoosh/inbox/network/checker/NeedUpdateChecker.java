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

package com.pushwoosh.inbox.network.checker;

import android.text.TextUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import com.pushwoosh.internal.checker.Checker;
import com.pushwoosh.repository.RepositoryModule;

public class NeedUpdateChecker implements Checker {
	private final AtomicBoolean isLoading = new AtomicBoolean(false);
	private long lastTimeUpdate;
	private final Object mutex = new Object();
	private String currentUserId;

	private final long minUpdateTime;

	public NeedUpdateChecker(long minUpdateTime) {
		this.minUpdateTime = minUpdateTime;
	}

	@Override
	public boolean check() {
		synchronized (mutex) {
			String userId = RepositoryModule.getRegistrationPreferences().userId().get();

			if (currentUserId != null && !TextUtils.equals(userId, currentUserId)) {
				lastTimeUpdate = 0;
			}

			currentUserId = userId;

			long diff = System.currentTimeMillis() - lastTimeUpdate;
			return diff > minUpdateTime;
		}
	}

	public boolean isLoading() {
		return isLoading.get();
	}

	public void startLoading() {
		synchronized (mutex) {
			lastTimeUpdate = System.currentTimeMillis();
			isLoading.set(true);
		}
	}

	public void finishLoading() {
		synchronized (mutex) {
			isLoading.set(false);
		}
	}
}
