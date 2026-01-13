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

package com.pushwoosh.repository;

import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.platform.AndroidPlatformModuleTest;
import com.pushwoosh.internal.prefs.TestPrefsProvider;
import com.pushwoosh.internal.utils.Config;


public class RepositoryTestManager {
	public static Class<? extends PushRequest> getMessageDeliveryClass() {
		return MessageDeliveredRequest.class;
	}

	public static Class<? extends PushRequest> getPushStatClass() {
		return PushStatRequest.class;
	}

	public static RegistrationPrefs createRegistrationPrefs(Config config, DeviceRegistrar deviceRegistrar) {
		AndroidPlatformModuleTest.changePrefsProvider(new TestPrefsProvider());
		return new RegistrationPrefs(config, deviceRegistrar);
	}

	public static void destroyRegistrationPrefs(RegistrationPrefs registrationPrefs) {
		registrationPrefs.pushToken().set(null);
		registrationPrefs.registeredOnServer().set(false);
		registrationPrefs.projectId().set(null);
		registrationPrefs.applicationId().set(null);
		registrationPrefs.lastPushRegistration().set(0);
		registrationPrefs.forceRegister().set(false);
		registrationPrefs.userId().set(null);
		registrationPrefs.deviceId().set(null);
		registrationPrefs.logLevel().set(null);
		registrationPrefs.baseUrl().set(null);
		RepositoryModule.setRegistrationPreferences(null);
	}
}
