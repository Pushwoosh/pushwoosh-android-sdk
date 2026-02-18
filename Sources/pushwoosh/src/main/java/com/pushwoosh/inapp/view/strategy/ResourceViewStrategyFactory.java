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
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.ModalRichMediaWindow;
import com.pushwoosh.inapp.view.RichMediaWebActivity;
import com.pushwoosh.inapp.view.strategy.model.ResourceType;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.richmedia.RichMediaType;

public class ResourceViewStrategyFactory {
	private static final String TAG = "[InApp]ResourceViewStrategyFactory";
	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	public void showResource(ResourceWrapper resourceWrapper) {
		if (resourceWrapper == null) {
			PWLog.warn(TAG, "resourceWrapper is null, aborting show");
			return;
		}

		Resource resource = resourceWrapper.getResource();
		if (resource == null) {
			PWLog.warn(TAG, "resource is null, aborting show");
			return;
		}

		Context context = AndroidPlatformModule.getApplicationContext();
		if (context == null) {
			PWLog.error(TAG, "context is null");
			return;
		}

		PWLog.info(TAG, String.format("presenting resource %s with code: %s, url: %s",
                resourceWrapper.getResourceType(), resource.getCode(), resource.getUrl()));

		try {
			if (resourceWrapper.getResourceType() == ResourceType.IN_APP) {
				showInApp(context, resource);
			} else if (resourceWrapper.isLockScreen()) {
				showRichMediaLockScreen(context, resource, resourceWrapper.getSound());
			} else {
				showRichMedia(context, resource, resourceWrapper.getDelay());
			}
		} catch (Throwable t) {
			PWLog.error(TAG, "Failed to show resource", t);
		}
	}

	private void showInApp(Context context, Resource resource) {
		PushwooshPlatform.getInstance().pushwooshRepository().setCurrentInAppCode(resource.getCode());
		PushwooshPlatform.getInstance().pushwooshRepository().setCurrentRichMediaCode(null);

		if (RichMediaManager.getRichMediaType() == RichMediaType.MODAL) {
			ModalRichMediaWindow.showModalRichMediaWindow(resource);
		} else if (RichMediaManager.getRichMediaType() == RichMediaType.DEFAULT) {
			Intent intent = RichMediaWebActivity.createInAppIntent(context, resource);
			mainHandler.post(() -> context.startActivity(intent));
		}
	}

	private void showRichMedia(Context context, Resource resource, long delay) {
		String richMediaCode = resource.getCode().substring(2);
		PushwooshPlatform.getInstance().pushwooshRepository().setCurrentRichMediaCode(richMediaCode);
		PushwooshPlatform.getInstance().pushwooshRepository().setCurrentInAppCode(null);

		if (RichMediaManager.getRichMediaType() == RichMediaType.MODAL) {
			ModalRichMediaWindow.showModalRichMediaWindow(resource);
		} else if (RichMediaManager.getRichMediaType() == RichMediaType.DEFAULT) {
			Intent intent = RichMediaWebActivity.createRichMediaIntent(context, resource);
			mainHandler.postDelayed(() -> context.startActivity(intent), delay);
		}
	}

	private void showRichMediaLockScreen(Context context, Resource resource, String sound) {
		Intent intent = RichMediaWebActivity.createRichMediaLockScreenIntent(context, resource, sound);
		context.startActivity(intent);
	}
}
