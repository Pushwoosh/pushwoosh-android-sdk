package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.os.Bundle;

import com.pushwoosh.ManifestConfigFixture;
import com.pushwoosh.internal.network.PushRequestHelper;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class AppOpenRequestTest {

    private static final List<String> APP_OPEN_KEYS = Arrays.asList(
            "android_package",
            "application",
            "device_type",
            "hwid",
            "language",
            "locale",
            "notificationTypes",
            "push_permission_status",
            "timezone",
            "v");

    private Locale originalDefaultLocale;

    private TimeZone originalDefaultTimeZone;

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        originalDefaultLocale = Locale.getDefault();
        originalDefaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(new Locale.Builder().setLanguage("pt").setRegion("BR").build());
    }

    @After
    public void tearDown() {
        platformTestManager.tearDown();
        Locale.setDefault(originalDefaultLocale);
        TimeZone.setDefault(originalDefaultTimeZone);
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

    @Test
    public void buildParams_noManualLocale_sendsFullDeviceLocaleTag() throws Exception {
        startPlatformWithLocaleFlag(true);

        JSONObject params = PushRequestHelper.getParams(new AppOpenRequest());

        assertEquals("pt-BR", params.getString("locale"));
        assertEquals("pt", params.getString("language"));
    }

    @Test
    public void buildParams_manualLocale_sendsItAndLeavesLanguageUntouched() throws Exception {
        startPlatformWithLocaleFlag(true);
        RepositoryModule.getRegistrationPreferences().setLocale("es-419");

        JSONObject params = PushRequestHelper.getParams(new AppOpenRequest());

        assertEquals("es-419", params.getString("locale"));
        assertEquals("pt", params.getString("language"));
    }

    @Test
    public void buildParams_localeCollectionDisabled_omitsLocaleAndLanguageEvenWithManualValues() throws Exception {
        startPlatformWithLocaleFlag(false);
        RepositoryModule.getRegistrationPreferences().setLocale("es-419");
        RepositoryModule.getRegistrationPreferences().setLanguage("de");

        JSONObject params = PushRequestHelper.getParams(new AppOpenRequest());

        assertFalse("locale must be omitted when locale collection is disabled", params.has("locale"));
        assertFalse("language must be omitted when locale collection is disabled", params.has("language"));
    }

    @Test
    public void buildParams_manifestAllowsDeviceData_bothRequestsCarryLocale() throws Exception {
        startPlatformWithManifest(true);

        assertEquals("pt-BR", PushRequestHelper.getParams(new AppOpenRequest()).getString("locale"));
        assertEquals(
                "pt-BR",
                PushRequestHelper.getParams(new RegisterDeviceRequest("dev-1", null, 3))
                        .getString("locale"));
    }

    @Test
    public void buildParams_manifestForbidsDeviceData_bothRequestsOmitLocaleAndLanguage() throws Exception {
        startPlatformWithManifest(false);
        RepositoryModule.getRegistrationPreferences().setLocale("es-419");

        assertFalse(
                "applicationOpen must not carry locale when device data collection is disabled",
                PushRequestHelper.getParams(new AppOpenRequest()).has("locale"));
        assertFalse(
                "registerDevice must not carry locale when device data collection is disabled",
                PushRequestHelper.getParams(new RegisterDeviceRequest("dev-1", null, 3))
                        .has("locale"));
        assertFalse(
                "applicationOpen must not carry language when device data collection is disabled",
                PushRequestHelper.getParams(new AppOpenRequest()).has("language"));
    }

    // Pins the full parameter set of applicationOpen, the base class of registerDevice.
    @Test
    public void getParams_applicationOpen_keepsWireContract() throws Exception {
        startPlatformWithLocaleFlag(true);
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        prefs.applicationId().set("APP-1");
        prefs.hwid().set("HW-1");
        prefs.setLanguage("es");
        prefs.setLocale("es-MX");

        JSONObject params = PushRequestHelper.getParams(new AppOpenRequest());

        assertEquals(APP_OPEN_KEYS, PushRequestHelper.sortedKeys(params));
        assertEquals("APP-1", params.getString("application"));
        assertEquals("HW-1", params.getString("hwid"));
        assertEquals(0, params.getInt("device_type"));
        assertEquals("es", params.getString("language"));
        assertEquals("es-MX", params.getString("locale"));
    }

    private Object timezoneSentBy(AppOpenRequest request, String zoneId) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
        return PushRequestHelper.getParams(request).get("timezone");
    }

    private void assertZoneNameSentByBoth(String zoneId) throws Exception {
        Object open = timezoneSentBy(new AppOpenRequest(), zoneId);
        Object register = timezoneSentBy(new RegisterDeviceRequest("dev-1", null, 3), zoneId);
        assertEquals(zoneId, open);
        assertEquals(zoneId, register);
    }

    private void assertOffsetSecondsSentByBoth(String zoneId, long expectedSeconds) throws Exception {
        Object open = timezoneSentBy(new AppOpenRequest(), zoneId);
        Object register = timezoneSentBy(new RegisterDeviceRequest("dev-1", null, 3), zoneId);
        assertTrue("applicationOpen must send a number for " + zoneId, open instanceof Number);
        assertTrue("registerDevice must send a number for " + zoneId, register instanceof Number);
        assertEquals(expectedSeconds, ((Number) open).longValue());
        assertEquals(expectedSeconds, ((Number) register).longValue());
    }

    @Test
    public void buildParams_regionZone_sendsZoneName() throws Exception {
        startPlatformWithLocaleFlag(true);

        assertZoneNameSentByBoth("Europe/Paris");
    }

    @Test
    public void buildParams_deprecatedAlias_sendsItUnchanged() throws Exception {
        startPlatformWithLocaleFlag(true);

        assertZoneNameSentByBoth("Asia/Calcutta");
    }

    @Test
    public void buildParams_customGmtOffsetZone_sendsOffsetSeconds() throws Exception {
        startPlatformWithLocaleFlag(true);

        assertOffsetSecondsSentByBoth("GMT+03:00", 10800L);
    }

    @Test
    public void buildParams_negativeHalfHourCustomZone_sendsNegativeOffsetSeconds() throws Exception {
        startPlatformWithLocaleFlag(true);

        assertOffsetSecondsSentByBoth("GMT-05:30", -19800L);
    }

    @Test
    public void buildParams_etcGmtAndBareGmtAndUtc_sendZoneNames() throws Exception {
        startPlatformWithLocaleFlag(true);

        assertZoneNameSentByBoth("Etc/GMT-3");
        assertZoneNameSentByBoth("GMT");
        assertZoneNameSentByBoth("UTC");
    }

    @Test
    public void buildParams_defaultZoneChangedBetweenRequests_sendsNewZone() throws Exception {
        startPlatformWithLocaleFlag(true);

        assertEquals("Europe/Paris", timezoneSentBy(new AppOpenRequest(), "Europe/Paris"));
        assertEquals("Asia/Tokyo", timezoneSentBy(new AppOpenRequest(), "Asia/Tokyo"));
    }
}
