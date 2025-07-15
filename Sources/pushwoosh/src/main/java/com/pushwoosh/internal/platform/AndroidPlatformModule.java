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

package com.pushwoosh.internal.platform;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.PermissionController;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.app.ContextAppInfoProvider;
import com.pushwoosh.internal.platform.manager.ContextManagerProvider;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.platform.prefs.PrefsFactory;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.platform.prefs.migration.PrefsMigration;
import com.pushwoosh.internal.platform.reciever.ContextReceiverProvider;
import com.pushwoosh.internal.platform.reciever.ReceiverProvider;
import com.pushwoosh.internal.platform.resource.ContextResourceProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
import com.pushwoosh.internal.utils.TimeProvider;

import java.lang.ref.WeakReference;

/**
 * Android specific managers and providers to separate android and business logic
 */
public final class AndroidPlatformModule {
	private static final AndroidPlatformModule ANDROID_PLATFORM_MODULE = new AndroidPlatformModule();
	public static final String NULL_CONTEXT_MESSAGE = "Incorrect state of app. Context is null";

	private ManagerProvider managerProvider;
	private AppInfoProvider appInfoProvider;
	private ResourceProvider resourceProvider;
	private PrefsProvider prefsProvider;
	private ReceiverProvider receiverProvider;
	private ApplicationOpenDetector applicationOpenDetector;
	private WeakReference<Context> context;
	private PrefsMigration prefsMigration;
	private TimeProvider timeProvider;
	private PermissionController permissionController;
	private ApplicationState applicationState;

	public static void init(Context context, boolean force) {
        if (context == null) {
            return;
        }

        if (isInit() && !force) {
            return;
        }
        ANDROID_PLATFORM_MODULE.initProviders(context);
    }

	public static void init(Context context){
    	init(context, false);
	}

    public static boolean isInit() {
        return ANDROID_PLATFORM_MODULE.context != null && ANDROID_PLATFORM_MODULE.context.get() != null;
    }

	public PermissionController getPermissionController() {
		return permissionController;
	}

	private void initProviders(@NonNull final Context context) {
		timeProvider = new TimeProvider();

		this.context = new WeakReference<>(context.getApplicationContext());
		prefsProvider = PrefsFactory.createPrefsProvider();
		prefsMigration = PrefsFactory.createPrefsMigration();

		managerProvider = new ContextManagerProvider(context);
		appInfoProvider = new ContextAppInfoProvider(context);
		resourceProvider = new ContextResourceProvider(context);
		receiverProvider = new ContextReceiverProvider(context);

		applicationOpenDetector = new ApplicationOpenDetector(context);
		permissionController = new PermissionController(context, managerProvider.getNotificationManager());
		applicationState = new ApplicationState();
	}

	public static AndroidPlatformModule getInstance() {
		return ANDROID_PLATFORM_MODULE;
	}

	private AndroidPlatformModule() {/*do nothing*/}

	public static TimeProvider getTimeProvide(){
		return ANDROID_PLATFORM_MODULE.timeProvider;
	}

	public static ManagerProvider getManagerProvider() {
		return ANDROID_PLATFORM_MODULE.managerProvider;
	}

	public static AppInfoProvider getAppInfoProvider() {
		return ANDROID_PLATFORM_MODULE.appInfoProvider;
	}

	public static ResourceProvider getResourceProvider() {
		return ANDROID_PLATFORM_MODULE.resourceProvider;
	}

	public static PrefsProvider getPrefsProvider() {
		return ANDROID_PLATFORM_MODULE.prefsProvider;
	}

	public static ReceiverProvider getReceiverProvider() {
		return ANDROID_PLATFORM_MODULE.receiverProvider;
	}

	public static ApplicationOpenDetector getApplicationOpenDetector() {
		return ANDROID_PLATFORM_MODULE.applicationOpenDetector;
	}

	@Nullable
	public static Context getApplicationContext() {
		return ANDROID_PLATFORM_MODULE.context == null ? null : ANDROID_PLATFORM_MODULE.context.get();
	}

	public static PrefsMigration getPrefsMigration() {
		return ANDROID_PLATFORM_MODULE.prefsMigration;
	}

	public TimeProvider getTimeProvider() {
		return timeProvider;
	}

	public static boolean isApplicationInForeground() {
		return ANDROID_PLATFORM_MODULE.applicationState.isForeground();
	}
}
