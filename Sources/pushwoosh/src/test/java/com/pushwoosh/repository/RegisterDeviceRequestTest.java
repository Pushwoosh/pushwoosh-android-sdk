package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.PushPermissionStatusTracker;
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
public class RegisterDeviceRequestTest {

    private Locale originalDefaultLocale;

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        originalDefaultLocale = Locale.getDefault();
        Config configMock = MockConfig.createMock();
        platformTestManager = new PlatformTestManager(configMock);
        platformTestManager.onApplicationCreated();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
        Locale.setDefault(originalDefaultLocale);
    }

    private void rebuildPlatform(boolean collectingLocaleAllowed) {
        platformTestManager.tearDown();
        Config configMock = MockConfig.createMock();
        when(configMock.isCollectingDeviceLocaleAllowed()).thenReturn(collectingLocaleAllowed);
        platformTestManager = new PlatformTestManager(configMock);
        platformTestManager.onApplicationCreated();
    }

    private JSONObject buildParams(RegisterDeviceRequest request) throws JSONException {
        JSONObject params = new JSONObject();
        request.buildParams(params);
        return params;
    }

    @Test
    public void getMethod_returnsRegisterDeviceLiteral() {
        RegisterDeviceRequest request = new RegisterDeviceRequest("dev-1", null, 3);

        assertEquals("registerDevice", request.getMethod());
    }

    @Test
    public void buildParams_platformSms_setsHwidToRawDeviceId() throws Exception {
        RegisterDeviceRequest request = new RegisterDeviceRequest("+15551234567", null, DeviceRegistrar.PLATFORM_SMS);

        JSONObject params = buildParams(request);

        assertEquals("+15551234567", params.getString("hwid"));
        assertEquals("+15551234567", params.getString("push_token"));
        assertEquals(DeviceRegistrar.PLATFORM_SMS, params.getInt("device_type"));
    }

    @Test
    public void buildParams_platformWhatsapp_prefixesHwidAndKeepsPushTokenIntact() throws Exception {
        RegisterDeviceRequest request =
                new RegisterDeviceRequest("+15559876543", null, DeviceRegistrar.PLATFORM_WHATSAPP);

        JSONObject params = buildParams(request);

        assertEquals("whatsapp:+15559876543", params.getString("hwid"));
        assertEquals("+15559876543", params.getString("push_token"));
        assertEquals(DeviceRegistrar.PLATFORM_WHATSAPP, params.getInt("device_type"));
    }

    // Restored from cross-check: pins that non-SMS / non-WhatsApp platforms don't get their own `hwid` write.
    // A refactor that adds an unconditional `hwid` write would send hwid for FCM/HMS where it must not appear.
    @Test
    public void buildParams_regularPlatform_omitsHwidOverride() throws Exception {
        RegisterDeviceRequest request = new RegisterDeviceRequest("test-device-id", null, 3);

        JSONObject params = buildParams(request);

        assertFalse("regular platform (device_type=3) must not write hwid", params.has("hwid"));
    }

    // Restored from cross-check: pins String -> JSONObject parsing transformation.
    // Refactor risk: `params.put("tags", tagsObject)` becomes `params.put("tags", tagsJson)` and backend
    // silently breaks (expects nested object, gets raw string).
    @Test
    public void buildParams_nonEmptyTagsJson_isParsedIntoNestedJsonObject() throws Exception {
        RegisterDeviceRequest request = new RegisterDeviceRequest("dev-1", "{\"Language\":\"en\",\"Age\":25}", 3);

        JSONObject params = buildParams(request);

        JSONObject tags = params.getJSONObject("tags");
        assertEquals("en", tags.getString("Language"));
        assertEquals(25, tags.getInt("Age"));
    }

    // Restored from cross-check: pins the TextUtils.isEmpty guard semantics — both null and empty-string
    // tagsJson skip the tags write. A refactor narrowing to `tagsJson != null` would feed "" into
    // `new JSONObject("")` and throw JSONException at runtime.
    @Test
    public void buildParams_emptyOrNullTagsJson_omitsTagsField() throws Exception {
        String[] inputs = new String[] {null, ""};
        for (String tagsJson : inputs) {
            String label = tagsJson == null ? "null" : "empty";
            RegisterDeviceRequest request = new RegisterDeviceRequest("dev-1", tagsJson, 3);

            JSONObject params = buildParams(request);

            assertFalse("tags must be omitted for tagsJson=" + label, params.has("tags"));
        }
    }

    @Test
    public void buildParams_sendsPushPermissionStatus() throws Exception {
        RegisterDeviceRequest request = new RegisterDeviceRequest("dev-1", null, 3);

        JSONObject params = buildParams(request);

        assertEquals(PushPermissionStatusTracker.getStatus(), params.getString("push_permission_status"));
    }

    @Test
    public void buildParams_smsRegistrationCarriesNoPushPermissionStatus() throws Exception {
        RegisterDeviceRequest request = new RegisterDeviceRequest("+15551234567", null, DeviceRegistrar.PLATFORM_SMS);

        JSONObject params = buildParams(request);

        assertFalse(params.has("push_permission_status"));
    }

    @Test
    public void getSentPushPermissionStatus_smsAndWhatsappCarryNone() throws Exception {
        int[] platforms = new int[] {DeviceRegistrar.PLATFORM_SMS, DeviceRegistrar.PLATFORM_WHATSAPP};
        for (int platform : platforms) {
            RegisterDeviceRequest request = new RegisterDeviceRequest("+15551234567", null, platform);
            buildParams(request);

            assertNull("platform " + platform, request.getSentPushPermissionStatus());
        }
    }

    @Test
    public void buildParams_noManualLocale_sendsFullDeviceLocaleTag() throws Exception {
        Locale.setDefault(new Locale.Builder().setLanguage("pt").setRegion("BR").build());
        rebuildPlatform(true);

        JSONObject params = buildParams(new RegisterDeviceRequest("dev-1", null, 3));

        assertEquals("pt-BR", params.getString("locale"));
    }

    // The locale field must not disturb the language field.
    @Test
    public void buildParams_manualLocale_sendsItAndLeavesLanguageUntouched() throws Exception {
        Locale.setDefault(new Locale.Builder().setLanguage("pt").setRegion("BR").build());
        rebuildPlatform(true);
        RepositoryModule.getRegistrationPreferences().setLocale("es-419");

        JSONObject params = buildParams(new RegisterDeviceRequest("dev-1", null, 3));

        assertEquals("es-419", params.getString("locale"));
        assertEquals("pt", params.getString("language"));
    }

    @Test
    public void buildParams_localeCollectionDisabled_omitsLocaleAndLanguageEvenWithManualValues() throws Exception {
        rebuildPlatform(false);
        RepositoryModule.getRegistrationPreferences().setLocale("es-419");
        RepositoryModule.getRegistrationPreferences().setLanguage("de");

        JSONObject params = buildParams(new RegisterDeviceRequest("dev-1", null, 3));

        assertFalse("locale must be omitted when locale collection is disabled", params.has("locale"));
        assertFalse("language must be omitted when locale collection is disabled", params.has("language"));
    }

    @Test
    public void buildParams_whatsappPlatform_carriesLocale() throws Exception {
        Locale.setDefault(new Locale.Builder().setLanguage("pt").setRegion("BR").build());
        rebuildPlatform(true);

        JSONObject params =
                buildParams(new RegisterDeviceRequest("+15559876543", null, DeviceRegistrar.PLATFORM_WHATSAPP));

        assertEquals("pt-BR", params.getString("locale"));
    }
}
