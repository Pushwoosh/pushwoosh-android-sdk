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

package com.pushwoosh.firebase.internal.specific;

import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.firebase.internal.registrar.FcmRegistrar;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecific;

import static com.pushwoosh.internal.specific.DeviceSpecificProvider.FCM_ANDROID_TYPE;

final class FcmDeviceSpecific implements DeviceSpecific {
	private static final int ANDROID_DEVICE_TYPE = 3;
	private static final String GCM_PERMISSION_SUFFIX = ".permission.C2D_MESSAGE";

	private final PushRegistrar pushRegistrar;

	FcmDeviceSpecific() {
		pushRegistrar = new FcmRegistrar();
	}

	@Override
	public PushRegistrar pushRegistrar() {
		return pushRegistrar;
	}

	public String permission(final String packageName) {
		return packageName + GCM_PERMISSION_SUFFIX;
	}

	@Override
	public int deviceType() {
		return ANDROID_DEVICE_TYPE;
	}

	@Override
	public String projectId() {
		return GeneralUtils.getSenderId();
	}

	@Override
	public String type() {
		return FCM_ANDROID_TYPE;
	}
}
