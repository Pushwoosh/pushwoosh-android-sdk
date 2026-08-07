package com.pushwoosh.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
public class AppCodeApplierTest {

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

    private AppCodeApplier createWithAppId(String appId) {
        Config config = MockConfig.createMock(appId);
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);
        return new AppCodeApplier(registrationPrefs);
    }

    private AppCodeApplier createWithAppIdAndRequestUrl(String appId, String requestUrl) {
        Config config = MockConfig.createMock(appId);
        when(config.getRequestUrl()).thenReturn(requestUrl);
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);
        return new AppCodeApplier(registrationPrefs);
    }

    // apply must persist canonical baseUrl built from the app code.
    @Test
    public void apply_validAppCode_persistsBaseUrl() {
        AppCodeApplier applier = createWithAppId(null);

        applier.apply("XXXXX", null);

        assertThat(registrationPrefs.applicationId().get(), is(equalTo("XXXXX")));
        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    @Test
    public void apply_emptyAppCode_rejectsAndWritesNothing() {
        AppCodeApplier applier = createWithAppId(null);

        AppCodeApplier.Result result = applier.apply("", null);

        assertTrue(result.isRejected());
        assertThat(registrationPrefs.applicationId().get(), is(equalTo("")));
        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("")));
    }

    // apply(same value) preserves a custom baseUrl that was set previously.
    @Test
    public void apply_preservesCustomBaseUrl_whenAppCodeUnchanged() {
        AppCodeApplier applier = createWithAppId("XXXXX");
        applier.apply("XXXXX", null);
        registrationPrefs.baseUrl().set("https://custom.example.com/");

        applier.apply("XXXXX", null);

        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("https://custom.example.com/")));
    }

    // apply on real change resets baseUrl to canonical default.
    @Test
    public void apply_resetsBaseUrl_whenAppCodeChanges() {
        AppCodeApplier applier = createWithAppId(null);
        applier.apply("AAAAA", null);
        registrationPrefs.baseUrl().set("https://custom.example.com/");

        applier.apply("BBBBB", null);

        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("https://BBBBB.api.pushwoosh.com/json/1.3/")));
    }

    // A half-cleared identity (appId empty, stale baseUrl left behind — e.g. process death between
    // removeAppId's four separate writes) must not survive: apply recomputes the default URL
    // whenever the stored code differs from the new one, even when the stored code is empty.
    @Test
    public void apply_emptyStoredCodeWithStaleBaseUrl_recomputesDefaultUrl() {
        AppCodeApplier applier = createWithAppId(null);
        registrationPrefs.baseUrl().set("https://OLDAPP.api.pushwoosh.com/json/1.3/");

        applier.apply("NEWAPP", null);

        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("https://NEWAPP.api.pushwoosh.com/json/1.3/")));
    }

    // The mirror half-state (code survived, baseUrl lost) must heal the same way: a same-code
    // apply with an empty stored URL recomputes the default instead of leaving the SDK pointing
    // nowhere.
    @Test
    public void apply_sameCodeWithEmptyStoredBaseUrl_recomputesDefaultUrl() {
        AppCodeApplier applier = createWithAppId(null);
        applier.apply("XXXXX", null);
        registrationPrefs.baseUrl().set("");

        applier.apply("XXXXX", null);

        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
    }

    // Explicit base URL is persisted verbatim and outranks the manifest requestUrl default.
    @Test
    public void apply_withCustomBaseUrl_persistsCustomUrlAsIs() {
        AppCodeApplier applier = createWithAppIdAndRequestUrl(null, "https://manifest.example.com/json/1.3/");

        applier.apply("XXXXX", "https://api.example.com/json/1.3/");

        assertThat(registrationPrefs.applicationId().get(), is(equalTo("XXXXX")));
        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("https://api.example.com/json/1.3/")));
    }

    // Same app code with a different explicit URL must overwrite the persisted URL (region switch).
    @Test
    public void apply_sameAppCodeWithCustomBaseUrl_overwritesCurrentUrl() {
        AppCodeApplier applier = createWithAppId(null);
        applier.apply("XXXXX", null);
        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));

        applier.apply("XXXXX", "https://api.example.com/json/1.3/");

        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("https://api.example.com/json/1.3/")));
    }

    // A rejected custom URL cancels the whole call: neither the app code nor the URL is applied, so
    // the device can never end up on a new app code pointing at the previous code's endpoint.
    @Test
    public void apply_rejectedCustomBaseUrl_appliesNothing() {
        AppCodeApplier applier = createWithAppId(null);
        applier.apply("AAAAA", null);

        AppCodeApplier.Result result = applier.apply("BBBBB", "not-a-url");

        assertTrue(result.isRejected());
        assertThat(registrationPrefs.applicationId().get(), is(equalTo("AAAAA")));
        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("https://AAAAA.api.pushwoosh.com/json/1.3/")));
    }

    // The applied verdict carries the persisted base URL so the caller retargets the request
    // manager without re-reading prefs — including the same-code path at process restart, where
    // this value is what seeds the request manager's baseRequestUrl.
    @Test
    public void apply_applied_resultCarriesPersistedBaseUrl() {
        AppCodeApplier applier = createWithAppId(null);

        AppCodeApplier.Result result = applier.apply("XXXXX", null);

        assertFalse(result.isRejected());
        assertThat(result.getBaseUrl(), is(equalTo("https://XXXXX.api.pushwoosh.com/json/1.3/")));
        assertThat(result.getBaseUrl(), is(equalTo(registrationPrefs.baseUrl().get())));
        assertNull(result.getPreviousRegistration());
    }

    // Same code with a previously persisted custom URL: the verdict must carry that stored URL,
    // not a recomputed default — the restart path relies on it to retarget correctly.
    @Test
    public void apply_sameCodeWithStoredCustomUrl_resultCarriesStoredUrl() {
        AppCodeApplier applier = createWithAppId(null);
        applier.apply("XXXXX", "https://api.example.com/json/1.3/");

        AppCodeApplier.Result result = applier.apply("XXXXX", null);

        assertThat(result.getBaseUrl(), is(equalTo("https://api.example.com/json/1.3/")));
        assertNull(result.getPreviousRegistration());
    }

    // The verdict reports the URL as normalized (trailing slash), not the raw input.
    @Test
    public void apply_customBaseUrlWithoutTrailingSlash_resultCarriesNormalizedUrl() {
        AppCodeApplier applier = createWithAppId(null);

        AppCodeApplier.Result result = applier.apply("XXXXX", "https://api.example.com/json/1.3");

        assertThat(result.getBaseUrl(), is(equalTo("https://api.example.com/json/1.3/")));
    }

    // Code change and explicit URL in one call: the snapshot captures the registration being left
    // (old code, old URL, token), while the new custom URL is persisted and reported.
    @Test
    public void apply_appCodeChangeWithCustomUrl_snapshotsPreviousAndAppliesCustomUrl() {
        AppCodeApplier applier = createWithAppId(null);
        applier.apply("AAAAA", null);
        registrationPrefs.pushToken().set("token-1");
        registrationPrefs.registeredOnServer().set(true);

        AppCodeApplier.Result result = applier.apply("BBBBB", "https://api.example.com/json/1.3/");

        AppCodeApplier.PreviousRegistration previous = result.getPreviousRegistration();
        assertNotNull(previous);
        assertThat(previous.appCode, is(equalTo("AAAAA")));
        assertThat(previous.pushToken, is(equalTo("token-1")));
        assertThat(previous.baseUrl, is(equalTo("https://AAAAA.api.pushwoosh.com/json/1.3/")));
        assertTrue(previous.wasRegisteredOnServer);
        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("https://api.example.com/json/1.3/")));
        assertThat(result.getBaseUrl(), is(equalTo("https://api.example.com/json/1.3/")));
    }

    // Trimming is facade policy (public API); the applier persists the code verbatim. A
    // whitespace code can't form a valid default URL, so the base URL stays untouched.
    @Test
    public void apply_whitespaceAppCode_persistsVerbatim() {
        AppCodeApplier applier = createWithAppId(null);

        AppCodeApplier.Result result = applier.apply("   ", null);

        assertFalse(result.isRejected());
        assertThat(registrationPrefs.applicationId().get(), is(equalTo("   ")));
        assertThat(registrationPrefs.baseUrl().get(), is(equalTo("")));
    }

    // The re-registration cycle on app code change clears lastPushRegistration and registeredOnServer
    // and returns the previous registration snapshot taken before the clearing.
    @Test
    public void apply_appCodeChange_clearsRegistrationStateAndSnapshotsPreviousRegistration() {
        AppCodeApplier applier = createWithAppId(null);
        applier.apply("AAAAA", null);
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.lastPushRegistration().set(99L);

        AppCodeApplier.Result result = applier.apply("BBBBB", null);

        AppCodeApplier.PreviousRegistration previous = result.getPreviousRegistration();
        assertNotNull(previous);
        assertThat(previous.appCode, is(equalTo("AAAAA")));
        assertThat(previous.baseUrl, is(equalTo("https://AAAAA.api.pushwoosh.com/json/1.3/")));
        assertTrue(previous.wasRegisteredOnServer);
        assertEquals(0L, (long) registrationPrefs.lastPushRegistration().get());
        assertFalse(registrationPrefs.registeredOnServer().get());
    }

    // forceRegister mirrors the user's subscription, it is not armed unconditionally: a device that
    // is not registered for push (unsubscribed, or a pending FCM unregister) must not be silently
    // re-registered by an application code change. The armed case is pinned in
    // PushwooshNotificationManagerTest#setAppId_newAppIdSameBaseUrl_runsFullChangeCycle.
    @Test
    public void apply_appCodeChangeWhileNotRegisteredForPush_disarmsForceRegister() {
        AppCodeApplier applier = createWithAppId(null);
        applier.apply("AAAAA", null);
        registrationPrefs.isRegisteredForPush().set(false);
        registrationPrefs.forceRegister().set(true);

        applier.apply("BBBBB", null);

        assertFalse(registrationPrefs.forceRegister().get());
    }
}
