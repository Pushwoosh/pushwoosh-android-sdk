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

package com.pushwoosh.notification.handlers.message.system;

import android.os.Bundle;
import android.text.TextUtils;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

class LogLevelMessageSystemHandler extends CommandMessageSystemHandler {

	private static final String KEY_SET_LOG_LEVEL = "setLogLevel";
	private final RegistrationPrefs registrationPrefs;

	//Send push with the following root params to change log level:
	//{"pw_system_push":1, "pw_command":"setLogLevel", "value":"INFO"}
	LogLevelMessageSystemHandler() {
		registrationPrefs = RepositoryModule.getRegistrationPreferences();
	}

	@Override
	protected boolean handleCommand(final Bundle pushBundle, final String command) {
		if (TextUtils.equals(KEY_SET_LOG_LEVEL, command)) {
			String logLevel = PushBundleDataProvider.getValue(pushBundle);
			if (logLevel != null) {
				registrationPrefs.logLevel().set(logLevel);
				PWLog.updateLogLevel(logLevel);
			}
			return true;
		}

		return false;
	}
}
