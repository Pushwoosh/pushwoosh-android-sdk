package com.pushwoosh.notification.handlers.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Bundle;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.richmedia.RichMediaController;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class RichMediaPushNotificationOpenHandlerTest {

    private static final String VALID_RICH_MEDIA =
            "{\"ts\":1730321891,\"url\":\"https://richmedia.pushwoosh.com/C/A/CAF38-1F50B.zip?ts=1730321891\"}";
    private static final String CUSTOM_DATA = "{\"k\":\"v\"}";
    private static final int RICH_MEDIA_DELAY_MS = 500;

    private PlatformTestManager platformTestManager;
    private AutoCloseable mocks;
    private NotificationPrefs originalNotificationPrefs;

    @Mock
    private NotificationPrefs notificationPrefs;

    @Mock
    private PreferenceIntValue richMediaDelayMs;

    @Mock
    private PreferenceStringValue customDataPref;

    @Mock
    private RichMediaController richMediaController;

    @Mock
    private Bundle bundle;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();

        originalNotificationPrefs = RepositoryModule.getNotificationPreferences();

        when(notificationPrefs.richMediaDelayMs()).thenReturn(richMediaDelayMs);
        when(notificationPrefs.customData()).thenReturn(customDataPref);
        when(richMediaDelayMs.get()).thenReturn(RICH_MEDIA_DELAY_MS);

        RepositoryModule.setNotificationPreferences(notificationPrefs);

        WhiteboxHelper.setInternalState(PushwooshPlatform.getInstance(), "richMediaController", richMediaController);
    }

    @After
    public void tearDown() throws Exception {
        RepositoryModule.setNotificationPreferences(originalNotificationPrefs);
        platformTestManager.tearDown();
        mocks.close();
    }

    // Verifies that a non-empty richMedia builds a ResourceWrapper with the prefs delay,
    // stores customData in prefs, and is dispatched to RichMediaController.
    @Test
    public void postHandleNotification_whenRichMediaPresentAndControllerSet_showsWrapper() {
        RichMediaPushNotificationOpenHandler handler = new RichMediaPushNotificationOpenHandler();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                Mockito.mockStatic(PushBundleDataProvider.class)) {
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(bundle))
                    .thenReturn(VALID_RICH_MEDIA);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getCustomData(bundle))
                    .thenReturn(CUSTOM_DATA);

            handler.postHandleNotification(bundle);

            verify(customDataPref).set(CUSTOM_DATA);

            ArgumentCaptor<ResourceWrapper> captor = ArgumentCaptor.forClass(ResourceWrapper.class);
            verify(richMediaController).showResourceWrapper(captor.capture());

            ResourceWrapper captured = captor.getValue();
            assertEquals(RICH_MEDIA_DELAY_MS, captured.getDelay());
            assertNotNull("Resource should be parsed from richMedia", captured.getResource());
            assertEquals(
                    "https://richmedia.pushwoosh.com/C/A/CAF38-1F50B.zip?ts=1730321891",
                    captured.getResource().getUrl());
        }
    }

    // Verifies that an empty richMedia short-circuits before reading customData and skips controller call.
    @Test
    public void postHandleNotification_whenRichMediaEmpty_returnsEarlyAndDoesNothing() {
        RichMediaPushNotificationOpenHandler handler = new RichMediaPushNotificationOpenHandler();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                Mockito.mockStatic(PushBundleDataProvider.class)) {
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(bundle))
                    .thenReturn("");

            handler.postHandleNotification(bundle);

            verify(customDataPref, never()).set(anyString());
            verify(richMediaController, never()).showResourceWrapper(any());
            bundleProviderMock.verify(() -> PushBundleDataProvider.getCustomData(any()), never());
        }
    }

    // Verifies that a null RichMediaController still triggers customData persistence without crashing.
    @Test
    public void postHandleNotification_whenControllerNull_stillSavesCustomDataAndDoesNotCrash() {
        WhiteboxHelper.setInternalState(PushwooshPlatform.getInstance(), "richMediaController", null);
        RichMediaPushNotificationOpenHandler handler = new RichMediaPushNotificationOpenHandler();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                Mockito.mockStatic(PushBundleDataProvider.class)) {
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(bundle))
                    .thenReturn(VALID_RICH_MEDIA);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getCustomData(bundle))
                    .thenReturn("cd");

            handler.postHandleNotification(bundle);

            verify(customDataPref).set("cd");
        }
    }

    // Verifies that a null customData is forwarded as-is to prefs and does not block showing rich media.
    @Test
    public void postHandleNotification_whenCustomDataNull_forwardsNullToPrefsAndShows() {
        RichMediaPushNotificationOpenHandler handler = new RichMediaPushNotificationOpenHandler();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                Mockito.mockStatic(PushBundleDataProvider.class)) {
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(bundle))
                    .thenReturn(VALID_RICH_MEDIA);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getCustomData(bundle))
                    .thenReturn(null);

            handler.postHandleNotification(bundle);

            verify(customDataPref).set(null);
            verify(richMediaController).showResourceWrapper(any(ResourceWrapper.class));
        }
    }
}
