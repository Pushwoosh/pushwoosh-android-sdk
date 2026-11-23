package com.pushwoosh.inapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class RegisterEmailRequestTest {

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager(MockConfig.createMock());
        platformTestManager.onApplicationCreated();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void buildParams_localeCollectionAllowed_sendsDeviceLanguage() throws Exception {
        RepositoryModule.getNotificationPreferences()
                .isCollectingDeviceLocaleAllowed()
                .set(true);
        JSONObject params = new JSONObject();

        new RegisterEmailRequest("user@example.com").buildParams(params);

        assertEquals(Locale.getDefault().getDisplayLanguage(), params.getString("language"));
    }

    @Test
    public void buildParams_localeCollectionDisabled_omitsLanguage() throws Exception {
        RepositoryModule.getNotificationPreferences()
                .isCollectingDeviceLocaleAllowed()
                .set(false);
        JSONObject params = new JSONObject();

        new RegisterEmailRequest("user@example.com").buildParams(params);

        assertFalse(params.has("language"));
        assertEquals("user@example.com", params.getString("email"));
    }
}
