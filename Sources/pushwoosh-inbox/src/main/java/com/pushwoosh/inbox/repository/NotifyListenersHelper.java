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

package com.pushwoosh.inbox.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inbox.exception.InboxMessagesException;
import com.pushwoosh.inbox.internal.mapping.Mapper;
import com.pushwoosh.inbox.repository.data.LoadResult;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.utils.PWLog;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

class NotifyListenersHelper<T> {

	private final Collection<Callback<T, InboxMessagesException>> listeners = new ConcurrentLinkedQueue<>();
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Mapper<Result<LoadResult, NetworkException>, Result<T, InboxMessagesException>> mapper;

	NotifyListenersHelper(Mapper<Result<LoadResult, NetworkException>, Result<T, InboxMessagesException>> mapper) {
		this.mapper = mapper;
	}

	void notify(final Result<LoadResult, NetworkException> result) {
		handler.post(() -> notifyListeners(mapper.map(result)));
	}

	void addListener(@Nullable Callback<T, InboxMessagesException> callback) {
		if (callback == null) {
			return;
		}

		listeners.add(callback);
	}

	private void notifyListeners(Result<T, InboxMessagesException> result) {
		for (Callback<T, InboxMessagesException> expectedCallback : listeners) {
			if (expectedCallback != null) {
				try {
					expectedCallback.process(result);
					listeners.remove(expectedCallback);
				} catch (Exception e) {
					PWLog.error("Error occurred while processing Callback", e.getMessage());
				}
			}
		}

		listeners.clear();
	}

}
