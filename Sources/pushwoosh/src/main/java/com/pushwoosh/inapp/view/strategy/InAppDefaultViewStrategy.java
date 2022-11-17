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
import android.os.AsyncTask;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.view.RichMediaWebActivity;
import com.pushwoosh.internal.utils.PWLog;

import java.lang.ref.WeakReference;

class InAppDefaultViewStrategy implements ResourceViewStrategy {
	private static final String TAG = "[InApp]InAppDefaultViewStrategy";

	private final Context context;
	private final InAppFolderProvider inAppFolderProvider;

	InAppDefaultViewStrategy(Context context, InAppFolderProvider inAppFolderProvider) {
		this.context = context;
		this.inAppFolderProvider = inAppFolderProvider;
	}

	@Override
	public void show(Resource resource) {
		if (resource == null) {
			PWLog.noise(TAG, "resource is empty");
			return;
		}

		new ShowInAppTask(this, resource, () -> {
			if (inAppFolderProvider.isInAppDownloaded(resource.getCode())) {
				context.startActivity(RichMediaWebActivity.createInAppIntent(context, resource));
			} else {
				PWLog.noise(TAG, "resource is not downloaded, abort show inApp");
			}
		}).execute();
	}

	private static class ShowInAppTask extends AsyncTask<Void, Void, Boolean> {
		private final WeakReference<InAppDefaultViewStrategy> weakRef;
		private final Resource resource;
		private final OnShowInAppFailureCallback callback;

		public ShowInAppTask(InAppDefaultViewStrategy inAppDefaultViewStrategy,
							 Resource resource,
							 OnShowInAppFailureCallback callback) {
			this.weakRef = new WeakReference<>(inAppDefaultViewStrategy);
			this.resource = resource;
			this.callback = callback;
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			if (weakRef.get() != null) {
				return weakRef.get().inAppFolderProvider.isInAppDownloaded(resource.getCode());
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean isInAppDownloaded) {
			super.onPostExecute(isInAppDownloaded);
			if (isInAppDownloaded && weakRef.get() != null) {
				Context context = weakRef.get().context;
				context.startActivity(RichMediaWebActivity.createInAppIntent(context, resource));
			} else {
				callback.onFail();
			}
		}
	}

	private interface OnShowInAppFailureCallback {
		void onFail();
	}
}
