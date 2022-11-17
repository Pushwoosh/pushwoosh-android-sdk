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

package com.pushwoosh.internal.crash;

import android.text.TextUtils;

import java.util.Collections;
import java.util.List;

import com.pushwoosh.BuildConfig;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.NativePluginProvider;
import com.pushwoosh.repository.RepositoryModule;

/**
 * Crash reporter for Pushwoosh. It is based on Rollbar.
 */
final class CrashConfig {
	/**
	 * Is crash report enabled
	 */
	static final boolean CRASH_REPORT_ENABLED = !BuildConfig.DEBUG;

	/**
	 * Crash report url.
	 */
	static final String BASE_CRASH_REPORT_URL = "https://api.rollbar.com/";

	/**
	 * Rollbar api token.
	 */
	static final String API_TOKEN = "TOKEN";

	/**
	 * Header name for api token.
	 */
	static final String API_TOKEN_HEADER = "X-Rollbar-Access-Token";

	/**
	 * Track crashed, contains only one of this packages.
	 */
	static final List<String> CATCH_PACKAGES = Collections.singletonList("com.pushwoosh");


	static String framework() {
		if (PushwooshPlatform.getInstance() != null
				&& PushwooshPlatform.getInstance().getConfig() != null
				&& PushwooshPlatform.getInstance().getConfig().getPluginProvider() != null) {
			return PushwooshPlatform.getInstance().getConfig().getPluginProvider().getPluginType();
		} else {
			return NativePluginProvider.NATIVE_PLUGIN_TYPE;
		}
	}

	static String appCode() {
		String appCode = Pushwoosh.getInstance().getApplicationCode();
		if (TextUtils.isEmpty(appCode)) {
			appCode = "Not yet initialized";
		}
		return appCode;
	}

	static String hwid() {
		String hwid = Pushwoosh.getInstance().getHwid();
		if (TextUtils.isEmpty(hwid)) {
			hwid = "Not yet generated";
		}
		return hwid;
	}

	static boolean isCollectingDeviceModelAllowed() {
		boolean flag = false;
		if (RepositoryModule.getNotificationPreferences() != null
				&& RepositoryModule.getNotificationPreferences()
						.isCollectingDeviceModelAllowed() != null) {
			flag = RepositoryModule
					.getNotificationPreferences()
					.isCollectingDeviceModelAllowed()
					.get();
		}
		return flag;
	}

	static boolean isCollectingDeviceOsVersionAllowed() {
		boolean flag = false;
		if (RepositoryModule.getNotificationPreferences() != null
				&& RepositoryModule.getNotificationPreferences()
						.isCollectingDeviceOsVersionAllowed() != null) {
			flag = RepositoryModule
					.getNotificationPreferences()
					.isCollectingDeviceOsVersionAllowed()
					.get();
		}
		return flag;
	}
}
