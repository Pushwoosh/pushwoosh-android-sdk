package com.pushwoosh;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class AndroidManifestConfigTest {
    private AndroidManifestConfig androidManifestConfig;
    private PlatformTestManager platformTestManager;


    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager();
    }

    private void sendPushStatIfShowForegroundDisabled(boolean sendPushStatIfShowForegroundDisabled) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("com.pushwoosh.send_push_stats_if_alert_disabled", sendPushStatIfShowForegroundDisabled);
        ApplicationInfo applicationInfo = AndroidPlatformModule.getAppInfoProvider().getApplicationInfo();
        applicationInfo.metaData = bundle;
        androidManifestConfig = new AndroidManifestConfig();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void sendPushStatIfShowForegroundDisabledFlagTrue(){
        sendPushStatIfShowForegroundDisabled(true);
        boolean result = androidManifestConfig.getSendPushStatIfShowForegroundDisabled();
        Assert.assertEquals(true, result);
    }

    @Test
    public void sendPushStatIfShowForegroundDisabledFlagFalse(){
        sendPushStatIfShowForegroundDisabled(false);
        boolean result = androidManifestConfig.getSendPushStatIfShowForegroundDisabled();
        Assert.assertEquals(false, result);
    }
}