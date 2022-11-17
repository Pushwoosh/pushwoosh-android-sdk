/*
 *
 * Copyright (c) 2019. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.baidu.internal;

import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecific;

public class BaiduDeviceSpecific implements DeviceSpecific {
    private static final int BAIDU_DEVICE_TYPE = 16;
    private static final String BAIDU_PROJECT_ID = "BAIDU_DEVICE";
    private static final String BAIDU_TYPE = "Baidu";

    private BaiduPushRegistrar pushRegistrar;

    public BaiduDeviceSpecific(BaiduPushRegistrar pushRegistrar) {
        this.pushRegistrar = pushRegistrar;
    }

    @Override
    public PushRegistrar pushRegistrar() {
        return pushRegistrar;
    }

    @Override
    public String permission(String packageName) {
        return null;
    }

    @Override
    public int deviceType() {
        return BAIDU_DEVICE_TYPE;
    }

    @Override
    public String projectId() {
        return BAIDU_PROJECT_ID;
    }

    @Override
    public String type() {
        return BAIDU_TYPE;
    }
}
