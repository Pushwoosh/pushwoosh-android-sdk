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

package com.pushwoosh.firebase.internal.mapper;

import android.os.Bundle;

import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.RemoteMessage;
import com.pushwoosh.BuildConfig;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by aevstefeev on 05/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(constants = BuildConfig.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@PrepareForTest({FirebaseApp.class, FirebaseInstanceId.class})
public class RemoteMessageMapperTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void mapToBundle() throws Exception {
        PowerMockito.mockStatic(FirebaseApp.class);
        PowerMockito.mockStatic(FirebaseInstanceId.class);
        FirebaseApp fireBaseIntance = PowerMockito.mock(FirebaseApp.class);
        FirebaseInstanceId firebaseInstanceId = PowerMockito.mock(FirebaseInstanceId.class);
        PowerMockito.when(firebaseInstanceId.getToken()).thenReturn("token123");
        PowerMockito.when(FirebaseApp.getInstance()).thenReturn(fireBaseIntance);
        PowerMockito.when(FirebaseInstanceId.getInstance()).thenReturn(firebaseInstanceId);

        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("key1", "value1");
        valueMap.put("key2", "value2");
        valueMap.put("key3", "value3");

        RemoteMessage remoteMessage = new RemoteMessage.Builder("123")
                .clearData()
                .setData(valueMap)
                .build();

        Bundle bundle = RemoteMessageMapper.mapToBundle(remoteMessage);

        Assert.assertEquals("value1", bundle.getString("key1"));
        Assert.assertEquals("value2", bundle.getString("key2"));
        Assert.assertEquals("value3", bundle.getString("key3"));
    }

}