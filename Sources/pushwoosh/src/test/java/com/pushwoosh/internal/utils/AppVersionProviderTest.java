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

package com.pushwoosh.internal.utils;

import android.content.SharedPreferences;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class AppVersionProviderTest {
    private static final String PREFS_NAME = "PWAppVersion_tests";
    private static final String KEY_LAST_LAUNCH_VERSION = "LastLaunchVersion";

    private PlatformTestManager platformTestManager;
    private AppVersionProvider appVersionProvider;
    private SharedPreferences prefs;

    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        prefs = AndroidPlatformModule.getPrefsProvider().providePrefs(PREFS_NAME);
        prefs.edit().clear().commit();
        appVersionProvider = new AppVersionProvider(prefs);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void isFirstLaunchAndDropValue() {
        Assert.assertTrue(appVersionProvider.getFirstLaunchAndDropValue());
        Assert.assertFalse(appVersionProvider.getFirstLaunchAndDropValue());
    }

    @Test
    public void isFirstLaunch() {
        Assert.assertTrue(appVersionProvider.isFirstLaunch());
        Assert.assertTrue(appVersionProvider.isFirstLaunch());
    }

    @Test
    public void getFirstLaunchAfterUpdateAndDropValueIsFalseOnFirstLaunch() {
        Assert.assertFalse(appVersionProvider.getFirstLaunchAfterUpdateAndDropValue());
        Assert.assertFalse(appVersionProvider.getFirstLaunchAfterUpdateAndDropValue());
    }

    @Test
    public void getFirstLaunchAfterUpdateAndDropValueIsTrueWhenVersionChanged() {
        prefs.edit().putInt(KEY_LAST_LAUNCH_VERSION, 99).commit();
        AppVersionProvider provider = new AppVersionProvider(prefs);

        Assert.assertTrue(provider.getFirstLaunchAfterUpdateAndDropValue());
        Assert.assertFalse(provider.getFirstLaunchAfterUpdateAndDropValue());

        SharedPreferences siblingPrefs =
                AndroidPlatformModule.getPrefsProvider().providePrefs(PREFS_NAME);
        siblingPrefs.edit().putInt(KEY_LAST_LAUNCH_VERSION, 99).commit();
        AppVersionProvider sibling = new AppVersionProvider(siblingPrefs);
        Assert.assertFalse(sibling.getFirstLaunchAndDropValue());
    }

    @Test
    public void bothFlagsAreFalseWhenLaunchedOnSameVersion() {
        int currentVersion = appVersionProvider.getCurrentVersion();
        prefs.edit().putInt(KEY_LAST_LAUNCH_VERSION, currentVersion).commit();
        AppVersionProvider provider = new AppVersionProvider(prefs);

        Assert.assertFalse(provider.getFirstLaunchAndDropValue());
        Assert.assertFalse(provider.getFirstLaunchAfterUpdateAndDropValue());
    }

    @Test
    public void handleLaunchPersistsCurrentVersionToPrefs() {
        Assert.assertFalse(prefs.contains(KEY_LAST_LAUNCH_VERSION));

        appVersionProvider.isFirstLaunch();

        Assert.assertTrue(prefs.contains(KEY_LAST_LAUNCH_VERSION));
        Assert.assertEquals(appVersionProvider.getCurrentVersion(), prefs.getInt(KEY_LAST_LAUNCH_VERSION, -1));
    }

    @Test
    public void handleLaunchIsIdempotent() {
        appVersionProvider.handleLaunch();

        // mutate prefs after first handleLaunch — a second handleLaunch() must not
        // re-read prefs nor recompute flags (handleLaunch guard returns early).
        prefs.edit().putInt(KEY_LAST_LAUNCH_VERSION, 12345).commit();
        appVersionProvider.handleLaunch();

        Assert.assertTrue(appVersionProvider.getFirstLaunchAndDropValue());
        Assert.assertFalse(appVersionProvider.getFirstLaunchAndDropValue());
        Assert.assertFalse(appVersionProvider.getFirstLaunchAfterUpdateAndDropValue());
    }
}
