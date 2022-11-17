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

package com.pushwoosh.notification.handlers.message.user;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.ApplicationOpenDetector;
import com.pushwoosh.internal.utils.LockScreenUtils;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.SilentRichMediaStorage;
import com.pushwoosh.richmedia.RichMediaController;

import org.json.JSONException;
import org.json.JSONObject;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

class RichMediaMessageHandler extends NotificationMessageHandler {
	private static final String PW_FORCE_SHOW_RICH_MEDIA_FLAG = "pw_force_show_rm";
	RichMediaMessageHandler() {
		EventBus.subscribe(
				ApplicationOpenDetector.ApplicationMovedToForegroundEvent.class,
				event -> new ShowSilentRichMediaTask().executeOnExecutor(THREAD_POOL_EXECUTOR));
	}

	@Override
	public void handlePushMessage(final PushMessage pushMessage) {
		String richMedia = PushBundleDataProvider.getRichMedia(pushMessage.toBundle());
		if (richMedia != null) {
			loadRichMedia(richMedia);
		}

		super.handlePushMessage(pushMessage);
	}

	@Override
	protected void handleNotification(final PushMessage pushMessage) {
		final String richMedia = PushBundleDataProvider.getRichMedia(pushMessage.toBundle());
		final String sound = pushMessage.getSound();

		if (pushMessage.isLockScreen()) {
			if (LockScreenUtils.isScreenLocked()) {
				showResource(richMedia, sound, true);
			} else {
				RepositoryModule.getLockScreenMediaStorage().cacheResource(pushMessage);
			}
		} else if (isSilentAndForceShowRmFlagPresent(pushMessage)) {
			if (AndroidPlatformModule.isApplicationInForeground()) {
				showResource(richMedia, sound, false);
			} else {
				RepositoryModule.getSilentRichMediaStorage().replaceResource(pushMessage);
			}
		}
	}

	private void showResource(String richMedia, String sound, boolean isLockScreen) {
		ResourceWrapper resourceWrapper = new ResourceWrapper.Builder()
				.setRichMedia(richMedia)
				.setSound(sound)
				.setLockScreen(isLockScreen)
				.build();

		if (resourceWrapper == null) {
			return;
		}

		new ShowResourceWrapperTask(resourceWrapper).executeOnExecutor(THREAD_POOL_EXECUTOR);
	}

	private void loadRichMedia(final String richMedia) {
		if (InAppModule.getInAppRepository() != null) {
			InAppModule.getInAppRepository().prefetchRichMedia(richMedia);
			PushwooshPlatform.getInstance().pushwooshRepository().prefetchTags();
		}
	}

	private boolean isSilentAndForceShowRmFlagPresent(PushMessage pushMessage) {
		if (pushMessage == null || TextUtils.isEmpty(pushMessage.getCustomData()) || !pushMessage.isSilent()) {
			return false;
		}
		try {
			JSONObject jsonObject = new JSONObject(pushMessage.getCustomData());
			return jsonObject.getBoolean(PW_FORCE_SHOW_RICH_MEDIA_FLAG);
		} catch (JSONException e) {
			return false;
		}
	}

	private static class ShowResourceWrapperTask extends AsyncTask<Void, Void, Void> {
		private final ResourceWrapper resourceWrapper;

		ShowResourceWrapperTask(ResourceWrapper resourceWrapper) {
			this.resourceWrapper = resourceWrapper;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			showRecourseWrapper(resourceWrapper);
			return null;
		}

	}
	private static class ShowSilentRichMediaTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... voids) {
			SilentRichMediaStorage silentRichMediaStorage = RepositoryModule.getSilentRichMediaStorage();
			ResourceWrapper resourceWrapper = silentRichMediaStorage.getResourceWrapper();
			if (resourceWrapper != null) {
				showRecourseWrapper(resourceWrapper);
			}
			return null;
		}
	}

	private static void showRecourseWrapper(ResourceWrapper resourceWrapper) {
		RichMediaController richMediaController = PushwooshPlatform.getInstance().getRichMediaController();
		if (richMediaController != null) {
			richMediaController.showResourceWrapper(resourceWrapper);
		}
	}
}
