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

package com.pushwoosh.inapp.view.strategy;

import android.content.Context;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.RemoteUrlActivity;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.utils.PWLog;

class RemoteUrlDefaultViewStrategy implements ResourceViewStrategy {
	private static final String TAG = "[InApp]RemoteUrlDefaultViewStrategy";

	private final Context context;

	RemoteUrlDefaultViewStrategy(Context context) {
		this.context = context;
	}

	@Override
	public void show(Resource resource) {
		if (resource == null) {
			PWLog.noise(TAG, "resource is empty");
			return;
		}

		if (!GeneralUtils.isNetworkAvailable()) {
			PWLog.error(TAG, "Remote page error: network unavailable");
			return;
		}

		RemoteUrlActivity.startRemotePage(context, resource.getUrl());
	}
}
