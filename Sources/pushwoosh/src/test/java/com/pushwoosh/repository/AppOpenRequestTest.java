package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import android.os.Bundle;

import com.pushwoosh.ManifestConfigFixture;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONException;
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
public class AppOpenRequestTest {

    private Locale originalDefaultLocale;

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        originalDefaultLocale = Locale.getDefault();
        Locale.setDefault(new Locale.Builder().setLanguage("pt").setRegion("BR").build());
    }

    @After
    public void tearDown() {
        platformTestManager.tearDown();
        Locale.setDefault(originalDefaultLocale);
    }

    private void startPlatform(Config config) {
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.onApplicationCreated();
    }

    private void startPlatformWithLocaleFlag(boolean collectingLocaleAllowed) {
        Config configMock = MockConfig.createMock();
        when(configMock.isCollectingDeviceLocaleAllowed()).thenReturn(collectingLocaleAllowed);
        startPlatform(configMock);
    }

    private void startPlatformWithManifest(boolean collectingDeviceDataAllowed) {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.appid", MockConfig.APP_ID);
        metaData.putBoolean("com.pushwoosh.allow_collecting_device_data", collectingDeviceDataAllowed);
        metaData.putBoolean("com.pushwoosh.allow_collecting_device_locale", true);
        startPlatform(ManifestConfigFixture.fromMetaData(metaData));
    }

    private JSONObject buildParams(AppOpenRequest request) throws JSONException {
        JSONObject params = new JSONObject();
        request.buildParams(params);
        return params;
    }

    @Test
    public void buildParams_noManualLocale_sendsFullDeviceLocaleTag() throws Exception {
        startPlatformWithLocaleFlag(true);

        JSONObject params = buildParams(new AppOpenRequest());

        assertEquals("pt-BR", params.getString("locale"));
        assertEquals("pt", params.getString("language"));
    }

    @Test
    public void buildParams_manualLocale_sendsItAndLeavesLanguageUntouched() throws Exception {
        startPlatformWithLocaleFlag(true);
        RepositoryModule.getRegistrationPreferences().setLocale("es-419");

        JSONObject params = buildParams(new AppOpenRequest());

        assertEquals("es-419", params.getString("locale"));
        assertEquals("pt", params.getString("language"));
    }

    @Test
    public void buildParams_localeCollectionDisabled_omitsLocaleAndLanguageEvenWithManualValues() throws Exception {
        startPlatformWithLocaleFlag(false);
        RepositoryModule.getRegistrationPreferences().setLocale("es-419");
        RepositoryModule.getRegistrationPreferences().setLanguage("de");

        JSONObject params = buildParams(new AppOpenRequest());

        assertFalse("locale must be omitted when locale collection is disabled", params.has("locale"));
        assertFalse("language must be omitted when locale collection is disabled", params.has("language"));
    }

    @Test
    public void buildParams_manifestAllowsDeviceData_bothRequestsCarryLocale() throws Exception {
        startPlatformWithManifest(true);

        assertEquals("pt-BR", buildParams(new AppOpenRequest()).getString("locale"));
        assertEquals(
                "pt-BR",
                buildParams(new RegisterDeviceRequest("dev-1", null, 3)).getString("locale"));
    }

    @Test
    public void buildParams_manifestForbidsDeviceData_bothRequestsOmitLocaleAndLanguage() throws Exception {
        startPlatformWithManifest(false);
        RepositoryModule.getRegistrationPreferences().setLocale("es-419");

        assertFalse(
                "applicationOpen must not carry locale when device data collection is disabled",
                buildParams(new AppOpenRequest()).has("locale"));
        assertFalse(
                "registerDevice must not carry locale when device data collection is disabled",
                buildParams(new RegisterDeviceRequest("dev-1", null, 3)).has("locale"));
        assertFalse(
                "applicationOpen must not carry language when device data collection is disabled",
                buildParams(new AppOpenRequest()).has("language"));
    }
}
