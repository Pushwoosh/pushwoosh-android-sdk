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

package com.pushwoosh.notification.channel;

import android.os.Bundle;

import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class NotificationChannelInfoProviderTest {
    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void channelId() {
        String result = NotificationChannelInfoProvider.channelId("pushwoosh_channelName");
        Assert.assertEquals("pushwoosh_channelName", result);

        String result2 = NotificationChannelInfoProvider.channelId("chagnnel name 2");
        Assert.assertEquals("pushwoosh_chagnnel_name_2", result2);
    }

    @Test
    public void getChannelName() {
        Bundle bundle = new Bundle();
        PushMessage pushMessage = Mockito.mock(PushMessage.class);
        Mockito.when(pushMessage.toBundle()).thenReturn(bundle);

        platformTestManager.getNotificationPrefs().channelName().set("old name channel");
        String result = NotificationChannelInfoProvider.getChannelName(pushMessage);
        Assert.assertEquals("old name channel", result);

        bundle.putString("pw_channel", "channel name");
        String result2 = NotificationChannelInfoProvider.getChannelName(pushMessage);
        Assert.assertEquals("channel name", result2);


    }
}