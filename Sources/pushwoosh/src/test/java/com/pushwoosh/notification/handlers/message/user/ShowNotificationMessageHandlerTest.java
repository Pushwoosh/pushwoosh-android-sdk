/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.preference.PreferenceArrayListValue;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.notification.LocalNotificationReceiver;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushMessageTestTool;
import com.pushwoosh.notification.PushwooshNotificationFactory;
import com.pushwoosh.notification.SummaryNotificationUtils;
import com.pushwoosh.notification.builder.NotificationBuilderManager;
import com.pushwoosh.repository.InboxNotificationStorage;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.PushBundleStorage;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by aevstefeev on 13/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class ShowNotificationMessageHandlerTest {
    private ShowNotificationMessageHandler showNotificationMessageHandler;
    private PushMessageTestTool pushMessageTestTool;

    private PushwooshNotificationFactory pushwooshNotificationFactoryMock;
    private NotificationPrefs notificationPrefsMock;

    private PreferenceBooleanValue preferenceBooleanValueMock;

    @Before
    public void setUp() throws Exception {
        pushMessageTestTool = new PushMessageTestTool();
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void handleNotification() throws Exception {
        try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                        Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryNotificationUtils =
                        Mockito.mockStatic(SummaryNotificationUtils.class)) {
            notificationUtilsMockedStatic
                    .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(Mockito.any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);
            String groupId = "groupId";
            summaryNotificationUtils
                    .when(() -> SummaryNotificationUtils.getNotificationIdForGroup(groupId))
                    .thenReturn(1);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            notificationPrefsMock = mock(NotificationPrefs.class);
            preferenceBooleanValueMock = mock(PreferenceBooleanValue.class);
            when(preferenceBooleanValueMock.get()).thenReturn(true);
            when(notificationPrefsMock.notificationEnabled()).thenReturn(preferenceBooleanValueMock);
            when(notificationPrefsMock.multiMode()).thenReturn(preferenceBooleanValueMock);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getNotificationPreferences)
                    .thenReturn(notificationPrefsMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);

            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);

            PreferenceBooleanValue lightScreenMock = mock(PreferenceBooleanValue.class);
            when(lightScreenMock.get()).thenReturn(true);
            when(notificationPrefsMock.lightScreenOn()).thenReturn(lightScreenMock);

            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);
            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);

            Notification notification = new Notification();
            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(notification);
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(new Intent());

            showNotificationMessageHandler.handleNotification(pushMessage);

            verify(pushwooshNotificationFactoryMock, times(1)).onGenerateNotification(pushMessage);

            notificationUtilsMockedStatic.verify(NotificationUtils::turnScreenOn, times(1));
            EventBus.sendEvent(Mockito.any());
            verify(notificationPrefsMock).pushHistory();
            verify(history).add(Mockito.any(Serializable.class));
            verify(localNotificationStorageMock).removeLocalNotificationShown(Mockito.anyInt(), Mockito.anyString());
        }
    }

    @Test
    public void handlePushMessage() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryNotificationUtils =
                        Mockito.mockStatic(SummaryNotificationUtils.class)) {
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);
            String groupId = "groupId";
            summaryNotificationUtils
                    .when(() -> SummaryNotificationUtils.getNotificationIdForGroup(groupId))
                    .thenReturn(1);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            notificationPrefsMock = mock(NotificationPrefs.class);
            preferenceBooleanValueMock = mock(PreferenceBooleanValue.class);
            when(preferenceBooleanValueMock.get()).thenReturn(true);
            when(notificationPrefsMock.notificationEnabled()).thenReturn(preferenceBooleanValueMock);
            when(notificationPrefsMock.multiMode()).thenReturn(preferenceBooleanValueMock);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getNotificationPreferences)
                    .thenReturn(notificationPrefsMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);

            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);

            PreferenceBooleanValue lightScreenMock = mock(PreferenceBooleanValue.class);
            when(lightScreenMock.get()).thenReturn(true);
            when(notificationPrefsMock.lightScreenOn()).thenReturn(lightScreenMock);

            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);

            when(preferenceBooleanValueMock.get()).thenReturn(true);
            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            showNotificationMessageHandler.handlePushMessage(pushMessage);

            verify(pushwooshNotificationFactoryMock).onGenerateNotification(pushMessage);
        }
    }

    @Test
    public void handlePushMessageNegative() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryNotificationUtils =
                        Mockito.mockStatic(SummaryNotificationUtils.class)) {
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);
            String groupId = "groupId";
            summaryNotificationUtils
                    .when(() -> SummaryNotificationUtils.getNotificationIdForGroup(groupId))
                    .thenReturn(1);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            notificationPrefsMock = mock(NotificationPrefs.class);
            preferenceBooleanValueMock = mock(PreferenceBooleanValue.class);
            when(preferenceBooleanValueMock.get()).thenReturn(true);
            when(notificationPrefsMock.notificationEnabled()).thenReturn(preferenceBooleanValueMock);
            when(notificationPrefsMock.multiMode()).thenReturn(preferenceBooleanValueMock);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getNotificationPreferences)
                    .thenReturn(notificationPrefsMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);

            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);

            PreferenceBooleanValue lightScreenMock = mock(PreferenceBooleanValue.class);
            when(lightScreenMock.get()).thenReturn(true);
            when(notificationPrefsMock.lightScreenOn()).thenReturn(lightScreenMock);

            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);

            when(preferenceBooleanValueMock.get()).thenReturn(false);
            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            showNotificationMessageHandler.handlePushMessage(pushMessage);

            verify(pushwooshNotificationFactoryMock, Mockito.never()).onGenerateNotification(pushMessage);
        }
    }

    // Verifies that silent push triggers early exit and skips factory entirely.
    @Test
    public void handleNotification_silentPush_skipsFactory() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);
            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(/*isSilent*/ true);

            showNotificationMessageHandler.handleNotification(pushMessage);

            verify(pushwooshNotificationFactoryMock, never()).onGenerateNotification(any(PushMessage.class));
            verify(pushwooshNotificationFactoryMock, never()).getNotificationIntent(any(PushMessage.class));
            eventBusMockedStatic.verifyNoInteractions();
        }
    }

    // Verifies that single-mode happy path fires EventBus, history, and cancelAll.
    @Test
    public void handleNotification_singleModeHappyPath_firesEventAndHistory() throws Exception {
        try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                        Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class)) {
            notificationUtilsMockedStatic
                    .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);

            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);
            PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
            when(pushBundleStorageMock.putGroupPushBundle(any(Bundle.class), anyInt(), any()))
                    .thenReturn(42L);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getPushBundleStorage)
                    .thenReturn(pushBundleStorageMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            when(pushMessage.toBundle()).thenReturn(new Bundle());

            Notification notification = new Notification();
            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(notification);
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(new Intent());

            showNotificationMessageHandler.handleNotification(pushMessage);

            verify(pushwooshNotificationFactoryMock, times(1)).onGenerateNotification(pushMessage);
            verify(history).add(any(Serializable.class));
            verify(localNotificationStorageMock).removeLocalNotificationShown(anyInt(), anyString());
            ArgumentCaptor<NotificationCreatedEvent> eventCaptor =
                    ArgumentCaptor.forClass(NotificationCreatedEvent.class);
            eventBusMockedStatic.verify(() -> EventBus.sendEvent(eventCaptor.capture()), times(1));
            assertEquals("tag", eventCaptor.getValue().getMessageTag());
            assertEquals(0, eventCaptor.getValue().getMessageId());
        }
    }

    // Verifies that single-mode skips side-effects when factory returns null.
    @Test
    public void handleNotification_singleModeFactoryReturnsNull_noSideEffects() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);
            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);
            when(pushwooshNotificationFactoryMock.onGenerateNotification(any(PushMessage.class)))
                    .thenReturn(null);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);

            showNotificationMessageHandler.handleNotification(pushMessage);

            verify(history, never()).add(any(Serializable.class));
            eventBusMockedStatic.verifyNoInteractions();
        }
    }

    // Verifies that multi-mode skips summary and storage when factory returns null.
    @Test
    public void handleNotification_multiModeFactoryReturnsNull_skipsSummary() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryStatic =
                        Mockito.mockStatic(SummaryNotificationUtils.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);
            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ true);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);
            when(pushwooshNotificationFactoryMock.onGenerateNotification(any(PushMessage.class)))
                    .thenReturn(null);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);

            showNotificationMessageHandler.handleNotification(pushMessage);

            summaryStatic.verify(
                    () -> SummaryNotificationUtils.getSummaryNotification(anyInt(), any(), any()), never());
            summaryStatic.verify(() -> SummaryNotificationUtils.fireSummaryNotification(any()), never());
            verify(history, never()).add(any(Serializable.class));
            eventBusMockedStatic.verifyNoInteractions();
        }
    }

    // Verifies that multi-mode with active group fires summary notification.
    @Test
    public void handleNotification_multiModeWithActiveGroupNotifications_firesSummary() throws Exception {
        try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                        Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryStatic =
                        Mockito.mockStatic(SummaryNotificationUtils.class);
                MockedStatic<NotificationBuilderManager> builderStatic =
                        Mockito.mockStatic(NotificationBuilderManager.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            notificationUtilsMockedStatic
                    .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ true);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);
            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);
            PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
            when(pushBundleStorageMock.putGroupPushBundle(any(), anyInt(), any()))
                    .thenReturn(1L);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getPushBundleStorage)
                    .thenReturn(pushBundleStorageMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            when(pushMessage.toBundle()).thenReturn(new Bundle());
            when(pushMessage.getGroupId()).thenReturn("groupX");

            Notification notification = new Notification();
            notification.extras = new Bundle();
            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(notification);
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(new Intent());

            StatusBarNotification sbn = mock(StatusBarNotification.class);
            Notification existingInGroup = new Notification();
            when(sbn.getNotification()).thenReturn(existingInGroup);
            builderStatic
                    .when(NotificationBuilderManager::getActiveNotifications)
                    .thenReturn(new ArrayList<>(Collections.singletonList(sbn)));
            builderStatic
                    .when(() -> NotificationBuilderManager.isReplacingMessage(any(PushMessage.class), any()))
                    .thenReturn(false);

            Notification summary = new Notification();
            summaryStatic
                    .when(() -> SummaryNotificationUtils.getSummaryNotification(anyInt(), any(), any()))
                    .thenReturn(summary);

            showNotificationMessageHandler.handleNotification(pushMessage);

            summaryStatic.verify(() -> SummaryNotificationUtils.fireSummaryNotification(summary), times(1));
        }
    }

    // Verifies that multi-mode without active group notifications skips summary firing.
    @Test
    public void handleNotification_multiModeNoActiveGroup_skipsSummaryFiring() throws Exception {
        try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                        Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryStatic =
                        Mockito.mockStatic(SummaryNotificationUtils.class);
                MockedStatic<NotificationBuilderManager> builderStatic =
                        Mockito.mockStatic(NotificationBuilderManager.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            notificationUtilsMockedStatic
                    .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ true);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);
            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);
            PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
            when(pushBundleStorageMock.putGroupPushBundle(any(), anyInt(), any()))
                    .thenReturn(1L);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getPushBundleStorage)
                    .thenReturn(pushBundleStorageMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            when(pushMessage.toBundle()).thenReturn(new Bundle());

            Notification notification = new Notification();
            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(notification);
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(new Intent());

            builderStatic
                    .when(NotificationBuilderManager::getActiveNotifications)
                    .thenReturn(new ArrayList<>());

            showNotificationMessageHandler.handleNotification(pushMessage);

            summaryStatic.verify(
                    () -> SummaryNotificationUtils.getSummaryNotification(anyInt(), any(), any()), never());
            summaryStatic.verify(() -> SummaryNotificationUtils.fireSummaryNotification(any()), never());
            verify(history).add(any(Serializable.class));
        }
    }

    // Verifies that fireNotification exits early when application context is null.
    @Test
    public void handleNotification_nullContextInFire_exitsBeforeNotify() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            final int[] callCount = {0};
            // Constructor (NotificationFactory base) calls getApplicationContext once;
            // single-mode cancelAll calls it once more — both need a real context.
            // Then fireNotification's own getApplicationContext() must return null.
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenAnswer(inv -> {
                        callCount[0]++;
                        return callCount[0] <= 2 ? RuntimeEnvironment.application : null;
                    });
            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(new Notification());
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(new Intent());

            showNotificationMessageHandler.handleNotification(pushMessage);

            verify(history, never()).add(any(Serializable.class));
            eventBusMockedStatic.verifyNoInteractions();
        }
    }

    // Verifies that fireNotification exits early when the NotificationManager is null.
    @Test
    public void handleNotification_nullNotificationManager_exitsBeforeHistory() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                        Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            notificationUtilsMockedStatic
                    .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            when(managerProviderMock.getNotificationManager()).thenReturn(null);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);
            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);
            PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
            when(pushBundleStorageMock.putGroupPushBundle(any(), anyInt(), any()))
                    .thenReturn(1L);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getPushBundleStorage)
                    .thenReturn(pushBundleStorageMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            when(pushMessage.toBundle()).thenReturn(new Bundle());
            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(new Notification());
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(new Intent());

            showNotificationMessageHandler.handleNotification(pushMessage);

            verify(localNotificationStorageMock).removeLocalNotificationShown(anyInt(), anyString());
            verify(history, never()).add(any(Serializable.class));
            eventBusMockedStatic.verifyNoInteractions();
            notificationUtilsMockedStatic.verify(NotificationUtils::turnScreenOn, never());
        }
    }

    // Verifies that notifyNotificationCreated stores local notification id when extra is present.
    @Test
    public void notifyNotificationCreated_intentWithLocalNotificationId_storesIt() throws Exception {
        try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                        Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            notificationUtilsMockedStatic
                    .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);
            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);
            PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
            when(pushBundleStorageMock.putGroupPushBundle(any(), anyInt(), any()))
                    .thenReturn(1L);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getPushBundleStorage)
                    .thenReturn(pushBundleStorageMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            when(pushMessage.toBundle()).thenReturn(new Bundle());

            Intent intent = new Intent();
            intent.putExtra(LocalNotificationReceiver.EXTRA_NOTIFICATION_ID, 42);
            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(new Notification());
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(intent);

            showNotificationMessageHandler.handleNotification(pushMessage);

            verify(localNotificationStorageMock).removeLocalNotificationShown(0, "tag");
            verify(localNotificationStorageMock).addLocalNotificationShown(42, 0, "tag");
        }
    }

    // Verifies that notifyNotificationCreated skips addLocalNotificationShown without the extra.
    @Test
    public void notifyNotificationCreated_intentWithoutLocalNotificationId_skipsAdd() throws Exception {
        try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                        Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            notificationUtilsMockedStatic
                    .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);
            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);
            PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
            when(pushBundleStorageMock.putGroupPushBundle(any(), anyInt(), any()))
                    .thenReturn(1L);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getPushBundleStorage)
                    .thenReturn(pushBundleStorageMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            when(pushMessage.toBundle()).thenReturn(new Bundle());

            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(new Notification());
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(new Intent());

            showNotificationMessageHandler.handleNotification(pushMessage);

            verify(localNotificationStorageMock, times(1)).removeLocalNotificationShown(anyInt(), anyString());
            verify(localNotificationStorageMock, never()).addLocalNotificationShown(anyInt(), anyInt(), anyString());
        }
    }

    // Verifies that getMessageId increments and persists messageId in multi-mode with empty tag.
    @Test
    public void getMessageId_multiModeEmptyTag_incrementsAndPersists() throws Exception {
        try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                        Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class);
                MockedStatic<NotificationBuilderManager> builderStatic =
                        Mockito.mockStatic(NotificationBuilderManager.class);
                MockedStatic<SummaryNotificationUtils> summaryStatic =
                        Mockito.mockStatic(SummaryNotificationUtils.class)) {
            notificationUtilsMockedStatic
                    .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ true);
            PreferenceIntValue messageIdMock = mock(PreferenceIntValue.class);
            when(messageIdMock.get()).thenReturn(7);
            when(notificationPrefsMock.messageId()).thenReturn(messageIdMock);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);
            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);
            PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
            when(pushBundleStorageMock.putGroupPushBundle(any(), anyInt(), any()))
                    .thenReturn(1L);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getPushBundleStorage)
                    .thenReturn(pushBundleStorageMock);

            builderStatic
                    .when(NotificationBuilderManager::getActiveNotifications)
                    .thenReturn(new ArrayList<>());

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            when(pushMessage.getTag()).thenReturn("");
            when(pushMessage.toBundle()).thenReturn(new Bundle());

            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(new Notification());
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(new Intent());

            showNotificationMessageHandler.handleNotification(pushMessage);

            verify(messageIdMock).set(8);
            ArgumentCaptor<NotificationCreatedEvent> eventCaptor =
                    ArgumentCaptor.forClass(NotificationCreatedEvent.class);
            eventBusMockedStatic.verify(() -> EventBus.sendEvent(eventCaptor.capture()), times(1));
            assertEquals(8, eventCaptor.getValue().getMessageId());
        }
    }

    // Verifies that getMessageId does not persist when tag is empty and single-mode.
    @Test
    public void getMessageId_singleModeEmptyTag_doesNotPersist() throws Exception {
        try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                        Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            notificationUtilsMockedStatic
                    .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);
            PreferenceIntValue messageIdMock = mock(PreferenceIntValue.class);
            when(messageIdMock.get()).thenReturn(7);
            when(notificationPrefsMock.messageId()).thenReturn(messageIdMock);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);
            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);
            PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
            when(pushBundleStorageMock.putGroupPushBundle(any(), anyInt(), any()))
                    .thenReturn(1L);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getPushBundleStorage)
                    .thenReturn(pushBundleStorageMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            when(pushMessage.getTag()).thenReturn("");
            when(pushMessage.toBundle()).thenReturn(new Bundle());

            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(new Notification());
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(new Intent());

            showNotificationMessageHandler.handleNotification(pushMessage);

            verify(messageIdMock, never()).set(anyInt());
            ArgumentCaptor<NotificationCreatedEvent> eventCaptor =
                    ArgumentCaptor.forClass(NotificationCreatedEvent.class);
            eventBusMockedStatic.verify(() -> EventBus.sendEvent(eventCaptor.capture()), times(1));
            assertEquals(7, eventCaptor.getValue().getMessageId());
        }
    }

    // Verifies that saveNotificationIdAndTag persists inbox mapping on Android < M with pw_inbox.
    @Test
    public void saveNotificationIdAndTag_legacyAndroidWithInboxId_persistsMapping() throws Exception {
        int originalSdkInt = Build.VERSION.SDK_INT;
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 22);
        try {
            try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                            Mockito.mockStatic(NotificationUtils.class);
                    MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                            Mockito.mockStatic(AndroidPlatformModule.class);
                    MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                            Mockito.mockStatic(RepositoryModule.class);
                    MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
                notificationUtilsMockedStatic
                        .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                        .thenAnswer(inv -> inv.getArgument(0));
                platformModuleMockedStatic
                        .when(AndroidPlatformModule::getApplicationContext)
                        .thenReturn(RuntimeEnvironment.application);

                ManagerProvider managerProviderMock = mock(ManagerProvider.class);
                NotificationManager notificationManager = (NotificationManager)
                        RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
                when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
                platformModuleMockedStatic
                        .when(AndroidPlatformModule::getManagerProvider)
                        .thenReturn(managerProviderMock);

                stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);
                PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
                when(notificationPrefsMock.pushHistory()).thenReturn(history);
                LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
                repositoryModuleMockedStatic
                        .when(RepositoryModule::getLocalNotificationStorage)
                        .thenReturn(localNotificationStorageMock);
                PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
                when(pushBundleStorageMock.putGroupPushBundle(any(), anyInt(), any()))
                        .thenReturn(1L);
                repositoryModuleMockedStatic
                        .when(RepositoryModule::getPushBundleStorage)
                        .thenReturn(pushBundleStorageMock);

                InboxNotificationStorage inboxNotificationStorageMock = mock(InboxNotificationStorage.class);
                repositoryModuleMockedStatic
                        .when(RepositoryModule::getInboxNotificationStorage)
                        .thenReturn(inboxNotificationStorageMock);

                showNotificationMessageHandler = new ShowNotificationMessageHandler();
                pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
                WhiteboxHelper.setInternalState(
                        showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

                PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
                Bundle bundle = new Bundle();
                bundle.putString("pw_inbox", "inbox-123");
                when(pushMessage.toBundle()).thenReturn(bundle);

                when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                        .thenReturn(new Notification());
                when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                        .thenReturn(new Intent());

                showNotificationMessageHandler.handleNotification(pushMessage);

                verify(inboxNotificationStorageMock).putNotificationIdAndTag("inbox-123", 0, "tag");
            }
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", originalSdkInt);
        }
    }

    // Verifies that saveNotificationIdAndTag skips inbox mapping on Android < M when pw_inbox absent.
    @Test
    public void saveNotificationIdAndTag_legacyAndroidWithoutInboxId_skipsMapping() throws Exception {
        int originalSdkInt = Build.VERSION.SDK_INT;
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 22);
        try {
            try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                            Mockito.mockStatic(NotificationUtils.class);
                    MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                            Mockito.mockStatic(AndroidPlatformModule.class);
                    MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                            Mockito.mockStatic(RepositoryModule.class);
                    MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
                notificationUtilsMockedStatic
                        .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                        .thenAnswer(inv -> inv.getArgument(0));
                platformModuleMockedStatic
                        .when(AndroidPlatformModule::getApplicationContext)
                        .thenReturn(RuntimeEnvironment.application);

                ManagerProvider managerProviderMock = mock(ManagerProvider.class);
                NotificationManager notificationManager = (NotificationManager)
                        RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
                when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
                platformModuleMockedStatic
                        .when(AndroidPlatformModule::getManagerProvider)
                        .thenReturn(managerProviderMock);

                stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);
                PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
                when(notificationPrefsMock.pushHistory()).thenReturn(history);
                LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
                repositoryModuleMockedStatic
                        .when(RepositoryModule::getLocalNotificationStorage)
                        .thenReturn(localNotificationStorageMock);
                PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
                when(pushBundleStorageMock.putGroupPushBundle(any(), anyInt(), any()))
                        .thenReturn(1L);
                repositoryModuleMockedStatic
                        .when(RepositoryModule::getPushBundleStorage)
                        .thenReturn(pushBundleStorageMock);

                InboxNotificationStorage inboxNotificationStorageMock = mock(InboxNotificationStorage.class);
                repositoryModuleMockedStatic
                        .when(RepositoryModule::getInboxNotificationStorage)
                        .thenReturn(inboxNotificationStorageMock);

                showNotificationMessageHandler = new ShowNotificationMessageHandler();
                pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
                WhiteboxHelper.setInternalState(
                        showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

                PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
                when(pushMessage.toBundle()).thenReturn(new Bundle());

                when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                        .thenReturn(new Notification());
                when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                        .thenReturn(new Intent());

                showNotificationMessageHandler.handleNotification(pushMessage);

                verify(inboxNotificationStorageMock, never())
                        .putNotificationIdAndTag(anyString(), anyInt(), anyString());
                verify(localNotificationStorageMock).removeLocalNotificationShown(anyInt(), anyString());
            }
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", originalSdkInt);
        }
    }

    // Verifies that saveNotificationIdAndTag is skipped entirely on Android M+ even with pw_inbox.
    @Test
    public void saveNotificationIdAndTag_modernAndroid_skippedEvenWithInboxId() throws Exception {
        try (MockedStatic<NotificationUtils> notificationUtilsMockedStatic =
                        Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic =
                        Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic =
                        Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class)) {
            notificationUtilsMockedStatic
                    .when(() -> NotificationUtils.rebuildWithDefaultValuesIfNeeded(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getApplicationContext)
                    .thenReturn(RuntimeEnvironment.application);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager =
                    (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic
                    .when(AndroidPlatformModule::getManagerProvider)
                    .thenReturn(managerProviderMock);

            stubNotificationPrefsBasics(repositoryModuleMockedStatic, /*multiMode*/ false);
            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);
            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getLocalNotificationStorage)
                    .thenReturn(localNotificationStorageMock);
            PushBundleStorage pushBundleStorageMock = mock(PushBundleStorage.class);
            when(pushBundleStorageMock.putGroupPushBundle(any(), anyInt(), any()))
                    .thenReturn(1L);
            repositoryModuleMockedStatic
                    .when(RepositoryModule::getPushBundleStorage)
                    .thenReturn(pushBundleStorageMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);
            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler, "notificationFactory", pushwooshNotificationFactoryMock);

            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            Bundle bundle = new Bundle();
            bundle.putString("pw_inbox", "inbox-xyz");
            when(pushMessage.toBundle()).thenReturn(bundle);

            when(pushwooshNotificationFactoryMock.onGenerateNotification(pushMessage))
                    .thenReturn(new Notification());
            when(pushwooshNotificationFactoryMock.getNotificationIntent(pushMessage))
                    .thenReturn(new Intent());

            showNotificationMessageHandler.handleNotification(pushMessage);

            repositoryModuleMockedStatic.verify(RepositoryModule::getInboxNotificationStorage, never());
        }
    }

    private void stubNotificationPrefsBasics(
            MockedStatic<RepositoryModule> repositoryModuleMockedStatic, boolean multiMode) {
        notificationPrefsMock = mock(NotificationPrefs.class);
        PreferenceBooleanValue multiModeMock = mock(PreferenceBooleanValue.class);
        when(multiModeMock.get()).thenReturn(multiMode);
        PreferenceBooleanValue notificationEnabledMock = mock(PreferenceBooleanValue.class);
        when(notificationEnabledMock.get()).thenReturn(true);
        PreferenceBooleanValue lightScreenMock = mock(PreferenceBooleanValue.class);
        when(lightScreenMock.get()).thenReturn(false);
        when(notificationPrefsMock.multiMode()).thenReturn(multiModeMock);
        when(notificationPrefsMock.notificationEnabled()).thenReturn(notificationEnabledMock);
        when(notificationPrefsMock.lightScreenOn()).thenReturn(lightScreenMock);
        repositoryModuleMockedStatic
                .when(RepositoryModule::getNotificationPreferences)
                .thenReturn(notificationPrefsMock);
    }
}
