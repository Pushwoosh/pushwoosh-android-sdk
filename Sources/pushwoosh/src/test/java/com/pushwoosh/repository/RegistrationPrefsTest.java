package com.pushwoosh.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.AndroidPlatformModuleTest;
import com.pushwoosh.internal.platform.prefs.migration.DefaultPrefsMigration;
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.internal.prefs.TestPrefsProvider;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

import java.util.Collections;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class RegistrationPrefsTest {

    private static final String PREFERENCE = "com.pushwoosh.registration";
    private static final String PROPERTY_PUSH_TOKEN = "registration_id";
    private static final String PROPERTY_APP_VERSION = "app_version";
    private static final String PROPERTY_USER_ID = "user_id";
    private static final String PROPERTY_IS_REGISTERED_FOR_NOTIFICATION = "pw_registered_for_push";

    private RegistrationPrefs registrationPrefs;
    private DeviceRegistrar deviceRegistrar;
    private Locale originalLocale;

    @Before
    public void setUp() {
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);
        deviceRegistrar = mock(DeviceRegistrar.class);
        originalLocale = Locale.getDefault();
    }

    @After
    public void tearDown() {
        if (registrationPrefs != null) {
            RepositoryTestManager.destroyRegistrationPrefs(registrationPrefs);
        }
        Locale.setDefault(originalLocale);
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

    // Parameterized rejection branches: all return null and keep current baseUrl.
    @Test
    public void updateBaseUrl_invalidInput_rejectsAndKeepsCurrent() {
        String[] invalidInputs = new String[] {
            "", null, "   ", "not-a-url", "file:///data/data/com.example/", "https://server example.com/"
        };

        for (String invalidInput : invalidInputs) {
            RegistrationPrefs prefs = createWithAppId(null);
            prefs.setAppId("XXXXX");

            String result = prefs.updateBaseUrl(invalidInput);

            assertNull("input=\"" + invalidInput + "\" must return null", result);
            assertThat(
                    "input=\"" + invalidInput + "\" must keep current baseUrl",
                    prefs.baseUrl().get(),
                    is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));

            RepositoryTestManager.destroyRegistrationPrefs(prefs);
            registrationPrefs = null;
        }
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

    // Constructor seeds apiToken from config when prefs are pristine.
    @Test
    public void constructor_pristinePrefs_seedsApiTokenFromConfig() {
        Config config = MockConfig.createMock("APP");
        when(config.getApiToken()).thenReturn("token-123");
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);

        assertThat(registrationPrefs.apiToken().get(), is(equalTo("token-123")));
    }

    // Constructor seeds logLevel from config when prefs are pristine.
    @Test
    public void constructor_pristinePrefs_seedsLogLevelFromConfig() {
        Config config = MockConfig.createMock("APP");
        when(config.getLogLevel()).thenReturn("DEBUG");
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);

        assertThat(registrationPrefs.logLevel().get(), is(equalTo("DEBUG")));
    }

    // Constructor seeds language from device locale when locale collection is allowed.
    @Test
    public void constructor_localeCollectionAllowed_seedsLanguageFromLocale() {
        Locale.setDefault(Locale.FRENCH);
        Config config = MockConfig.createMock("APP");
        when(config.isCollectingDeviceLocaleAllowed()).thenReturn(true);
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);

        assertThat(registrationPrefs.language().get(), is(equalTo("fr")));
    }

    // Constructor falls back to "en" when locale collection is disabled.
    @Test
    public void constructor_localeCollectionDisabled_fallsBackToEnglish() {
        Locale.setDefault(Locale.FRENCH);
        Config config = MockConfig.createMock("APP");
        when(config.isCollectingDeviceLocaleAllowed()).thenReturn(false);
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);

        assertThat(registrationPrefs.language().get(), is(equalTo("en")));
    }

    // Chinese locale uses full language tag (workaround for simplified/traditional split).
    @Test
    public void constructor_chineseLocale_usesLanguageTag() {
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        Config config = MockConfig.createMock("APP");
        when(config.isCollectingDeviceLocaleAllowed()).thenReturn(true);
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);

        assertThat(registrationPrefs.language().get(), is(equalTo(Locale.SIMPLIFIED_CHINESE.toLanguageTag())));
    }

    // Constructor with existing non-empty pushToken seeds isRegisteredForPush=true.
    @Test
    public void constructor_existingPushToken_defaultsRegisteredForPushTrue() {
        SharedPreferences prefs = installFreshTestPrefsProvider();
        prefs.edit().putString(PROPERTY_PUSH_TOKEN, "tkn").apply();

        Config config = MockConfig.createMock("APP");
        registrationPrefs = new RegistrationPrefs(config, deviceRegistrar);

        assertTrue(registrationPrefs.isRegisteredForPush().get());
    }

    // Constructor with empty pushToken defaults isRegisteredForPush=false.
    @Test
    public void constructor_emptyPushToken_defaultsRegisteredForPushFalse() {
        Config config = MockConfig.createMock("APP");
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);

        assertFalse(registrationPrefs.isRegisteredForPush().get());
    }

    // Constructor resets pushToken when stored app_version differs from current app version.
    @Test
    public void constructor_appVersionDiverges_resetsPushToken() {
        SharedPreferences prefs = installFreshTestPrefsProvider();
        prefs.edit().putString(PROPERTY_PUSH_TOKEN, "old-token").apply();
        // Plant a clearly-bogus version code so it cannot accidentally match getAppVersion().
        new PreferenceIntValue(prefs, PROPERTY_APP_VERSION, 0).set(987654);

        Config config = MockConfig.createMock("APP");
        registrationPrefs = new RegistrationPrefs(config, deviceRegistrar);

        assertThat(registrationPrefs.pushToken().get(), is(equalTo("")));
        assertEquals(
                "app_version must be rewritten to current after divergent reset",
                GeneralUtils.getAppVersion(),
                prefs.getInt(PROPERTY_APP_VERSION, -1));
    }

    // setLanguage with non-null value persists language, resets lastPushRegistration, calls updateRegistration.
    @Test
    public void setLanguage_nonNull_persistsResetsAndTriggersRegistrar() {
        RegistrationPrefs prefs = createWithAppId("APP");
        prefs.lastPushRegistration().set(12345L);

        prefs.setLanguage("de");

        assertThat(prefs.language().get(), is(equalTo("de")));
        assertEquals(0L, (long) prefs.lastPushRegistration().get());
        verify(deviceRegistrar).updateRegistration();
    }

    // setLanguage(null) is a no-op.
    @Test
    public void setLanguage_null_isNoOp() {
        RegistrationPrefs prefs = createWithAppId("APP");
        prefs.language().set("fr");

        prefs.setLanguage(null);

        assertThat(prefs.language().get(), is(equalTo("fr")));
        verify(deviceRegistrar, never()).updateRegistration();
    }

    // setApiToken with non-null value overwrites stored apiToken.
    @Test
    public void setApiToken_nonNull_overwritesStoredValue() {
        Config config = MockConfig.createMock("APP");
        when(config.getApiToken()).thenReturn("initial");
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);

        registrationPrefs.setApiToken("updated");

        assertThat(registrationPrefs.apiToken().get(), is(equalTo("updated")));
    }

    // setApiToken(null) is a no-op.
    @Test
    public void setApiToken_null_preservesExistingValue() {
        Config config = MockConfig.createMock("APP");
        when(config.getApiToken()).thenReturn("initial");
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);

        registrationPrefs.setApiToken(null);

        assertThat(registrationPrefs.apiToken().get(), is(equalTo("initial")));
    }

    // getTrackingUrl returns normalized url from config (trailing slash added) when valid.
    @Test
    public void getTrackingUrl_validConfigUrl_normalizesAndReturns() {
        Config config = MockConfig.createMock("APP");
        when(config.getTrackingUrl()).thenReturn("https://custom-tracking.example.com");
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);

        assertThat(registrationPrefs.getTrackingUrl(), is(equalTo("https://custom-tracking.example.com/")));
    }

    // Parameterized fallback: both null and malformed config urls fall back to the default constant.
    @Test
    public void getTrackingUrl_invalidConfigUrl_fallsBackToDefault() {
        String[] invalidInputs = new String[] {null, "not-a-url"};

        for (String invalidInput : invalidInputs) {
            Config config = MockConfig.createMock("APP");
            when(config.getTrackingUrl()).thenReturn(invalidInput);
            registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);

            assertThat(
                    "config.getTrackingUrl()=\"" + invalidInput + "\" must fall back to default",
                    registrationPrefs.getTrackingUrl(),
                    is(equalTo("https://tracking.svc-nue.pushwoosh.com/api/v2/device-api/")));

            RepositoryTestManager.destroyRegistrationPrefs(registrationPrefs);
            registrationPrefs = null;
        }
    }

    // removeAppId clears appId, baseUrl, lastPushRegistration and registeredOnServer.
    @Test
    public void removeAppId_clearsAppIdBaseUrlLastRegistrationAndRegisteredOnServer() {
        RegistrationPrefs prefs = createWithAppId(null);
        prefs.setAppId("AAAAA");
        prefs.registeredOnServer().set(true);
        prefs.lastPushRegistration().set(99L);

        prefs.removeAppId();

        assertThat(prefs.applicationId().get(), is(equalTo("")));
        assertThat(prefs.baseUrl().get(), is(equalTo("")));
        assertEquals(0L, (long) prefs.lastPushRegistration().get());
        assertFalse(prefs.registeredOnServer().get());
    }

    // clearPushRegistrationInfo resets pushToken and lastPushRegistration only.
    @Test
    public void clearPushRegistrationInfo_resetsPushTokenAndLastRegistrationOnly() {
        RegistrationPrefs prefs = createWithAppId("APP");
        prefs.pushToken().set("tkn");
        prefs.lastPushRegistration().set(42L);
        prefs.registeredOnServer().set(true);

        prefs.clearPushRegistrationInfo();

        assertThat(prefs.pushToken().get(), is(equalTo("")));
        assertEquals(0L, (long) prefs.lastPushRegistration().get());
        assertTrue(prefs.registeredOnServer().get());
    }

    // provideMigrationScheme carries the existing IS_REGISTERED_FOR_NOTIFICATION flag verbatim.
    @Test
    public void provideMigrationScheme_existingFlag_carriesValue() {
        TestPrefsProvider sourceProvider = new TestPrefsProvider();
        SharedPreferences raw = sourceProvider.providePrefs(PREFERENCE);
        raw.edit().putBoolean(PROPERTY_IS_REGISTERED_FOR_NOTIFICATION, true).apply();

        MigrationScheme scheme = RegistrationPrefs.provideMigrationScheme(sourceProvider);

        TestPrefsProvider destinationProvider = new TestPrefsProvider();
        new DefaultPrefsMigration(destinationProvider).migrate(Collections.singletonList(scheme));

        SharedPreferences migrated = destinationProvider.providePrefs(PREFERENCE);
        assertTrue(migrated.contains(PROPERTY_IS_REGISTERED_FOR_NOTIFICATION));
        assertTrue(migrated.getBoolean(PROPERTY_IS_REGISTERED_FOR_NOTIFICATION, false));
    }

    // provideMigrationScheme infers IS_REGISTERED_FOR_NOTIFICATION=true from non-empty pushToken.
    @Test
    public void provideMigrationScheme_pushTokenPresent_infersFlagTrue() {
        TestPrefsProvider sourceProvider = new TestPrefsProvider();
        SharedPreferences raw = sourceProvider.providePrefs(PREFERENCE);
        raw.edit().putString(PROPERTY_PUSH_TOKEN, "tkn").apply();

        MigrationScheme scheme = RegistrationPrefs.provideMigrationScheme(sourceProvider);

        TestPrefsProvider destinationProvider = new TestPrefsProvider();
        new DefaultPrefsMigration(destinationProvider).migrate(Collections.singletonList(scheme));

        SharedPreferences migrated = destinationProvider.providePrefs(PREFERENCE);
        assertTrue(migrated.getBoolean(PROPERTY_IS_REGISTERED_FOR_NOTIFICATION, false));
    }

    // provideMigrationScheme infers IS_REGISTERED_FOR_NOTIFICATION=false when both inputs are absent.
    @Test
    public void provideMigrationScheme_pristine_infersFlagFalse() {
        TestPrefsProvider sourceProvider = new TestPrefsProvider();
        // touch prefs to materialize an empty backing store
        sourceProvider.providePrefs(PREFERENCE);

        MigrationScheme scheme = RegistrationPrefs.provideMigrationScheme(sourceProvider);

        TestPrefsProvider destinationProvider = new TestPrefsProvider();
        new DefaultPrefsMigration(destinationProvider).migrate(Collections.singletonList(scheme));

        SharedPreferences migrated = destinationProvider.providePrefs(PREFERENCE);
        assertTrue(migrated.contains(PROPERTY_IS_REGISTERED_FOR_NOTIFICATION));
        assertFalse(migrated.getBoolean(PROPERTY_IS_REGISTERED_FOR_NOTIFICATION, true));
    }

    /**
     * Installs a fresh {@link TestPrefsProvider} into {@link AndroidPlatformModule} and returns
     * the in-memory {@link SharedPreferences} backing for {@link #PREFERENCE}. Use to pre-seed
     * data BEFORE constructing {@code RegistrationPrefs} directly via {@code new ...}.
     * <p>NOTE: do NOT pair with {@link RepositoryTestManager#createRegistrationPrefs} — that
     * helper overwrites the prefs provider, discarding any pre-seeded data.
     */
    private SharedPreferences installFreshTestPrefsProvider() {
        TestPrefsProvider provider = new TestPrefsProvider();
        AndroidPlatformModuleTest.changePrefsProvider(provider);
        return provider.providePrefs(PREFERENCE);
    }
}
