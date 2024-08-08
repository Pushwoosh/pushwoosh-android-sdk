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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class AppVersionProviderTest {
    private PlatformTestManager platformTestManager;
    private AppVersionProvider appVersionProvider;

    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        appVersionProvider = new AppVersionProvider(AndroidPlatformModule.getPrefsProvider().providePrefs("PWAppVersion_tests"));
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    @Ignore
    public void getCurrentVersion() throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = getPackageInfo();

        Assert.assertEquals(0, appVersionProvider.getCurrentVersion());

        packageInfo.versionCode = 1;

        Assert.assertEquals(1, appVersionProvider.getCurrentVersion());
    }

    private PackageInfo getPackageInfo() throws PackageManager.NameNotFoundException {
        Context context = AndroidPlatformModule.getApplicationContext();
        return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
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
    @Ignore
    public void isFirstLaunchAfterUpdate() throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = getPackageInfo();
        Assert.assertFalse(appVersionProvider.getFirstLaunchAfterUpdateAndDropValue());
        WhiteboxHelper.setInternalState(appVersionProvider, "handleLaunch", false);
        packageInfo.versionCode = 1;

        Assert.assertTrue(appVersionProvider.getFirstLaunchAfterUpdateAndDropValue());
        Assert.assertFalse(appVersionProvider.getFirstLaunchAfterUpdateAndDropValue());
    }


}