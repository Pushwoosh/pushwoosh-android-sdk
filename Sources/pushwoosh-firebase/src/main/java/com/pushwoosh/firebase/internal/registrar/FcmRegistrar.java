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

package com.pushwoosh.firebase.internal.registrar;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.pushwoosh.PushwooshWorkManagerHelper;
import com.pushwoosh.firebase.internal.checker.FirebaseChecker;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.tags.TagsBundle;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

import static com.pushwoosh.internal.platform.AndroidPlatformModule.NULL_CONTEXT_MESSAGE;

import org.json.JSONObject;

public class FcmRegistrar implements PushRegistrar {

	private Impl impl;

	@Override
	public void init() {
		new FirebaseChecker().check();
		impl = new Impl();
	}

	@Override
	public void checkDevice(String appId) throws Exception {
		impl.checkDevice(appId);
	}

	@Override
	public void registerPW(TagsBundle tags) {
		impl.registerPW(tags);
	}

	@Override
	public void unregisterPW() {
		impl.unregisterPW();
	}

	private static class Impl {
		private static final String TAG = "PushRegistrarFCM";

		/**
		 * Permission necessary to receive GCM intents.
		 */
		private static final String PERMISSION_FCM_INTENTS =
				"com.google.android.c2dm.permission.RECEIVE";

		@Nullable
		private final Context context;
		private final RegistrationPrefs registrationPrefs;

		private Impl() {
			context = AndroidPlatformModule.getApplicationContext();
			registrationPrefs = RepositoryModule.getRegistrationPreferences();
		}

		void checkDevice(final String appId) throws Exception {
			String senderId = registrationPrefs.projectId().get();

			GeneralUtils.checkNotNullOrEmpty(appId, "mAppId");
			GeneralUtils.checkNotNullOrEmpty(senderId, "mSenderId");

			// Make sure the manifest was properly set - comment out this line
			// while developing the app, then uncomment it when it's ready.
			if(context == null){
				PWLog.error(NULL_CONTEXT_MESSAGE);
				return;
			}

			checkManifest(context);
		}

		void registerPW(TagsBundle tags) {
			String tagsJson = null;
			if (tags != null) {
				tagsJson = tags.toJson().toString();
			}
			Data inputData = new Data.Builder()
					.putBoolean(FcmRegistrarWorker.DATA_REGISTER, true)
					.putString(FcmRegistrarWorker.DATA_TAGS, tagsJson)
					.build();
			OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FcmRegistrarWorker.class)
					.setInputData(inputData)
					.setConstraints(PushwooshWorkManagerHelper.getNetworkAvailableConstraints())
					.build();
			PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(request, FcmRegistrarWorker.TAG, ExistingWorkPolicy.REPLACE);
		}

		void unregisterPW() {
			Data inputData = new Data.Builder()
					.putBoolean(FcmRegistrarWorker.DATA_UNREGISTER, true)
					.build();
			OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FcmRegistrarWorker.class)
					.setInputData(inputData)
					.setConstraints(PushwooshWorkManagerHelper.getNetworkAvailableConstraints())
					.build();
			PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(request, FcmRegistrarWorker.TAG, ExistingWorkPolicy.REPLACE);
		}

		/**
		 * Checks that the application manifest is properly configured.
		 * <p/>
		 * A proper configuration means:
		 * <ol>
		 * {@value FcmRegistrar.Impl#PERMISSION_FCM_INTENTS} permission.
		 * </ol>
		 * <p/>
		 * This method should be used during development time to verify that the
		 * manifest is properly set up, but it doesn't need to be called once the
		 * application is deployed to the users' devices.
		 *
		 * @param context application context.
		 * @throws IllegalStateException if any of the conditions above is not met.
		 */
		@SuppressWarnings("WrongConstant")
		static void checkManifest(@NonNull Context context) {
			PackageManager packageManager = context.getPackageManager();
			// check permission
			try {
				packageManager.getPermissionInfo(PERMISSION_FCM_INTENTS,
						PackageManager.GET_PERMISSIONS);
			} catch (NameNotFoundException e) {
				throw new IllegalStateException(
						"Application does not define permission " + PERMISSION_FCM_INTENTS);
			}
		}
	}
}
