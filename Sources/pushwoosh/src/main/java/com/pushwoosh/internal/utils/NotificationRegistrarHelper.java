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

package com.pushwoosh.internal.utils;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

import com.pushwoosh.HandleMessageWorker;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.PushwooshWorkManagerHelper;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.PushBundleStorage;
import com.pushwoosh.repository.RepositoryModule;

import java.util.concurrent.TimeUnit;


public final class NotificationRegistrarHelper {

	private NotificationRegistrarHelper() {/*do nothing*/}

	public static boolean isRegisteredForRemoteNotifications() {
		return RepositoryModule.getRegistrationPreferences().isRegisteredForPush().get();
	}

	public static void onRegisteredForRemoteNotifications(final String registrationId, String tagsJson) {
		PWLog.noise("NotificationRegistrarHelper", String.format("onRegisteredForRemoteNotifications: %s", registrationId));
		// this if checks whether device is registered with Pushwoosh and does not allow passing a token if it is not
		//todo: remove this check
		if (!isRegisteredForRemoteNotifications()) {
			PWLog.warn("NotificationRegistrarHelper", "device should be registered directly for continue, abort");
			return;
		}

		PushwooshNotificationManager notificationManager = PushwooshPlatform.getInstance().notificationManager();
		notificationManager.onRegisteredForRemoteNotifications(registrationId, tagsJson);
	}

	public static void clearToken() {
		RepositoryModule.getRegistrationPreferences().pushToken().set("");
	}

	public static void onUnregisteredFromRemoteNotifications(final String registrationId) {
		PushwooshNotificationManager notificationManager = PushwooshPlatform.getInstance().notificationManager();
		notificationManager.onUnregisteredFromRemoteNotifications(registrationId);
	}

	public static void onFailedToRegisterForRemoteNotifications(final String errorId) {
		PushwooshNotificationManager notificationManager = PushwooshPlatform.getInstance().notificationManager();
		notificationManager.onFailedToRegisterForRemoteNotifications(errorId);
	}

	public static void handleMessage(final Bundle bundle) {
		boolean handleUsingWorkManager = RepositoryModule.getNotificationPreferences() != null &&
				RepositoryModule.getNotificationPreferences().handleNotificationsUsingWorkManager().get();
		if (handleUsingWorkManager) {
			handleMessageUsingWorkManager(bundle);
		} else {
			handleMessageBundle(bundle);
		}
	}

	public static void onFailedToUnregisterFromRemoteNotifications(String message) {
		PushwooshNotificationManager notificationManager = PushwooshPlatform.getInstance().notificationManager();
		notificationManager.onFailedToUnregisterFromRemoteNotifications(message);
	}

	public static void handleMessageBundle(final Bundle bundle) {
		try {
			NotificationServiceExtension notificationServiceExtension = PushwooshPlatform.getInstance().notificationService();
			notificationServiceExtension.handleMessage(bundle);
		} catch (Exception e) {
			PWLog.exception(e);
		}
	}

	private static void handleMessageUsingWorkManager(final Bundle bundle) {
		new ScheduleHandleMessageWorkerTask(bundle, () -> handleMessageBundle(bundle))
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private static class ScheduleHandleMessageWorkerTask extends AsyncTask<Void, Void, Boolean> {
		final Bundle pushBundle;
		final HandleRemoteMessageFailureCallback failureCallback;

		public ScheduleHandleMessageWorkerTask(Bundle pushBundle,
											   HandleRemoteMessageFailureCallback failureCallback) {
			this.pushBundle = pushBundle;
			this.failureCallback = failureCallback;
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			PushBundleStorage storage = RepositoryModule.getPushBundleStorage();
			if (storage != null) {
				try {
					long id = storage.putPushBundle(pushBundle);
					scheduleWorker(id);
				} catch (Exception e) {
					return false;
				}
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			super.onPostExecute(success);
			if (!success && failureCallback != null) {
				failureCallback.onFail();
			}
		}

		private void scheduleWorker(long id) {
			Data inputData = new Data.Builder()
					.putLong(HandleMessageWorker.DATA_PUSH_BUNDLE_ID, id)
					.build();
			OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(HandleMessageWorker.class)
					.setInputData(inputData)
					.setConstraints(PushwooshWorkManagerHelper.getNetworkAvailableConstraints())
					.setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
					.build();
			PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(request,
					HandleMessageWorker.TAG,
					ExistingWorkPolicy.APPEND);
		}
	}

	private interface HandleRemoteMessageFailureCallback {
		void onFail();
	}

}
