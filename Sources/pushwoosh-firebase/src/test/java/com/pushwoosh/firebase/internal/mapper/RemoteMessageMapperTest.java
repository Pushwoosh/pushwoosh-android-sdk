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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by aevstefeev on 05/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class RemoteMessageMapperTest {

    @Mock
    public Task<String> tokenStringTask;

    @Test
    public void mapToBundle() throws Exception {
        try (
                MockedStatic<FirebaseApp> firebaseAppMockedStatic = Mockito.mockStatic(FirebaseApp.class);
                MockedStatic<FirebaseMessaging> firebaseMessagingMockedStatic = Mockito.mockStatic(FirebaseMessaging.class);
                MockedStatic<Tasks> tasksMockedStatic = Mockito.mockStatic(Tasks.class)
                ){
            FirebaseMessaging instanceMock = Mockito.mock(FirebaseMessaging.class);
            firebaseMessagingMockedStatic.when(FirebaseMessaging::getInstance).thenReturn(instanceMock);
            Mockito.when(instanceMock.getToken()).thenReturn(tokenStringTask);
            tasksMockedStatic.when(()->Tasks.await(tokenStringTask)).thenReturn("token123");

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

}