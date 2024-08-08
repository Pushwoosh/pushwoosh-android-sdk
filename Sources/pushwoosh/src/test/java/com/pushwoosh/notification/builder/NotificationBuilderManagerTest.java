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

package com.pushwoosh.notification.builder;

import android.app.PendingIntent;
import android.content.Context;
import androidx.annotation.NonNull;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.notification.Action;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.eq;

/**
 * Created by aevstefeev on 12/03/2018.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class NotificationBuilderManagerTest {

    @Test
    public void createNotificationBuilder() throws Exception {
        Context context = RuntimeEnvironment.application;

        NotificationBuilder notificationBuilder =
                NotificationBuilderManager.createNotificationBuilder(context, "id");

        Assert.assertTrue(notificationBuilder instanceof NotificationBuilderApi14);
    }

    @Test
    public void addAction() throws Exception {
        Context context = RuntimeEnvironment.application;
        AndroidPlatformModule.init(context, true);
        NotificationBuilder notificationBuilder = Mockito.mock(NotificationBuilder.class);

        Action action = getActionMock();

        JSONObject extras = new JSONObject();
        Mockito.when(action.getExtras()).thenReturn(extras);

        NotificationBuilderManager.addAction(context, notificationBuilder, action);

        Mockito.verify(notificationBuilder).addAction(eq(17301540), eq("title"), Mockito.any(PendingIntent.class));
    }

    @NonNull
    private Action getActionMock() {
        Action action = Mockito.mock(Action.class);
        Mockito.when(action.getIcon()).thenReturn("android.R.drawable.ic_media_play");
        Mockito.when(action.getType()).thenReturn(Action.Type.ACTIVITY);
        Mockito.when(action.getTitle()).thenReturn("title");
        Mockito.when(action.getUrl()).thenReturn("http://url");
        Mockito.when(action.getActionClass()).thenReturn(Object.class);
        return action;
    }

}