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

package com.pushwoosh.repository;

import com.pushwoosh.BaseTest;

import junit.runner.BaseTestRunner;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class AppOpenRequestTest extends BaseTest {


    public static final String EXPECTED_RESULT =
            "{\"device_name\":\"Phone\"," +
                    "\"device_model\":\"Unknown robolectric\"," +
                    "\"android_package\":\"com.pushwoosh\"," +
                    "\"timezone\":25200," +
                    "\"os_version\":\"4.1.2\"," +
                    "\"language\":\"en\",\"notificationTypes\":6}";

    @Test
    @Ignore("This test fails depending on system settings (timezone) which is unacceptable.")
    public void buildParams() throws JSONException {
        Locale.setDefault(Locale.forLanguageTag("en"));

        AppOpenRequest appOpenRequest = new AppOpenRequest();
        JSONObject jsonObject = new JSONObject();
        appOpenRequest.buildParams(jsonObject);

        Assert.assertEquals(EXPECTED_RESULT, jsonObject.toString());
    }
}