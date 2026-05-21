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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;

import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

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
        assertEquals("pushwoosh_channelName", result);

        String result2 = NotificationChannelInfoProvider.channelId("chagnnel name 2");
        assertEquals("pushwoosh_chagnnel_name_2", result2);
    }

    @Test
    public void getChannelName() {
        Bundle bundle = new Bundle();
        PushMessage pushMessage = Mockito.mock(PushMessage.class);
        Mockito.when(pushMessage.toBundle()).thenReturn(bundle);

        platformTestManager.getNotificationPrefs().channelName().set("old name channel");
        String result = NotificationChannelInfoProvider.getChannelName(pushMessage);
        assertEquals("old name channel", result);

        bundle.putString("pw_channel", "channel name");
        String result2 = NotificationChannelInfoProvider.getChannelName(pushMessage);
        assertEquals("channel name", result2);
    }

    // Verifies that channelId re-applies prefix when input already starts with prefix but contains whitespace.
    @Test
    public void channelId_prefixedNameWithSpace_reappliesPrefixAndNormalizes() {
        String result = NotificationChannelInfoProvider.channelId("pushwoosh_my channel");
        assertEquals("pushwoosh_pushwoosh_my_channel", result);
    }

    // Verifies that channelId table-driven normalization (uppercase, multi-whitespace, trim).
    @Test
    public void channelId_normalizationCases_returnExpectedIds() {
        List<String[]> cases = Arrays.asList(
                new String[] {"MyChannel", "pushwoosh_mychannel"},
                new String[] {"my   channel", "pushwoosh_my_channel"},
                new String[] {"  channel  ", "pushwoosh_channel"});

        for (String[] row : cases) {
            String input = row[0];
            String expected = row[1];
            assertEquals("input=" + input, expected, NotificationChannelInfoProvider.channelId(input));
        }
    }

    // Verifies that priority 0 maps to IMPORTANCE_DEFAULT on API >= N.
    @Test
    public void getChannelImportance_priorityZero_returnsImportanceDefault() {
        PushMessage pushMessage = Mockito.mock(PushMessage.class);
        Mockito.when(pushMessage.getPriority()).thenReturn(0);

        int result = NotificationChannelInfoProvider.getChannelImportance(pushMessage);

        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, result);
    }

    // Verifies that positive priorities 1 and 2 map to IMPORTANCE_HIGH on API >= N.
    @Test
    public void getChannelImportance_positivePriorities_returnImportanceHigh() {
        for (int priority : new int[] {1, 2}) {
            PushMessage pushMessage = Mockito.mock(PushMessage.class);
            Mockito.when(pushMessage.getPriority()).thenReturn(priority);

            int result = NotificationChannelInfoProvider.getChannelImportance(pushMessage);

            assertEquals("priority=" + priority, NotificationManager.IMPORTANCE_HIGH, result);
        }
    }

    // Verifies that negative priorities -1 and -2 map to IMPORTANCE_LOW on API >= N.
    @Test
    public void getChannelImportance_negativePriorities_returnImportanceLow() {
        for (int priority : new int[] {-1, -2}) {
            PushMessage pushMessage = Mockito.mock(PushMessage.class);
            Mockito.when(pushMessage.getPriority()).thenReturn(priority);

            int result = NotificationChannelInfoProvider.getChannelImportance(pushMessage);

            assertEquals("priority=" + priority, NotificationManager.IMPORTANCE_LOW, result);
        }
    }

    // Verifies that priorities outside [-2..2] fall through to IMPORTANCE_UNSPECIFIED.
    @Test
    public void getChannelImportance_outOfRangePriorities_returnImportanceUnspecified() {
        for (int priority : new int[] {5, -3}) {
            PushMessage pushMessage = Mockito.mock(PushMessage.class);
            Mockito.when(pushMessage.getPriority()).thenReturn(priority);

            int result = NotificationChannelInfoProvider.getChannelImportance(pushMessage);

            assertEquals("priority=" + priority, NotificationManager.IMPORTANCE_UNSPECIFIED, result);
        }
    }

    // Verifies that on pre-N (API < 24) the raw priority is returned instead of importance mapping.
    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void getChannelImportance_preN_returnsRawPriority() {
        PushMessage pushMessage = Mockito.mock(PushMessage.class);
        Mockito.when(pushMessage.getPriority()).thenReturn(2);

        int result = NotificationChannelInfoProvider.getChannelImportance(pushMessage);

        assertEquals(2, result);
        assertNotEquals(NotificationManager.IMPORTANCE_HIGH, result);
    }
}
