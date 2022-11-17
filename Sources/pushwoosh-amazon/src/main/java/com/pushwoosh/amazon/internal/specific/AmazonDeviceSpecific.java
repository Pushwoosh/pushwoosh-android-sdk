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

package com.pushwoosh.amazon.internal.specific;

import com.pushwoosh.amazon.internal.registrar.AdmRegistrar;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecific;

final class AmazonDeviceSpecific implements DeviceSpecific {
	private static final int AMAZON_DEVICE_TYPE = 9;
	private static final String AMAZON_PERMISSION_SUFFIX = ".permission.RECEIVE_ADM_MESSAGE";
	private static final String AMAZON_PROJECT_ID = "AMAZON_DEVICE";
	private static final String AMAZON_TYPE = "Amazon";

	private final PushRegistrar pushRegistrar = new AdmRegistrar();

	@Override
	public PushRegistrar pushRegistrar() {
		return pushRegistrar;
	}

	@Override
	public String permission(final String packageName) {
		return packageName + AMAZON_PERMISSION_SUFFIX;
	}

	@Override
	public int deviceType() {
		return AMAZON_DEVICE_TYPE;
	}

	@Override
	public String projectId() {
		return AMAZON_PROJECT_ID;
	}

	@Override
	public String type() {
		return AMAZON_TYPE;
	}
}
