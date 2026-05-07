package com.pushwoosh.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class RegistrationPrefsTest {

    private RegistrationPrefs registrationPrefs;
    private DeviceRegistrar deviceRegistrar;

    @Before
    public void setUp() {
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);
        deviceRegistrar = mock(DeviceRegistrar.class);
    }

    @After
    public void tearDown() {
        if (registrationPrefs != null) {
            RepositoryTestManager.destroyRegistrationPrefs(registrationPrefs);
        }
    }

    private RegistrationPrefs createWithAppId(String appId) {
        Config config = MockConfig.createMock(appId);
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);
        return registrationPrefs;
    }

    private RegistrationPrefs createWithAppIdAndRequestUrl(String appId, String requestUrl) {
        Config config = MockConfig.createMock(appId);
        when(config.getRequestUrl()).thenReturn(requestUrl);
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);
        return registrationPrefs;
    }

    // Constructor must not persist baseUrl when manifest appId is empty.
    @Test
    public void constructor_emptyManifestAppId_doesNotPersistBaseUrl() {
        RegistrationPrefs prefs = createWithAppId("");

        assertThat(prefs.baseUrl().get(), is(equalTo("")));
        assertThat(prefs.applicationId().get(), is(equalTo("")));
    }

    // Constructor must not persist baseUrl when manifest appId is null.
    @Test
    public void constructor_nullManifestAppId_doesNotPersistBaseUrl() {
        RegistrationPrefs prefs = createWithAppId(null);

        assertThat(prefs.baseUrl().get(), is(equalTo("")));
        assertThat(prefs.applicationId().get(), is(equalTo("")));
    }

    // Constructor must not write baseUrl even when appId is valid; baseUrl is set via setAppId().
    @Test
    public void constructor_validAppId_doesNotPersistBaseUrl() {
        RegistrationPrefs prefs = createWithAppId("APP_ID_VALID");

        assertThat(prefs.applicationId().get(), is(equalTo("APP_ID_VALID")));
        assertThat(prefs.baseUrl().get(), is(equalTo("")));
    }

    // setAppId must persist canonical baseUrl built from appId.
    @Test
    public void setAppId_validAppId_persistsBaseUrl() {
        RegistrationPrefs prefs = createWithAppId(null);

        prefs.setAppId("XXXXX");

        assertThat(prefs.applicationId().get(), is(equalTo("XXXXX")));
        assertThat(prefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAppId_emptyAppId_throwsIllegalArgumentException() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.setAppId("");
    }

    // setAppId(same value) preserves a custom baseUrl that was set previously.
    @Test
    public void setAppId_preservesCustomBaseUrl_whenAppIdUnchanged() {
        RegistrationPrefs prefs = createWithAppId("XXXXX");
        prefs.setAppId("XXXXX");
        prefs.baseUrl().set("https://custom.example.com/");

        prefs.setAppId("XXXXX");

        assertThat(prefs.baseUrl().get(), is(equalTo("https://custom.example.com/")));
    }

    // setAppId on real change resets baseUrl to canonical default.
    @Test
    public void setAppId_resetsBaseUrl_whenAppIdChanges() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.setAppId("AAAAA");
        prefs.baseUrl().set("https://custom.example.com/");

        prefs.setAppId("BBBBB");

        assertThat(prefs.baseUrl().get(), is(equalTo("https://BBBBB.api.pushwoosh.com/json/1.3/")));
    }

    @Test
    public void getDefaultBaseUrl_buildsUrlFromAppId() {
        RegistrationPrefs prefs = createWithAppId(null);
        String url = prefs.getDefaultBaseUrl("XXXXX");
        assertThat(url, is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    // config.getRequestUrl() takes precedence over default builder; trailing slash added.
    @Test
    public void getDefaultBaseUrl_customRequestUrl_takesPrecedence() {
        RegistrationPrefs prefs = createWithAppIdAndRequestUrl("XXXXX", "https://custom.example.com");
        String url = prefs.getDefaultBaseUrl("XXXXX");
        assertThat(url, is(equalTo("https://custom.example.com/")));
    }

    // updateBaseUrl is the single entry point: normalize + persist + return normalized.
    @Test
    public void updateBaseUrl_validUrl_persistsAndReturnsNormalized() {
        RegistrationPrefs prefs = createWithAppId(null);

        String result = prefs.updateBaseUrl("https://server.example.com/json/1.3/");

        assertThat(result, is(equalTo("https://server.example.com/json/1.3/")));
        assertThat(prefs.baseUrl().get(), is(equalTo("https://server.example.com/json/1.3/")));
    }

    @Test
    public void updateBaseUrl_urlWithoutTrailingSlash_addsSlash() {
        RegistrationPrefs prefs = createWithAppId(null);

        String result = prefs.updateBaseUrl("https://server.example.com/json/1.3");

        assertThat(result, is(equalTo("https://server.example.com/json/1.3/")));
        assertThat(prefs.baseUrl().get(), is(equalTo("https://server.example.com/json/1.3/")));
    }

    @Test
    public void updateBaseUrl_urlWithWhitespace_trimsAndPersists() {
        RegistrationPrefs prefs = createWithAppId(null);

        String result = prefs.updateBaseUrl("  https://server.example.com/  ");

        assertThat(result, is(equalTo("https://server.example.com/")));
        assertThat(prefs.baseUrl().get(), is(equalTo("https://server.example.com/")));
    }

    @Test
    public void updateBaseUrl_emptyValue_returnsNullAndKeepsCurrent() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.setAppId("XXXXX");

        String result = prefs.updateBaseUrl("");

        assertNull(result);
        assertThat(prefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    @Test
    public void updateBaseUrl_nullValue_returnsNullAndKeepsCurrent() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.setAppId("XXXXX");

        String result = prefs.updateBaseUrl(null);

        assertNull(result);
        assertThat(prefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    @Test
    public void updateBaseUrl_whitespaceOnly_returnsNullAndKeepsCurrent() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.setAppId("XXXXX");

        String result = prefs.updateBaseUrl("   ");

        assertNull(result);
        assertThat(prefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    @Test
    public void updateBaseUrl_malformedUrl_returnsNullAndKeepsCurrent() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.setAppId("XXXXX");

        String result = prefs.updateBaseUrl("not-a-url");

        assertNull(result);
        assertThat(prefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    @Test
    public void updateBaseUrl_nonHttpScheme_returnsNullAndKeepsCurrent() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.setAppId("XXXXX");

        String result = prefs.updateBaseUrl("file:///data/data/com.example/");

        assertNull(result);
        assertThat(prefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    @Test
    public void updateBaseUrl_ftpScheme_returnsNullAndKeepsCurrent() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.setAppId("XXXXX");

        String result = prefs.updateBaseUrl("ftp://server.example.com/");

        assertNull(result);
        assertThat(prefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    @Test
    public void updateBaseUrl_whitespaceInsideUrl_returnsNullAndKeepsCurrent() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.setAppId("XXXXX");

        String result = prefs.updateBaseUrl("https://server example.com/");

        assertNull(result);
        assertThat(prefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    @Test
    public void updateBaseUrl_httpScheme_isAccepted() {
        RegistrationPrefs prefs = createWithAppId(null);

        String result = prefs.updateBaseUrl("http://server.example.com/");

        assertThat(result, is(equalTo("http://server.example.com/")));
        assertThat(prefs.baseUrl().get(), is(equalTo("http://server.example.com/")));
    }

    @Test
    public void updateBaseUrl_sameAsCurrent_isDedupedAndReturnsNormalized() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.updateBaseUrl("https://server.example.com/");

        String result = prefs.updateBaseUrl("https://server.example.com/");

        assertThat(result, is(equalTo("https://server.example.com/")));
        assertThat(prefs.baseUrl().get(), is(equalTo("https://server.example.com/")));
    }
}
