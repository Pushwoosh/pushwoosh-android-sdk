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

package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushBundleDataProviderTest {

    @Mock
    private AppInfoProvider appInfoProvider;

    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void getActions() throws Exception {
        Bundle bundle = new Bundle();
        Collection<Action> actions = PushBundleDataProvider.getActions(bundle);
        Assert.assertTrue(actions.isEmpty());

        bundle.putString(
                "pw_actions",
                "[{title:\"action1\", type:\"ACTIVITY\"},{title:\"action2\",type:\"ACTIVITY\"},{title:\"action3\",type:\"ACTIVITY\"}]");
        actions = PushBundleDataProvider.getActions(bundle);
        Assert.assertEquals(3, actions.size());
    }

    @Test
    public void isSilent() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("silent", "true");
        boolean result = PushBundleDataProvider.isSilent(bundle);
        Assert.assertTrue(result);

        Bundle bundle2 = new Bundle();
        bundle2.putString("pw_silent", "false");
        boolean result2 = PushBundleDataProvider.isSilent(bundle2);
        Assert.assertFalse(result2);
    }

    // Restored from cross-check: getStringBoolean accepts both "true" literal AND positive integer ("1").
    // Server payloads historically use "1"/"0" for silent flag. A refactor narrowing to Boolean.parseBoolean
    // would silently break on-wire compat.
    @Test
    public void isSilent_returnsTrueForPositiveIntegerStringAndFalseForZeroOrNegative() {
        Bundle silentOne = new Bundle();
        silentOne.putString("silent", "1");
        assertTrue("silent=\"1\" must be true", PushBundleDataProvider.isSilent(silentOne));

        Bundle silentZero = new Bundle();
        silentZero.putString("silent", "0");
        assertFalse("silent=\"0\" must be false", PushBundleDataProvider.isSilent(silentZero));

        Bundle silentNegative = new Bundle();
        silentNegative.putString("silent", "-5");
        assertFalse("silent=\"-5\" must be false", PushBundleDataProvider.isSilent(silentNegative));

        Bundle pwSilentTwo = new Bundle();
        pwSilentTwo.putString("pw_silent", "2");
        assertTrue("pw_silent=\"2\" must be true", PushBundleDataProvider.isSilent(pwSilentTwo));
    }

    @Test
    public void getPriority() throws Exception {
        Bundle bundle = new Bundle();
        int result = PushBundleDataProvider.getPriority(bundle);
        Assert.assertEquals(0, result);

        bundle.putString("pri", "2");
        int result2 = PushBundleDataProvider.getPriority(bundle);
        Assert.assertEquals(2, result2);

        bundle = new Bundle();
        bundle.putString("pri", "5");
        int result3 = PushBundleDataProvider.getPriority(bundle);
        Assert.assertEquals(0, result3);

        bundle = new Bundle();
        bundle.putString("pri", "ewrw");
        int result4 = PushBundleDataProvider.getPriority(bundle);
        Assert.assertEquals(0, result4);
    }

    // --- getVibration ---

    @Test
    public void getVibration_returnsTrue_whenValueIs1() {
        Bundle bundle = new Bundle();
        bundle.putString("vib", "1");

        assertTrue(PushBundleDataProvider.getVibration(bundle));
    }

    @Test
    public void getVibration_returnsFalse_whenValueIsNot1() {
        List<String> nonOneValues = Arrays.asList("true", "0", "2", "");
        for (String value : nonOneValues) {
            Bundle bundle = new Bundle();
            bundle.putString("vib", value);

            assertFalse("vib=\"" + value + "\"", PushBundleDataProvider.getVibration(bundle));
        }
    }

    // --- getVisibility ---

    @Test
    public void getVisibility_returnsValue_whenInRange() {
        List<int[]> samples = Arrays.asList(new int[] {1, 1}, new int[] {0, 0}, new int[] {-1, -1});
        for (int[] sample : samples) {
            Bundle bundle = new Bundle();
            bundle.putString("visibility", String.valueOf(sample[0]));

            assertEquals("visibility=\"" + sample[0] + "\"", sample[1], PushBundleDataProvider.getVisibility(bundle));
        }
    }

    @Test
    public void getVisibility_returnsDefault_whenOutOfRange() {
        List<String> outOfRange = Arrays.asList("3", "-3", "7");
        for (String value : outOfRange) {
            Bundle bundle = new Bundle();
            bundle.putString("visibility", value);

            assertEquals("visibility=\"" + value + "\"", 1, PushBundleDataProvider.getVisibility(bundle));
        }
    }

    // --- isBadgesAdditive ---

    @Test
    public void isBadgesAdditive_returnsTrue_whenValueStartsWithPlus() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_badges", "+3");

        assertTrue(PushBundleDataProvider.isBadgesAdditive(bundle));
    }

    @Test
    public void isBadgesAdditive_returnsTrue_whenValueStartsWithMinus() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_badges", "-2");

        assertTrue(PushBundleDataProvider.isBadgesAdditive(bundle));
    }

    @Test
    public void isBadgesAdditive_returnsFalse_whenValueIsAbsoluteNumber() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_badges", "5");

        assertFalse(PushBundleDataProvider.isBadgesAdditive(bundle));
    }

    // --- getActions error path ---

    @Test
    public void getActions_returnsEmpty_whenJsonIsMalformed() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_actions", "not-a-json");

        assertTrue(PushBundleDataProvider.getActions(bundle).isEmpty());
    }

    // --- getHeader ---

    @Test
    public void getHeader_returnsBundleValue_whenHeaderPresent() {
        Bundle bundle = new Bundle();
        bundle.putString("header", "MyTitle");

        assertEquals("MyTitle", PushBundleDataProvider.getHeader(bundle));
    }

    @Test
    public void getHeader_returnsApplicationLabel_whenKeyMissing() {
        when(appInfoProvider.getApplicationLabel()).thenReturn("AppName");

        try (MockedStatic<AndroidPlatformModule> mocked = mockStatic(AndroidPlatformModule.class)) {
            mocked.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);

            assertEquals("AppName", PushBundleDataProvider.getHeader(new Bundle()));
        }
    }

    @Test
    public void getHeader_returnsEmpty_whenKeyMissingAndApplicationLabelNull() {
        when(appInfoProvider.getApplicationLabel()).thenReturn(null);

        try (MockedStatic<AndroidPlatformModule> mocked = mockStatic(AndroidPlatformModule.class)) {
            mocked.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);

            assertEquals("", PushBundleDataProvider.getHeader(new Bundle()));
        }
    }

    // --- isUserPush ---

    @Test
    public void isUserPush_returnsTrue_whenPwMsgIs1() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_msg", "1");

        assertTrue(PushBundleDataProvider.isUserPush(bundle));
    }
}
