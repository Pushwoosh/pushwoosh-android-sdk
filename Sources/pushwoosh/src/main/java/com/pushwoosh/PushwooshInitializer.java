/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.pushwoosh.internal.crash.InternalCrashAnalyticsModule;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.LockScreenReceiver;
import com.pushwoosh.internal.utils.PWLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PushwooshInitializer {

	private static final String TAG = PushwooshInitializer.class.getSimpleName();
	private static final String NO_ATTACHED_PUSH_NOTIFICATIONS_PROVIDERS_FOUND_MESSAGE =
			"No attached push notifications providers have been found.\n" +
			"This error can be seen when you use 'pushwoosh-huawei' module\n" +
			"not on Huawei device or you have not added any module attaching\n" +
			"push notifications provider.\n" +
			"Pushwoosh supports Firebase, Amazon, Huawei, Baidu push notification providers.\n" +
			"See the integration guide https://docs.pushwoosh.com/platform-docs/pushwoosh-sdk/android-push-notifications";

	public static void init(Context context) {
		if (isComponentInit()) {
			PWLog.noise(TAG, "already init");
			return;
		}

		if (!DeviceSpecificProvider.isInited()) {
			// TODO: check decive specific provider is null
			// crash application there

		}

		AndroidPlatformModule.init(context);
		InternalCrashAnalyticsModule.init(context);

		//initialize Firebase PushRegistrar in Xamarin plugin:
		//must be removed in the PUSH-27936
		initFirebaseInXamarinPlugin(context);

		if (DeviceSpecificProvider.getInstance() == null) {
			PWLog.error(TAG, NO_ATTACHED_PUSH_NOTIFICATIONS_PROVIDERS_FOUND_MESSAGE);
			return;
		}

		PushwooshPlatform pushwooshPlatform = new PushwooshPlatform.Builder()
				.setConfig(new AndroidManifestConfig())
				.setPushRegistrar(DeviceSpecificProvider.getInstance().pushRegistrar())
				.build();


		pushwooshPlatform.onApplicationCreated();
		AndroidPlatformModule.getApplicationOpenDetector().onApplicationCreated(pushwooshPlatform.getAppVersionProvider().isFirstLaunch());

		// lock screen receiver
		LockScreenReceiver lockScreenReceiver = new LockScreenReceiver();
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_ANSWER);
		filter.addAction(Intent.ACTION_USER_PRESENT);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			context.registerReceiver(lockScreenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			context.registerReceiver(lockScreenReceiver, filter);
		}
		PWLog.noise(TAG, "Pushwoosh init finished");
	}

	private static void initFirebaseInXamarinPlugin(Context context) {
		try {
			Class xamarinPluginProviderClass = Class.forName("com.pushwoosh.xamarin.internal.XamarinPluginProvider");
			// class found, initializing Firebase
			Class initializerClass = Class.forName("com.pushwoosh.firebase.FirebaseInitializer");
			Method initMethod = initializerClass.getMethod("init", Context.class);
			initMethod.invoke(null, context);
		} catch (ClassNotFoundException e) {
			// ignore
		} catch (NoSuchMethodException e) {
			// ignore
		} catch (IllegalAccessException e) {
			// ignore
		} catch (InvocationTargetException e) {
			// ignore
		}
	}

	private static boolean isComponentInit() {
		return DeviceSpecificProvider.isInited()
				&& PushwooshPlatform.getInstance() != null
				&& AndroidPlatformModule.getApplicationContext() != null;
	}

}
