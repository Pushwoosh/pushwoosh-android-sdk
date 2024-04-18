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

package com.pushwoosh.amazon.internal.registrar;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.device.messaging.ADM;
import com.pushwoosh.PushAmazonHandlerJob;
import com.pushwoosh.amazon.TagsRegistrarHelper;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.tags.TagsBundle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdmRegistrar implements PushRegistrar {

	private Impl impl;

	@Override
	public void init() {
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
		private final ADM adm;
		@Nullable
		private Context context;

		/**
		 * Permission necessary to receive ADM intents.
		 */
		private static final String PERMISSION_ADM_INTENTS = "com.amazon.device.messaging.permission.SEND";
		/**
		 * Intent sent by ADM indicating with the result of a registration request.
		 */
		private static final String INTENT_FROM_ADM_REGISTRATION_CALLBACK = "com.amazon.device.messaging.intent.REGISTRATION";
		/**
		 * Intent sent by ADM containing a message.
		 */
		private static final String INTENT_FROM_ADM_MESSAGE = "com.amazon.device.messaging.intent.RECEIVE";


		private static final String TAG = "ADMRegistrar";

		Impl() {
			context = AndroidPlatformModule.getApplicationContext();
			adm = new ADM(context);
		}

		void checkDevice(final String appId) throws Exception {
			GeneralUtils.checkNotNullOrEmpty(appId, "mAppId");

			// Make sure the device has the proper dependencies.
			if (!adm.isSupported()) {
				throw new UnsupportedOperationException("ADM is not supported on the current device");
			}

			if(context == null){
				PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
				return;
			}

			// Make sure the manifest was properly set - comment out this line
			// while developing the app, then uncomment it when it's ready.
			checkManifest(context);
		}

		void registerPW(TagsBundle tagsBundle) {
			adm.startRegister();
			TagsRegistrarHelper.tagsBundle = tagsBundle;
		}

		void unregisterPW() {
			adm.startUnregister();
		}

		/**
		 * Checks that the application manifest is properly configured.
		 * <p/>
		 * A proper configuration means:
		 * <ol>
		 * <li>It creates a custom permission called
		 * {@code PACKAGE_NAME.permission.RECEIVE_ADM_MESSAGE}.
		 * <li>It defines at least one {@link android.content.BroadcastReceiver} with category
		 * {@code PACKAGE_NAME}.
		 * <li>The {@link android.content.BroadcastReceiver}(s) uses the
		 * {@value AdmRegistrar.Impl#PERMISSION_ADM_INTENTS} permission.
		 * <li>The {@link android.content.BroadcastReceiver}(s) handles the 2 ADM intents
		 * ({@value AdmRegistrar.Impl#INTENT_FROM_ADM_MESSAGE},
		 * and {@value AdmRegistrar.Impl#INTENT_FROM_ADM_REGISTRATION_CALLBACK}).
		 * </ol>
		 * ...where {@code PACKAGE_NAME} is the application package.
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
			String packageName = context.getPackageName();
			String permissionName = packageName + ".permission.RECEIVE_ADM_MESSAGE";
			// check permission
			try {
				//noinspection ConstantConditions
				packageManager.getPermissionInfo(permissionName, PackageManager.GET_PERMISSIONS);
			} catch (NameNotFoundException e) {
				throw new IllegalStateException(
						"Application does not define permission " + permissionName);
			}
			// check receivers
			PackageInfo receiversInfo;
			try {
				receiversInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_RECEIVERS);
			} catch (NameNotFoundException e) {
				throw new IllegalStateException("Could not get receivers for package " + packageName);
			}
			ActivityInfo[] receivers = receiversInfo.receivers;
			if (receivers == null || receivers.length == 0) {
				throw new IllegalStateException("No receiver for package " + packageName);
			}
			if (PWLog.isLoggable(TAG, PWLog.VERBOSE)) {
				PWLog.noise(TAG, "number of receivers for " + packageName + ": " + receivers.length);
			}
			Set<String> allowedReceivers = new HashSet<>();
			for (ActivityInfo receiver : receivers) {
				if (PERMISSION_ADM_INTENTS.equals(receiver.permission)) {
					allowedReceivers.add(receiver.name);
				}
			}
			if (allowedReceivers.isEmpty()) {
				throw new IllegalStateException("No receiver allowed to receive " + PERMISSION_ADM_INTENTS);
			}
			checkReceiver(context, allowedReceivers, INTENT_FROM_ADM_REGISTRATION_CALLBACK);
			checkReceiver(context, allowedReceivers, INTENT_FROM_ADM_MESSAGE);
		}

		@SuppressWarnings("WrongConstant")
		private static void checkReceiver(@NonNull Context context,
										  @NonNull Set<String> allowedReceivers,
										  @Nullable String action) {
			PackageManager pm = context.getPackageManager();
			String packageName = context.getPackageName();
			Intent intent = new Intent(action);
			intent.setPackage(packageName);
			//noinspection ConstantConditions
			List<ResolveInfo> receivers = pm.queryBroadcastReceivers(intent, PackageManager.GET_INTENT_FILTERS);
			if (receivers.isEmpty()) {
				throw new IllegalStateException("No receivers for action " + action);
			}
			if (PWLog.isLoggable(TAG, PWLog.VERBOSE)) {
				PWLog.noise(TAG, "Found " + receivers.size() + " receivers for action " + action);
			}
			// make sure receivers match
			for (ResolveInfo receiver : receivers) {
				//noinspection ConstantConditions
				String name = receiver.activityInfo.name;
				if (!allowedReceivers.contains(name)) {
					throw new IllegalStateException("Receiver " + name + " is not set with permission " + PERMISSION_ADM_INTENTS);
				}
			}
		}
	}

}
