package com.pushwoosh.appevents;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.ApplicationOpenDetector;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.tags.TagsBundle;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class PushwooshDefaultEventsTest {

    @Mock
    private static PushwooshPlatform pushwooshPlatform = mock(PushwooshPlatform.class);

    @Mock
    private static PushwooshRepository pushwooshRepository = mock(PushwooshRepository.class);

    @Mock
    private static AppInfoProvider appInfoProvider = mock(AppInfoProvider.class);

    @Mock
    private static DeviceSpecificProvider deviceSpecificProvider = mock(DeviceSpecificProvider.class);

    @Before
    public void setUp() throws Exception {
        when(deviceSpecificProvider.deviceType()).thenReturn(10);
        when(appInfoProvider.getVersionName()).thenReturn("v1.0.0");
    }

    @After
    public void tearDown() throws Exception {}

    // Restored from cross-check: pins PW_UserIdle wire contract (idle_seconds int + session_duration long).
    @Test
    public void buildIdleAttributes_allFieldsPresent_returnsBundleWithIdleData() {
        try (MockedStatic<PushwooshPlatform> pushwooshPlatformMockedStatic = mockStatic(PushwooshPlatform.class);
                MockedStatic<DeviceSpecificProvider> deviceSpecificProviderMockedStatic =
                        mockStatic(DeviceSpecificProvider.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        mockStatic(AndroidPlatformModule.class)) {
            deviceSpecificProviderMockedStatic
                    .when(DeviceSpecificProvider::getInstance)
                    .thenReturn(deviceSpecificProvider);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getAppInfoProvider)
                    .thenReturn(appInfoProvider);
            pushwooshPlatformMockedStatic.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);

            TagsBundle attributes = PushwooshDefaultEvents.buildIdleAttributes("MainActivity/Cart", 120L, 30);

            Assert.assertNotNull(attributes);
            Assert.assertEquals("MainActivity/Cart", attributes.getString("screen_name"));
            Assert.assertEquals(30, attributes.getInt("idle_seconds", -1));
            Assert.assertEquals(120L, attributes.getLong("session_duration", -1L));
            Assert.assertEquals(10, attributes.getInt("device_type", -1));
            Assert.assertEquals("v1.0.0", attributes.getString("application_version"));
        }
    }

    // Restored from cross-check: pins PW_ApplicationExit wire contract (exit_intent_seconds int + session_duration
    // long).
    @Test
    public void buildExitIntentAttributes_populatesAllKeys() {
        try (MockedStatic<PushwooshPlatform> pushwooshPlatformMockedStatic = mockStatic(PushwooshPlatform.class);
                MockedStatic<DeviceSpecificProvider> deviceSpecificProviderMockedStatic =
                        mockStatic(DeviceSpecificProvider.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        mockStatic(AndroidPlatformModule.class)) {
            deviceSpecificProviderMockedStatic
                    .when(DeviceSpecificProvider::getInstance)
                    .thenReturn(deviceSpecificProvider);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getAppInfoProvider)
                    .thenReturn(appInfoProvider);
            pushwooshPlatformMockedStatic.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);

            TagsBundle attributes = PushwooshDefaultEvents.buildExitIntentAttributes("MainActivity/Profile", 42L, 15);

            Assert.assertNotNull(attributes);
            Assert.assertEquals(10, attributes.getInt("device_type", -1));
            Assert.assertEquals("v1.0.0", attributes.getString("application_version"));
            Assert.assertEquals("MainActivity/Profile", attributes.getString("screen_name"));
            Assert.assertEquals(42L, attributes.getLong("session_duration", -1L));
            Assert.assertEquals(15, attributes.getInt("exit_intent_seconds", -1));
        }
    }

    @Test
    public void checkBuildAttributes() {
        try (MockedStatic<PushwooshPlatform> pushwooshPlatformMockedStatic = mockStatic(PushwooshPlatform.class);
                MockedStatic<DeviceSpecificProvider> deviceSpecificProviderMockedStatic =
                        mockStatic(DeviceSpecificProvider.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        mockStatic(AndroidPlatformModule.class)) {
            deviceSpecificProviderMockedStatic
                    .when(DeviceSpecificProvider::getInstance)
                    .thenReturn(deviceSpecificProvider);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getAppInfoProvider)
                    .thenReturn(appInfoProvider);
            pushwooshPlatformMockedStatic.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);
            when(pushwooshPlatform.pushwooshRepository()).thenReturn(pushwooshRepository);

            TagsBundle appOpenEventAttributes = PushwooshDefaultEvents.buildAttributes(
                    PushwooshDefaultEvents.APPLICATION_OPENED_EVENT, "activityName");

            Assert.assertNotNull(appOpenEventAttributes);
            Assert.assertEquals(appOpenEventAttributes.getInt("device_type", -1), 10);
            Assert.assertEquals(appOpenEventAttributes.getString("application_version"), "v1.0.0");
            Assert.assertNull(appOpenEventAttributes.getString("screen_name"));

            TagsBundle screenOpenAttributes =
                    PushwooshDefaultEvents.buildAttributes(PushwooshDefaultEvents.SCREEN_OPENED_EVENT, "activityName");

            Assert.assertNotNull(screenOpenAttributes);
            Assert.assertEquals(screenOpenAttributes.getInt("device_type", -1), 10);
            Assert.assertEquals(screenOpenAttributes.getString("application_version"), "v1.0.0");
            Assert.assertEquals(screenOpenAttributes.getString("screen_name"), "activityName");
        }
    }

    @Test
    public void init_applicationContextIsNull_subscribesToApplicationOpenEventAndSkipsRegistration() throws Exception {
        resetActivityLifecycleCallbacks();

        try (MockedStatic<AndroidPlatformModule> platformModuleMockedStatic = mockStatic(AndroidPlatformModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = mockStatic(EventBus.class)) {
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(null);

            new PushwooshDefaultEvents().init();

            eventBusMockedStatic.verify(
                    () -> EventBus.subscribe(eq(ApplicationOpenDetector.ApplicationOpenEvent.class), any()), times(1));
            Assert.assertNull(readActivityLifecycleCallbacks());
        }
    }

    @Test
    public void init_applicationContextPresent_registersLifecycleCallbacks() throws Exception {
        resetActivityLifecycleCallbacks();

        Application app = mock(Application.class);
        Config config = mock(Config.class);
        when(config.getIdleTimeoutSeconds()).thenReturn(0);
        when(config.getExitIntentTimeoutSeconds()).thenReturn(0);

        try (MockedStatic<AndroidPlatformModule> platformModuleMockedStatic = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMockedStatic = mockStatic(PushwooshPlatform.class);
                MockedStatic<EventBus> eventBusMockedStatic = mockStatic(EventBus.class)) {
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(app);
            pushwooshPlatformMockedStatic.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);
            when(pushwooshPlatform.getConfig()).thenReturn(config);

            new PushwooshDefaultEvents().init();

            verify(app, times(1)).registerActivityLifecycleCallbacks(any(PushwooshAppLifecycleCallbacks.class));
            eventBusMockedStatic.verify(
                    () -> EventBus.subscribe(eq(ApplicationOpenDetector.ApplicationOpenEvent.class), any()), never());
            Assert.assertNotNull(readActivityLifecycleCallbacks());
        }

        resetActivityLifecycleCallbacks();
    }

    // Restored from cross-check: pins the `if (activityLifecycleCallbacks == null)` idempotency guard.
    // Removing it would double-register on every init() call, causing duplicate PW_ApplicationOpen events.
    @Test
    public void init_calledTwiceWithSameAppContext_registersLifecycleCallbacksOnce() throws Exception {
        resetActivityLifecycleCallbacks();

        Application app = mock(Application.class);
        Config config = mock(Config.class);
        when(config.getIdleTimeoutSeconds()).thenReturn(0);
        when(config.getExitIntentTimeoutSeconds()).thenReturn(0);

        try (MockedStatic<AndroidPlatformModule> platformModuleMockedStatic = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMockedStatic = mockStatic(PushwooshPlatform.class)) {
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(app);
            pushwooshPlatformMockedStatic.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);
            when(pushwooshPlatform.getConfig()).thenReturn(config);

            PushwooshDefaultEvents instance = new PushwooshDefaultEvents();
            instance.init();
            instance.init();

            verify(app, times(1)).registerActivityLifecycleCallbacks(any(PushwooshAppLifecycleCallbacks.class));
        }

        resetActivityLifecycleCallbacks();
    }

    private static void resetActivityLifecycleCallbacks() throws Exception {
        Field field = PushwooshDefaultEvents.class.getDeclaredField("activityLifecycleCallbacks");
        field.setAccessible(true);
        field.set(null, null);
    }

    private static Object readActivityLifecycleCallbacks() throws Exception {
        Field field = PushwooshDefaultEvents.class.getDeclaredField("activityLifecycleCallbacks");
        field.setAccessible(true);
        return field.get(null);
    }
}
