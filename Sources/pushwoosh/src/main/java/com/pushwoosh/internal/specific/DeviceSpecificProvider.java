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

package com.pushwoosh.internal.specific;

import com.pushwoosh.internal.registrar.PushRegistrar;

/**
 * This class provides specific properties depends on device (fcm, gcm or amazon)
 */
public class DeviceSpecificProvider {
	public static final String FCM_ANDROID_TYPE = "Android FCM";
	public static final String HUAWEI_TYPE = "Huawei";
	public static final String XIAOMI_TYPE = "Xiaomi";
	private static DeviceSpecificProvider instance;
	private final DeviceSpecific deviceSpecific;

	private DeviceSpecificProvider(final DeviceSpecific deviceSpecific) {
		this.deviceSpecific = deviceSpecific;
	}

	public PushRegistrar pushRegistrar() {
		return deviceSpecific.pushRegistrar();
	}

	public String permission(final String packageName) {
		return deviceSpecific.permission(packageName);
	}

	public int deviceType() {
		return deviceSpecific.deviceType();
	}

	public String projectId() {
		return deviceSpecific.projectId();
	}

	public String type() {
		return deviceSpecific.type();
	}

	public boolean isFirebase() {
		return type().equals(FCM_ANDROID_TYPE);
	}

	public boolean isHuawei() {
		return type().equals(HUAWEI_TYPE);
	}

	public boolean isXiaomi() { return type().equals(XIAOMI_TYPE); }

	public static class Builder {
		private DeviceSpecific deviceSpecific;

		public Builder setDeviceSpecific(final DeviceSpecific deviceSpecific) {
			this.deviceSpecific = deviceSpecific;
			return this;
		}

		public DeviceSpecificProvider build(boolean forceReplace) {
			if (deviceSpecific == null) {
				throw new IllegalArgumentException("You must setup deviceSpecific");
			}

			if (instance == null || forceReplace) {
				instance = new DeviceSpecificProvider(deviceSpecific);
			}

			return instance;
		}
	}

	public static DeviceSpecificProvider getInstance() {
		return instance;
	}

	public static boolean isInited() {
		return instance != null;
	}
}
