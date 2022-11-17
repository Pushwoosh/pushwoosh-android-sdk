package com.pushwoosh.repository;

import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NotificationPrefsTest {
    private NotificationPrefs notificationPrefs;

    private PlatformTestManager testManager;

    @Before
    public void setUp() throws Exception {
        testManager = new PlatformTestManager();
        testManager.setUp();
        notificationPrefs = new NotificationPrefs(MockConfig.createMock());
    }

    @After
    public void tearDown() throws Exception {
        testManager.tearDown();
    }

    @Test
    public void shouldReturnBooleanStorage() {
        PreferenceBooleanValue preferenceBooleanValue = notificationPrefs.tagsMigrationDone();
        Assert.assertNotNull(preferenceBooleanValue);
    }


}