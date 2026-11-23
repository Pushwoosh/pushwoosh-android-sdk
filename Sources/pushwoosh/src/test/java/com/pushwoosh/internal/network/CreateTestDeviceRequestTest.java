package com.pushwoosh.internal.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.pushwoosh.internal.utils.Config;
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

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class CreateTestDeviceRequestTest {

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        Config configMock = MockConfig.createMock();
        platformTestManager = new PlatformTestManager(configMock);
        platformTestManager.onApplicationCreated();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void buildParams_usesLanguageFromRegistrationPrefs() throws Exception {
        RepositoryModule.getNotificationPreferences()
                .isCollectingDeviceLocaleAllowed()
                .set(true);
        RepositoryModule.getRegistrationPreferences().setLanguage("es");
        CreateTestDeviceRequest request = new CreateTestDeviceRequest("name", "desc", false);
        JSONObject params = new JSONObject();

        request.buildParams(params);

        assertEquals("es", params.getString("language"));
    }

    @Test
    public void buildParams_localeCollectionDisabled_omitsLanguageEvenWithManualValue() throws Exception {
        RepositoryModule.getNotificationPreferences()
                .isCollectingDeviceLocaleAllowed()
                .set(false);
        RepositoryModule.getRegistrationPreferences().setLanguage("es");
        CreateTestDeviceRequest request = new CreateTestDeviceRequest("name", "desc", false);
        JSONObject params = new JSONObject();

        request.buildParams(params);

        assertFalse(params.has("language"));
    }
}
