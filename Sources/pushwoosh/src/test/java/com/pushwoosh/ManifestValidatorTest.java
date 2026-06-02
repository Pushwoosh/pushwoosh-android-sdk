package com.pushwoosh;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class ManifestValidatorTest {

    @Mock
    private AppInfoProvider appInfoProvider;

    private AutoCloseable mocks;
    private MockedStatic<AndroidPlatformModule> platformMock;
    private MockedStatic<PWLog> pwLogMock;
    private MockedStatic<DeviceSpecificProvider> providerMock;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        platformMock = mockStatic(AndroidPlatformModule.class);
        platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
        when(appInfoProvider.getPackageName()).thenReturn("com.pushwoosh.test");
        pwLogMock = mockStatic(PWLog.class);
        providerMock = mockStatic(DeviceSpecificProvider.class);
        providerMock.when(DeviceSpecificProvider::getInstance).thenReturn(mock(DeviceSpecificProvider.class));
    }

    @After
    public void tearDown() throws Exception {
        ManifestValidator.resetForTesting();
        providerMock.close();
        pwLogMock.close();
        platformMock.close();
        mocks.close();
    }

    private void prepareMetaData(Bundle metaData) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.metaData = metaData;
        when(appInfoProvider.getApplicationInfo()).thenReturn(applicationInfo);
    }

    @Test
    public void scheduleValidationIsIdempotentAcrossCalls() {
        assertTrue(ManifestValidator.scheduleValidation(Runnable::run));
        assertFalse(ManifestValidator.scheduleValidation(Runnable::run));
        assertFalse(ManifestValidator.scheduleValidation(Runnable::run));
    }

    @Test
    public void notificationServiceExtensionKeyAbsentIsSilent() {
        prepareMetaData(new Bundle());

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void notificationServiceExtensionUnknownClassWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.notification_service_extension", "com.foo.NotExisting");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("com.foo.NotExisting") && s.contains("not found"))));
    }

    @Test
    public void notificationServiceExtensionWrongBaseWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.notification_service_extension", "com.pushwoosh.test.manifest.FakeNotificationService");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNotificationService") && s.contains("does not extend"))));
    }

    @Test
    public void notificationServiceExtensionNoCtorWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.notification_service_extension",
                "com.pushwoosh.test.manifest.FakeNoCtorNotificationServiceExtension");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNoCtorNotificationServiceExtension")
                        && s.contains("no public no-argument constructor"))));
    }

    @Test
    public void notificationServiceExtensionValidIsSilent() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.notification_service_extension",
                "com.pushwoosh.test.manifest.FakeValidNotificationServiceExtension");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    // Verifies that a leading-dot class name in the manifest is resolved against the application's package name.
    @Test
    public void notificationServiceExtensionLeadingDotResolvesAgainstPackageName() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.notification_service_extension", ".manifest.FakeValidNotificationServiceExtension");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    // Verifies that when a leading-dot value resolves to a missing class, the warning reports the resolved FQN — not
    // the original dotted string.
    @Test
    public void notificationServiceExtensionLeadingDotUnknownClassReportsResolvedFqn() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.notification_service_extension", ".foo.NotExisting");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("com.pushwoosh.test.foo.NotExisting") && s.contains("not found"))));
    }

    @Test
    public void notificationFactoryKeyAbsentIsSilent() {
        prepareMetaData(new Bundle());

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void notificationFactoryUnknownClassWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.notification_factory", "com.foo.NotExisting");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("com.foo.NotExisting") && s.contains("not found"))));
    }

    @Test
    public void notificationFactoryWrongBaseWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.notification_factory", "com.pushwoosh.test.manifest.FakeNotificationService");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNotificationService") && s.contains("does not extend"))));
    }

    @Test
    public void notificationFactoryNoCtorWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.notification_factory", "com.pushwoosh.test.manifest.FakeNoCtorNotificationFactory");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNoCtorNotificationFactory")
                        && s.contains("no public no-argument constructor"))));
    }

    @Test
    public void notificationFactoryValidIsSilent() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.notification_factory", "com.pushwoosh.test.manifest.FakeValidNotificationFactory");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void summaryNotificationFactoryKeyAbsentIsSilent() {
        prepareMetaData(new Bundle());

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void summaryNotificationFactoryUnknownClassWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.summary_notification_factory", "com.foo.NotExisting");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("com.foo.NotExisting") && s.contains("not found"))));
    }

    @Test
    public void summaryNotificationFactoryWrongBaseWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.summary_notification_factory", "com.pushwoosh.test.manifest.FakeNotificationService");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNotificationService") && s.contains("does not extend"))));
    }

    @Test
    public void summaryNotificationFactoryNoCtorWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.summary_notification_factory",
                "com.pushwoosh.notification.FakeNoCtorSummaryNotificationFactory");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNoCtorSummaryNotificationFactory")
                        && s.contains("no public no-argument constructor"))));
    }

    @Test
    public void summaryNotificationFactoryValidIsSilent() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.summary_notification_factory",
                "com.pushwoosh.notification.FakeValidSummaryNotificationFactory");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void callEventListenerKeyAbsentIsSilent() {
        prepareMetaData(new Bundle());

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void callEventListenerUnknownClassWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.CALL_EVENT_LISTENER", "com.foo.NotExisting");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("com.foo.NotExisting") && s.contains("not found"))));
    }

    @Test
    public void callEventListenerWrongBaseWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.CALL_EVENT_LISTENER", "com.pushwoosh.test.manifest.FakeNotificationService");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNotificationService") && s.contains("does not implement"))));
    }

    @Test
    public void callEventListenerNoCtorWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.CALL_EVENT_LISTENER", "com.pushwoosh.test.manifest.FakeNoCtorCallEventListener");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) ->
                        s.contains("FakeNoCtorCallEventListener") && s.contains("no public no-argument constructor"))));
    }

    @Test
    public void callEventListenerValidIsSilent() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.CALL_EVENT_LISTENER", "com.pushwoosh.test.manifest.FakeValidCallEventListener");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void liveUpdateStyleProviderKeyAbsentIsSilent() {
        prepareMetaData(new Bundle());

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void liveUpdateStyleProviderUnknownClassWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER", "com.foo.NotExisting");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("com.foo.NotExisting") && s.contains("not found"))));
    }

    @Test
    public void liveUpdateStyleProviderWrongBaseWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER", "com.pushwoosh.test.manifest.FakeNotificationService");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNotificationService") && s.contains("does not implement"))));
    }

    @Test
    public void liveUpdateStyleProviderNoCtorWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER",
                "com.pushwoosh.test.manifest.FakeNoCtorLiveUpdateStyleProvider");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNoCtorLiveUpdateStyleProvider")
                        && s.contains("no public no-argument constructor"))));
    }

    @Test
    public void liveUpdateStyleProviderValidIsSilent() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER",
                "com.pushwoosh.test.manifest.FakeValidLiveUpdateStyleProvider");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    // The declared user class exists, but the module's base type is absent from the classpath
    // (integrator added the <meta-data> without the matching module dependency). Driven directly
    // because every real optional-module base is on the core test classpath.
    @Test
    public void optionalModuleClassBaseNotOnClasspathWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER",
                "com.pushwoosh.test.manifest.FakeValidLiveUpdateStyleProvider");

        ManifestValidator.validateOptionalModuleClass(
                metaData,
                "com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER",
                "com.example.AbsentModuleBase",
                "pushwoosh-some-module",
                "AbsentModuleBase");

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("pushwoosh-some-module") && s.contains("not on the classpath"))));
    }

    @Test
    public void pluginProviderKeyAbsentIsSilent() {
        prepareMetaData(new Bundle());

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void pluginProviderUnknownClassWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.internal.plugin_provider", "com.foo.NotExisting");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("com.foo.NotExisting") && s.contains("not found"))));
    }

    @Test
    public void pluginProviderWrongBaseWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.internal.plugin_provider", "com.pushwoosh.test.manifest.FakeNotificationService");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNotificationService") && s.contains("does not implement"))));
    }

    @Test
    public void pluginProviderNoCtorWarns() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.internal.plugin_provider", "com.pushwoosh.test.manifest.FakeNoCtorPluginProvider");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) ->
                        s.contains("FakeNoCtorPluginProvider") && s.contains("no public no-argument constructor"))));
    }

    @Test
    public void pluginProviderValidIsSilent() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.internal.plugin_provider", "com.pushwoosh.test.manifest.FakePluginProvider");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void customPluginsAllAbsentIsSilent() {
        prepareMetaData(new Bundle());

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void customPluginsUnknownClassWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.plugin.foo", "com.foo.NotExisting");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("com.foo.NotExisting") && s.contains("not found"))));
    }

    @Test
    public void customPluginsWrongBaseWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.plugin.foo", "com.pushwoosh.test.manifest.FakeNotificationService");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("FakeNotificationService") && s.contains("does not implement"))));
    }

    @Test
    public void customPluginsNoCtorWarns() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.plugin.foo", "com.pushwoosh.test.manifest.FakeNoCtorPlugin");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) ->
                        s.contains("FakeNoCtorPlugin") && s.contains("no public no-argument constructor"))));
    }

    @Test
    public void customPluginsValidIsSilent() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.plugin.foo", "com.pushwoosh.test.manifest.FakePlugin");
        metaData.putString("com.pushwoosh.plugin.bar", "com.pushwoosh.test.manifest.FakePluginTwo");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void customPluginsMixedValidAndBrokenWarnsOnlyOnBroken() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.plugin.good", "com.pushwoosh.test.manifest.FakePlugin");
        metaData.putString("com.pushwoosh.plugin.bad", "com.foo.NotExisting");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq("ManifestValidator"),
                argThat((String s) -> s.contains("com.foo.NotExisting") && s.contains("not found"))));
    }

    @Test
    public void customPluginsDoesNotProcessPluginProviderKey() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.internal.plugin_provider", "com.pushwoosh.test.manifest.FakeNotificationService");
        prepareMetaData(metaData);

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), times(1));
    }

    @Test
    public void providerMatchWithProviderPresentIsSilent() {
        prepareMetaData(new Bundle());

        ManifestValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(eq("ManifestValidator"), anyString()), never());
    }

    @Test
    public void providerMatchWithProviderAbsentWarns() {
        prepareMetaData(new Bundle());
        providerMock.when(DeviceSpecificProvider::getInstance).thenReturn(null);

        ManifestValidator.validate();

        pwLogMock.verify(() ->
                PWLog.warn(eq("ManifestValidator"), argThat((String s) -> s.contains("push notifications providers"))));
    }

    @Test
    public void validateWithNullAppInfoProviderStillReportsProviderMatch() {
        platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(null);
        providerMock.when(DeviceSpecificProvider::getInstance).thenReturn(null);

        ManifestValidator.validate();

        pwLogMock.verify(() ->
                PWLog.warn(eq("ManifestValidator"), argThat((String s) -> s.contains("push notifications providers"))));
    }

    @Test
    public void validateWithNullMetaDataStillReportsProviderMatch() {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.metaData = null;
        when(appInfoProvider.getApplicationInfo()).thenReturn(applicationInfo);
        providerMock.when(DeviceSpecificProvider::getInstance).thenReturn(null);

        ManifestValidator.validate();

        pwLogMock.verify(() ->
                PWLog.warn(eq("ManifestValidator"), argThat((String s) -> s.contains("push notifications providers"))));
    }

    @Test
    public void scheduleValidationSwallowsExceptionAndReportsViaPWLog() {
        when(appInfoProvider.getApplicationInfo()).thenThrow(new RuntimeException("boom"));

        // Synchronous executor: the lambda runs on the test thread, so MockedStatic mocks apply,
        // and we can deterministically assert PWLog.exception was called.
        ManifestValidator.scheduleValidation(Runnable::run);

        pwLogMock.verify(() -> PWLog.exception(any(Throwable.class)));
    }
}
