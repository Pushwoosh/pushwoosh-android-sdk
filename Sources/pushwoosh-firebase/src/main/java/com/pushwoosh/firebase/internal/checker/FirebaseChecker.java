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

package com.pushwoosh.firebase.internal.checker;

import com.pushwoosh.internal.checker.Checker;
import com.pushwoosh.internal.utils.PWLog;

public class FirebaseChecker implements Checker {
	@Override
	public boolean check() {
		try {
			Class.forName("com.google.firebase.messaging.FirebaseMessaging");
		} catch (ClassNotFoundException e) {
			final String message = "com.google.firebase:firebase-messaging is missing. It is normally pulled in transitively by "
					+ "pushwoosh-firebase; add it explicitly only if you excluded it, preferably via the Firebase BoM:\n"
					+ "    implementation(platform(\"com.google.firebase:firebase-bom:<version>\"))\n"
					+ "    implementation(\"com.google.firebase:firebase-messaging\")\n"
					+ "See https://firebase.google.com/docs/android/setup#available-libraries";
			PWLog.error(message);
			throw new IllegalStateException(message, e);
		}
		return false;
	}
}
