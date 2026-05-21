package com.pushwoosh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import com.pushwoosh.internal.NativePluginProvider;
import com.pushwoosh.internal.PluginProvider;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
import com.pushwoosh.richmedia.RichMediaType;
import com.pushwoosh.test.manifest.FakeNotificationService;
import com.pushwoosh.test.manifest.FakePlugin;
import com.pushwoosh.test.manifest.FakePluginProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class AndroidManifestConfigTest {

    @Mock
    private AppInfoProvider appInfoProvider;

    @Mock
    private ResourceProvider resourceProvider;

    private AutoCloseable mocks;
    private MockedStatic<AndroidPlatformModule> platformMock;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        platformMock = mockStatic(AndroidPlatformModule.class);
        platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
        platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);
        when(appInfoProvider.getPackageName()).thenReturn("com.pushwoosh.test");
    }

    @After
    public void tearDown() throws Exception {
        platformMock.close();
        mocks.close();
    }

    private void prepareConfigInputs(Bundle metaData) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.metaData = metaData;
        when(appInfoProvider.getApplicationInfo()).thenReturn(applicationInfo);
    }

    private AndroidManifestConfig configWithMetaData(Bundle metaData) {
        prepareConfigInputs(metaData);
        return new AndroidManifestConfig();
    }

    @Test
    public void appIdWithTrailingNewlineIsSanitized() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.appid", "XXXXX-XXXXX\n");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals("XXXXX-XXXXX", config.getAppId());
    }

    @Test
    public void emptyAppIdReturnsNull() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.appid", "");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertNull(config.getAppId());
    }

    @Test
    public void appIdWithDotIsRejected() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.appid", "invalid.appid");
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.metaData = metaData;
        when(appInfoProvider.getApplicationInfo()).thenReturn(applicationInfo);

        assertThrows(IllegalStateException.class, AndroidManifestConfig::new);
    }

    // Verifies that legacy meta-data key is used as fallback when the canonical key is absent.
    @Test
    public void deprecatedAppIdKeyIsUsedAsFallback() {
        Bundle metaData = new Bundle();
        metaData.putString("PW_APPID", "LEGACY-APPID");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals("LEGACY-APPID", config.getAppId());
    }

    // Verifies that non-string meta-data values are coerced via String.valueOf fallback.
    @Test
    public void nonStringLogLevelIsCoercedToString() {
        Bundle metaData = new Bundle();
        metaData.putInt("com.pushwoosh.log_level", 42);

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals("42", config.getLogLevel());
    }

    // Verifies that a class name starting with '.' is resolved relative to the application package.
    @Test
    public void notificationServiceClassWithDotPrefixIsResolvedAgainstPackage() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.notification_service_extension", ".manifest.FakeNotificationService");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals(FakeNotificationService.class, config.getNotificationService());
    }

    // Verifies that referencing a non-existent class throws IllegalStateException.
    @Test
    public void unknownNotificationServiceClassThrows() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.notification_service_extension", "com.pushwoosh.test.manifest.NotExisting");
        prepareConfigInputs(metaData);

        IllegalStateException ex = assertThrows(IllegalStateException.class, AndroidManifestConfig::new);
        assertTrue(ex.getMessage().contains("com.pushwoosh.test.manifest.NotExisting"));
    }

    // Verifies that a class without a public no-arg constructor is rejected with IllegalStateException.
    @Test
    public void classWithoutPublicDefaultConstructorThrows() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.notification_service_extension", "com.pushwoosh.test.manifest.NoDefaultCtorClass");
        prepareConfigInputs(metaData);

        IllegalStateException ex = assertThrows(IllegalStateException.class, AndroidManifestConfig::new);
        assertTrue(ex.getMessage().toLowerCase().contains("default constructor"));
    }

    // Verifies the documented defaults of every boolean flag and timeout when no meta-data is supplied.
    @Test
    public void booleanFlagsHaveDocumentedDefaults() {
        Bundle metaData = new Bundle();

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertFalse("lazySdkInitialization", config.isLazySdkInitialization());
        assertFalse("multinotificationMode", config.isMultinotificationMode());
        assertFalse("lightscreenNotification", config.isLightscreenNotification());
        assertFalse("sendPushStatIfShowForegroundDisabled", config.getSendPushStatIfShowForegroundDisabled());
        assertTrue("isServerCommunicationAllowed", config.isServerCommunicationAllowed());
        assertFalse("showPushNotificationAlert", config.showPushNotificationAlert());
        assertFalse("handleNotificationsUsingWorkManager", config.handleNotificationsUsingWorkManager());
        assertTrue("shouldShowFullscreenRichMedia", config.shouldShowFullscreenRichMedia());
        assertFalse("isReverseProxyAllowed", config.isReverseProxyAllowed());
        assertTrue("isCollectingDeviceOsVersionAllowed", config.isCollectingDeviceOsVersionAllowed());
        assertTrue("isCollectingDeviceLocaleAllowed", config.isCollectingDeviceLocaleAllowed());
        assertTrue("isCollectingDeviceModelAllowed", config.isCollectingDeviceModelAllowed());
        assertTrue("isCollectingLifecycleEventsAllowed", config.isCollectingLifecycleEventsAllowed());
        assertEquals("idleTimeoutSeconds", 0, config.getIdleTimeoutSeconds());
        assertEquals("exitIntentTimeoutSeconds", 0, config.getExitIntentTimeoutSeconds());
        assertEquals("richMediaType", RichMediaType.DEFAULT, config.getRichMediaType());
    }

    // Verifies that allow_collecting_device_data=false overrides every collecting flag and zeroes timeouts.
    @Test
    public void allowCollectingDeviceDataFalseCascadesOffEverything() {
        Bundle metaData = new Bundle();
        metaData.putBoolean("com.pushwoosh.allow_collecting_device_data", false);
        metaData.putBoolean("com.pushwoosh.allow_collecting_device_os_version", true);
        metaData.putBoolean("com.pushwoosh.allow_collecting_device_locale", true);
        metaData.putBoolean("com.pushwoosh.allow_collecting_device_model", true);
        metaData.putBoolean("com.pushwoosh.allow_collecting_events", true);
        metaData.putInt("com.pushwoosh.idle_timeout_seconds", 30);
        metaData.putInt("com.pushwoosh.exit_intent_timeout_seconds", 60);

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertFalse(config.isCollectingDeviceOsVersionAllowed());
        assertFalse(config.isCollectingDeviceLocaleAllowed());
        assertFalse(config.isCollectingDeviceModelAllowed());
        assertFalse(config.isCollectingLifecycleEventsAllowed());
        assertEquals(0, config.getIdleTimeoutSeconds());
        assertEquals(0, config.getExitIntentTimeoutSeconds());
    }

    // Verifies that disabling lifecycle events alone zeroes timeouts but keeps the other collecting flags.
    @Test
    public void allowCollectingEventsFalseZeroesTimeoutsOnly() {
        Bundle metaData = new Bundle();
        metaData.putBoolean("com.pushwoosh.allow_collecting_events", false);
        metaData.putInt("com.pushwoosh.idle_timeout_seconds", 30);
        metaData.putInt("com.pushwoosh.exit_intent_timeout_seconds", 60);

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertFalse(config.isCollectingLifecycleEventsAllowed());
        assertEquals(0, config.getIdleTimeoutSeconds());
        assertEquals(0, config.getExitIntentTimeoutSeconds());
        assertTrue(config.isCollectingDeviceOsVersionAllowed());
        assertTrue(config.isCollectingDeviceLocaleAllowed());
        assertTrue(config.isCollectingDeviceModelAllowed());
    }

    // Verifies that timeouts are preserved when both data and events collection are allowed.
    @Test
    public void timeoutsAreKeptWhenCollectingEnabled() {
        Bundle metaData = new Bundle();
        metaData.putInt("com.pushwoosh.idle_timeout_seconds", 30);
        metaData.putInt("com.pushwoosh.exit_intent_timeout_seconds", 60);

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals(30, config.getIdleTimeoutSeconds());
        assertEquals(60, config.getExitIntentTimeoutSeconds());
    }

    // Verifies that trusted_package_names is split on commas and each entry is trimmed.
    @Test
    public void trustedPackageNamesAreSplitAndTrimmed() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.trusted_package_names", " com.foo.app , com.bar.app , com.baz.app");

        AndroidManifestConfig config = configWithMetaData(metaData);

        String[] names = config.getTrustedPackageNames();
        assertEquals(3, names.length);
        assertEquals("com.foo.app", names[0]);
        assertEquals("com.bar.app", names[1]);
        assertEquals("com.baz.app", names[2]);
    }

    // Verifies that an empty trusted_package_names string yields an empty array.
    @Test
    public void emptyTrustedPackageNamesYieldsEmptyArray() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.trusted_package_names", "");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals(0, config.getTrustedPackageNames().length);
    }

    // Verifies that meta-data plugin entries are instantiated and exposed via getPlugins().
    @Test
    public void pluginsAreLoadedFromMetaData() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.plugin.fake", "com.pushwoosh.test.manifest.FakePlugin");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals(1, config.getPlugins().size());
        assertTrue(config.getPlugins().iterator().next() instanceof FakePlugin);
    }

    // Verifies that a plugin whose constructor throws does not break SDK config and is just skipped.
    @Test
    public void throwingPluginIsSkippedWithoutFailingConstructor() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.plugin.bad", "com.pushwoosh.test.manifest.ThrowingPlugin");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertTrue(config.getPlugins().isEmpty());
    }

    // Verifies that an explicit plugin_provider meta-data value is honoured.
    @Test
    public void customPluginProviderIsUsedWhenSpecified() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.internal.plugin_provider", "com.pushwoosh.test.manifest.FakePluginProvider");

        AndroidManifestConfig config = configWithMetaData(metaData);

        PluginProvider provider = config.getPluginProvider();
        assertTrue(provider instanceof FakePluginProvider);
    }

    // Verifies that NativePluginProvider is used as fallback when no plugin_provider key is set.
    @Test
    public void pluginProviderFallsBackToNativeWhenAbsent() {
        Bundle metaData = new Bundle();

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertTrue(config.getPluginProvider() instanceof NativePluginProvider);
    }

    // Verifies that a throwing plugin provider triggers fallback to NativePluginProvider without failing the
    // constructor.
    @Test
    public void throwingPluginProviderFallsBackToNative() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.internal.plugin_provider", "com.pushwoosh.test.manifest.ThrowingPluginProvider");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertTrue(config.getPluginProvider() instanceof NativePluginProvider);
    }

    // Verifies that a null ApplicationInfo short-circuits the constructor with safe defaults and no plugin provider.
    @Test
    public void nullApplicationInfoUsesDefaultsAndReturnsEarly() {
        when(appInfoProvider.getApplicationInfo()).thenReturn(null);

        AndroidManifestConfig config = new AndroidManifestConfig();

        assertNull(config.getAppId());
        assertNull(config.getApiToken());
        assertTrue(config.isServerCommunicationAllowed());
        assertEquals(0, config.getTrustedPackageNames().length);
        assertTrue(config.getPlugins().isEmpty());
        assertNull(config.getPluginProvider());
    }

    // Verifies that notification_icon path is parsed and resolved through ResourceProvider.getIdentifier.
    @Test
    public void notificationIconPathIsResolvedViaResourceProvider() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.notification_icon", " res/drawable-xxhdpi-v11/notification_small_icon.png ");
        when(resourceProvider.getIdentifier("notification_small_icon", "drawable"))
                .thenReturn(12345);

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals(12345, config.getNotificationIcon());
        verify(resourceProvider).getIdentifier("notification_small_icon", "drawable");
    }
}
