/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.notification.handlers.message.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.os.Bundle;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.LockScreenUtils;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.repository.LockScreenMediaStorage;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.SilentRichMediaStorage;
import com.pushwoosh.richmedia.RichMediaController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class RichMediaMessageHandlerTest {

    private static final String RICH_MEDIA_JSON =
            "{\"url\":\"https://richmedia.pushwoosh.com/A/B/CODE.zip\",\"ts\":1730000000}";

    private AutoCloseable mocks;

    private PushMessage pushMessage;
    private Bundle pushBundle;
    private NotificationPrefs notificationPrefs;
    private PreferenceBooleanValue notificationEnabledPref;
    private PreferenceStringValue messageHashPref;

    private LockScreenMediaStorage lockScreenMediaStorage;
    private SilentRichMediaStorage silentRichMediaStorage;
    private RichMediaController richMediaController;
    private InAppRepository inAppRepository;
    private PushwooshRepository pushwooshRepository;
    private PushwooshPlatform pushwooshPlatform;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        pushMessage = mock(PushMessage.class);
        pushBundle = new Bundle();
        when(pushMessage.toBundle()).thenReturn(pushBundle);
        when(pushMessage.getSound()).thenReturn("sound.mp3");

        notificationPrefs = mock(NotificationPrefs.class);
        notificationEnabledPref = mock(PreferenceBooleanValue.class);
        messageHashPref = mock(PreferenceStringValue.class);
        when(notificationEnabledPref.get()).thenReturn(true);
        when(notificationPrefs.notificationEnabled()).thenReturn(notificationEnabledPref);
        when(notificationPrefs.messageHash()).thenReturn(messageHashPref);

        lockScreenMediaStorage = mock(LockScreenMediaStorage.class);
        silentRichMediaStorage = mock(SilentRichMediaStorage.class);
        richMediaController = mock(RichMediaController.class);
        inAppRepository = mock(InAppRepository.class);
        pushwooshRepository = mock(PushwooshRepository.class);
        pushwooshPlatform = mock(PushwooshPlatform.class);
        when(pushwooshPlatform.getRichMediaController()).thenReturn(richMediaController);
        when(pushwooshPlatform.pushwooshRepository()).thenReturn(pushwooshRepository);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    private void stubGlobalStatics(
            MockedStatic<RepositoryModule> repositoryModuleMock,
            MockedStatic<PushwooshPlatform> pushwooshPlatformMock,
            MockedStatic<BackgroundExecutor> backgroundExecutorMock) {
        repositoryModuleMock.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefs);
        repositoryModuleMock.when(RepositoryModule::getLockScreenMediaStorage).thenReturn(lockScreenMediaStorage);
        repositoryModuleMock.when(RepositoryModule::getSilentRichMediaStorage).thenReturn(silentRichMediaStorage);
        pushwooshPlatformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);
        backgroundExecutorMock
                .when(() -> BackgroundExecutor.executeOnPool(any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                });
    }

    // Verifies that lock-screen push shows Rich Media immediately when the screen is locked.
    @Test
    public void handleNotification_lockScreenAndScreenLocked_showsResourceWrapper() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            when(pushMessage.isLockScreen()).thenReturn(true);
            lockScreenUtilsMock.when(LockScreenUtils::isScreenLocked).thenReturn(true);

            new RichMediaMessageHandler().handleNotification(pushMessage);

            ArgumentCaptor<ResourceWrapper> wrapperCaptor = ArgumentCaptor.forClass(ResourceWrapper.class);
            verify(richMediaController, times(1)).showResourceWrapper(wrapperCaptor.capture());
            ResourceWrapper wrapper = wrapperCaptor.getValue();
            assertTrue(wrapper.isLockScreen());
            assertEquals("sound.mp3", wrapper.getSound());
            verify(lockScreenMediaStorage, never()).cacheResource(any());
        }
    }

    // Verifies that lock-screen push caches the resource when the screen is unlocked instead of showing it.
    @Test
    public void handleNotification_lockScreenAndScreenUnlocked_cachesResource() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            when(pushMessage.isLockScreen()).thenReturn(true);
            lockScreenUtilsMock.when(LockScreenUtils::isScreenLocked).thenReturn(false);

            new RichMediaMessageHandler().handleNotification(pushMessage);

            verify(lockScreenMediaStorage, times(1)).cacheResource(pushMessage);
            verify(richMediaController, never()).showResourceWrapper(any());
        }
    }

    // Verifies that silent push with pw_force_show_rm in foreground shows Rich Media immediately.
    @Test
    public void handleNotification_silentForceShowInForeground_showsResourceWrapper() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            when(pushMessage.isLockScreen()).thenReturn(false);
            when(pushMessage.isSilent()).thenReturn(true);
            when(pushMessage.getCustomData()).thenReturn("{\"pw_force_show_rm\":true}");
            androidPlatformModuleMock
                    .when(AndroidPlatformModule::isApplicationInForeground)
                    .thenReturn(true);

            new RichMediaMessageHandler().handleNotification(pushMessage);

            ArgumentCaptor<ResourceWrapper> wrapperCaptor = ArgumentCaptor.forClass(ResourceWrapper.class);
            verify(richMediaController, times(1)).showResourceWrapper(wrapperCaptor.capture());
            assertFalse(wrapperCaptor.getValue().isLockScreen());
            verify(silentRichMediaStorage, never()).replaceResource(any());
        }
    }

    // Verifies that silent push with pw_force_show_rm in background defers via SilentRichMediaStorage.
    @Test
    public void handleNotification_silentForceShowInBackground_defersToSilentStorage() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            when(pushMessage.isLockScreen()).thenReturn(false);
            when(pushMessage.isSilent()).thenReturn(true);
            when(pushMessage.getCustomData()).thenReturn("{\"pw_force_show_rm\":true}");
            androidPlatformModuleMock
                    .when(AndroidPlatformModule::isApplicationInForeground)
                    .thenReturn(false);

            new RichMediaMessageHandler().handleNotification(pushMessage);

            verify(silentRichMediaStorage, times(1)).replaceResource(pushMessage);
            verify(richMediaController, never()).showResourceWrapper(any());
        }
    }

    // Verifies that handleNotification no-ops when richMedia is empty (early return).
    @Test
    public void handleNotification_richMediaEmpty_doesNothing() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn("");
            when(pushMessage.isLockScreen()).thenReturn(true);
            lockScreenUtilsMock.when(LockScreenUtils::isScreenLocked).thenReturn(true);

            new RichMediaMessageHandler().handleNotification(pushMessage);

            verify(lockScreenMediaStorage, never()).cacheResource(any());
            verify(silentRichMediaStorage, never()).replaceResource(any());
            verify(richMediaController, never()).showResourceWrapper(any());
            backgroundExecutorMock.verify(() -> BackgroundExecutor.executeOnPool(any(Runnable.class)), never());
        }
    }

    // Verifies that handleNotification no-ops for non-lockScreen, non-silent push with richMedia present.
    @Test
    public void handleNotification_notLockScreenNotSilent_doesNothing() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            when(pushMessage.isLockScreen()).thenReturn(false);
            when(pushMessage.isSilent()).thenReturn(false);

            new RichMediaMessageHandler().handleNotification(pushMessage);

            verify(lockScreenMediaStorage, never()).cacheResource(any());
            verify(silentRichMediaStorage, never()).replaceResource(any());
            verify(richMediaController, never()).showResourceWrapper(any());
            backgroundExecutorMock.verify(() -> BackgroundExecutor.executeOnPool(any(Runnable.class)), never());
        }
    }

    // Verifies that handleNotification no-ops for silent push without pw_force_show_rm flag.
    @Test
    public void handleNotification_silentWithoutForceShowFlag_doesNothing() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            when(pushMessage.isLockScreen()).thenReturn(false);
            when(pushMessage.isSilent()).thenReturn(true);
            when(pushMessage.getCustomData()).thenReturn("{}");

            new RichMediaMessageHandler().handleNotification(pushMessage);

            verify(lockScreenMediaStorage, never()).cacheResource(any());
            verify(silentRichMediaStorage, never()).replaceResource(any());
            verify(richMediaController, never()).showResourceWrapper(any());
            backgroundExecutorMock.verify(() -> BackgroundExecutor.executeOnPool(any(Runnable.class)), never());
        }
    }

    // Verifies that handleNotification no-ops for silent push with empty customData.
    @Test
    public void handleNotification_silentWithEmptyCustomData_doesNothing() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            when(pushMessage.isLockScreen()).thenReturn(false);
            when(pushMessage.isSilent()).thenReturn(true);
            when(pushMessage.getCustomData()).thenReturn("");

            new RichMediaMessageHandler().handleNotification(pushMessage);

            verify(lockScreenMediaStorage, never()).cacheResource(any());
            verify(silentRichMediaStorage, never()).replaceResource(any());
            verify(richMediaController, never()).showResourceWrapper(any());
            backgroundExecutorMock.verify(() -> BackgroundExecutor.executeOnPool(any(Runnable.class)), never());
        }
    }

    // Verifies that handleNotification treats malformed customData JSON as no flag (catches JSONException).
    @Test
    public void handleNotification_silentWithMalformedCustomData_doesNothingAndSwallowsException() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            when(pushMessage.isLockScreen()).thenReturn(false);
            when(pushMessage.isSilent()).thenReturn(true);
            when(pushMessage.getCustomData()).thenReturn("not-json{");

            new RichMediaMessageHandler().handleNotification(pushMessage);

            verify(lockScreenMediaStorage, never()).cacheResource(any());
            verify(silentRichMediaStorage, never()).replaceResource(any());
            verify(richMediaController, never()).showResourceWrapper(any());
        }
    }

    // Verifies that handlePushMessage prefetches Rich Media and tags when richMedia is present and push is not silent.
    @Test
    public void handlePushMessage_richMediaPresentNotSilent_prefetchesAndDelegates() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.isSilent(pushBundle))
                    .thenReturn(false);
            inAppModuleMock.when(InAppModule::getInAppRepository).thenReturn(inAppRepository);
            when(pushMessage.isLockScreen()).thenReturn(false);
            when(pushMessage.isSilent()).thenReturn(false);

            new RichMediaMessageHandler().handlePushMessage(pushMessage);

            verify(inAppRepository, times(1)).prefetchRichMedia(RICH_MEDIA_JSON);
            verify(pushwooshRepository, times(1)).prefetchTags();
            verify(messageHashPref, never()).set(Mockito.anyString());
        }
    }

    // Verifies that handlePushMessage records messageHash when push is silent with richMedia.
    @Test
    public void handlePushMessage_richMediaPresentAndSilent_recordsMessageHash() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.isSilent(pushBundle))
                    .thenReturn(true);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn("hash-xyz");
            inAppModuleMock.when(InAppModule::getInAppRepository).thenReturn(inAppRepository);
            when(pushMessage.isLockScreen()).thenReturn(false);
            when(pushMessage.isSilent()).thenReturn(false);

            new RichMediaMessageHandler().handlePushMessage(pushMessage);

            verify(messageHashPref, times(1)).set("hash-xyz");
        }
    }

    // Verifies that handlePushMessage skips prefetch when InAppModule.getInAppRepository() is null and does not throw.
    @Test
    public void handlePushMessage_inAppRepositoryNull_skipsPrefetchWithoutThrowing() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.isSilent(pushBundle))
                    .thenReturn(false);
            inAppModuleMock.when(InAppModule::getInAppRepository).thenReturn(null);
            when(pushMessage.isLockScreen()).thenReturn(false);
            when(pushMessage.isSilent()).thenReturn(false);

            new RichMediaMessageHandler().handlePushMessage(pushMessage);

            verify(pushwooshRepository, never()).prefetchTags();
            verify(notificationEnabledPref, times(1)).get();
        }
    }

    // Verifies that handlePushMessage skips prefetch and hash write when richMedia is empty.
    @Test
    public void handlePushMessage_richMediaEmpty_noPrefetchNoHashWrite() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn("");
            when(pushMessage.isLockScreen()).thenReturn(false);
            when(pushMessage.isSilent()).thenReturn(false);

            new RichMediaMessageHandler().handlePushMessage(pushMessage);

            verify(inAppRepository, never()).prefetchRichMedia(Mockito.anyString());
            verify(pushwooshRepository, never()).prefetchTags();
            verify(messageHashPref, never()).set(Mockito.anyString());
            verify(notificationEnabledPref, times(1)).get();
        }
    }

    // Verifies that lock-screen path does not crash when RichMediaController is null (defensive null-guard).
    @Test
    public void handleNotification_lockScreenLockedButControllerNull_doesNotCrash() {
        try (MockedStatic<RepositoryModule> repositoryModuleMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushBundleDataProvider> pushBundleDataProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<LockScreenUtils> lockScreenUtilsMock = Mockito.mockStatic(LockScreenUtils.class);
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMock =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<BackgroundExecutor> backgroundExecutorMock = Mockito.mockStatic(BackgroundExecutor.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMock = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<InAppModule> inAppModuleMock = Mockito.mockStatic(InAppModule.class)) {
            stubGlobalStatics(repositoryModuleMock, pushwooshPlatformMock, backgroundExecutorMock);
            pushBundleDataProviderMock
                    .when(() -> PushBundleDataProvider.getRichMedia(pushBundle))
                    .thenReturn(RICH_MEDIA_JSON);
            when(pushMessage.isLockScreen()).thenReturn(true);
            lockScreenUtilsMock.when(LockScreenUtils::isScreenLocked).thenReturn(true);
            when(pushwooshPlatform.getRichMediaController()).thenReturn(null);

            new RichMediaMessageHandler().handleNotification(pushMessage);

            verifyNoInteractions(richMediaController);
        }
    }
}
