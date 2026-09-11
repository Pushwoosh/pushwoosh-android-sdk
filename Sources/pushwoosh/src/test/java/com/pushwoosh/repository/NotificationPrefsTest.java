package com.pushwoosh.repository;

import static org.mockito.Mockito.when;

import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.richmedia.RichMediaColorScheme;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
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

    @Test
    public void richMediaColorSchemeDefaultsToConfigValue() {
        Config config = MockConfig.createMock();
        when(config.getRichMediaColorScheme()).thenReturn(RichMediaColorScheme.DARK);

        NotificationPrefs prefs = new NotificationPrefs(config);

        Assert.assertEquals("DARK", prefs.richMediaColorScheme().get());
    }

    @Test
    public void richMediaColorSchemeSetterBeatsConfigDefault() {
        notificationPrefs.richMediaColorScheme().set(RichMediaColorScheme.LIGHT.name());

        Config config = MockConfig.createMock();
        when(config.getRichMediaColorScheme()).thenReturn(RichMediaColorScheme.DARK);
        NotificationPrefs reloaded = new NotificationPrefs(config);

        Assert.assertEquals("LIGHT", reloaded.richMediaColorScheme().get());
    }
}
