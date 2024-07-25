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

package com.pushwoosh.internal.network;

import androidx.annotation.Nullable;
import android.os.AsyncTask;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;

/**
 * DI to provide {@link com.pushwoosh.internal.network.RequestManager}
 */
public class NetworkModule {
	private static RequestManager requestManager;

	public static void init(RegistrationPrefs registrationPrefs,
							ServerCommunicationManager serverCommunicationManager) {
		if (requestManager == null) {
			requestManager = new PushwooshRequestManager(registrationPrefs, serverCommunicationManager);
		}
	}

	@Nullable
	public static RequestManager getRequestManager() {
		return requestManager;
	}

	public static void setRequestManager(RequestManager requestManager) {
		NetworkModule.requestManager = requestManager;
	}

	/**
	 * Execute {@param task} on background thread
	 * @param task - task for executing
	 */
	public static void execute(Runnable task) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				try {
					task.run();
				} catch (Exception e) {
					PWLog.error(e.getMessage());
				}
				return null;
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
