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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.manager.ContextManagerProvider;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.preference.PreferenceArrayListValue;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushMessageTestTool;
import com.pushwoosh.notification.PushwooshNotificationFactory;
import com.pushwoosh.notification.SummaryNotificationUtils;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.Serializable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    public void tearDown() throws Exception {

    }

    @Test
    public void handleNotification() throws Exception {
        try (
                MockedStatic<NotificationUtils> notificationUtilsMockedStatic = Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<EventBus> eventBusMockedStatic = Mockito.mockStatic(EventBus.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryNotificationUtils = Mockito.mockStatic(SummaryNotificationUtils.class)
        ) {
            platformModuleMockedStatic.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);
            String groupId = "groupId";
            summaryNotificationUtils.when(() -> SummaryNotificationUtils.getNotificationIdForGroup(groupId)).thenReturn(1);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProviderMock);

            notificationPrefsMock = mock(NotificationPrefs.class);
            preferenceBooleanValueMock = mock(PreferenceBooleanValue.class);
            when(preferenceBooleanValueMock.get()).thenReturn(true);
            when(notificationPrefsMock.notificationEnabled()).thenReturn(preferenceBooleanValueMock);
            when(notificationPrefsMock.multiMode()).thenReturn(preferenceBooleanValueMock);
            repositoryModuleMockedStatic.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefsMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);


            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler,
                    "notificationFactory",
                    pushwooshNotificationFactoryMock);

            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic.when(RepositoryModule::getLocalNotificationStorage).thenReturn(localNotificationStorageMock);

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

            notificationUtilsMockedStatic.verify(NotificationUtils::turnScreenOn,times(1));
            EventBus.sendEvent(Mockito.any());
            verify(notificationPrefsMock).pushHistory();
            verify(history).add(Mockito.any(Serializable.class));
            verify(localNotificationStorageMock).removeLocalNotificationShown(Mockito.anyInt(), Mockito.anyString());
        }




    }

    @Test
    public void handlePushMessage() throws Exception {
        try (
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryNotificationUtils = Mockito.mockStatic(SummaryNotificationUtils.class)
                ){
            platformModuleMockedStatic.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);
            String groupId = "groupId";
            summaryNotificationUtils.when(() -> SummaryNotificationUtils.getNotificationIdForGroup(groupId)).thenReturn(1);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProviderMock);

            notificationPrefsMock = mock(NotificationPrefs.class);
            preferenceBooleanValueMock = mock(PreferenceBooleanValue.class);
            when(preferenceBooleanValueMock.get()).thenReturn(true);
            when(notificationPrefsMock.notificationEnabled()).thenReturn(preferenceBooleanValueMock);
            when(notificationPrefsMock.multiMode()).thenReturn(preferenceBooleanValueMock);
            repositoryModuleMockedStatic.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefsMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);


            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler,
                    "notificationFactory",
                    pushwooshNotificationFactoryMock);

            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic.when(RepositoryModule::getLocalNotificationStorage).thenReturn(localNotificationStorageMock);

            PreferenceBooleanValue lightScreenMock = mock(PreferenceBooleanValue.class);
            when(lightScreenMock.get()).thenReturn(true);
            when(notificationPrefsMock.lightScreenOn()).thenReturn(lightScreenMock);

            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);

            when(preferenceBooleanValueMock.get()).thenReturn(true);
            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            showNotificationMessageHandler.handlePushMessage(pushMessage);

            verify(pushwooshNotificationFactoryMock)
                    .onGenerateNotification(pushMessage);
        }

    }

    @Test
    public void handlePushMessageNegative() throws Exception {
        try (
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryNotificationUtils = Mockito.mockStatic(SummaryNotificationUtils.class)
                ){
            platformModuleMockedStatic.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);
            String groupId = "groupId";
            summaryNotificationUtils.when(() -> SummaryNotificationUtils.getNotificationIdForGroup(groupId)).thenReturn(1);

            ManagerProvider managerProviderMock = mock(ManagerProvider.class);
            NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
            when(managerProviderMock.getNotificationManager()).thenReturn(notificationManager);
            platformModuleMockedStatic.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProviderMock);

            notificationPrefsMock = mock(NotificationPrefs.class);
            preferenceBooleanValueMock = mock(PreferenceBooleanValue.class);
            when(preferenceBooleanValueMock.get()).thenReturn(true);
            when(notificationPrefsMock.notificationEnabled()).thenReturn(preferenceBooleanValueMock);
            when(notificationPrefsMock.multiMode()).thenReturn(preferenceBooleanValueMock);
            repositoryModuleMockedStatic.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefsMock);

            showNotificationMessageHandler = new ShowNotificationMessageHandler();
            pushwooshNotificationFactoryMock = mock(PushwooshNotificationFactory.class);


            WhiteboxHelper.setInternalState(
                    showNotificationMessageHandler,
                    "notificationFactory",
                    pushwooshNotificationFactoryMock);

            LocalNotificationStorage localNotificationStorageMock = mock(LocalNotificationStorage.class);
            repositoryModuleMockedStatic.when(RepositoryModule::getLocalNotificationStorage).thenReturn(localNotificationStorageMock);

            PreferenceBooleanValue lightScreenMock = mock(PreferenceBooleanValue.class);
            when(lightScreenMock.get()).thenReturn(true);
            when(notificationPrefsMock.lightScreenOn()).thenReturn(lightScreenMock);

            PreferenceArrayListValue history = mock(PreferenceArrayListValue.class);
            when(notificationPrefsMock.pushHistory()).thenReturn(history);

            when(preferenceBooleanValueMock.get()).thenReturn(false);
            PushMessage pushMessage = pushMessageTestTool.getPushMessageMock(false);
            showNotificationMessageHandler.handlePushMessage(pushMessage);

            verify(pushwooshNotificationFactoryMock, Mockito.never())
                    .onGenerateNotification(pushMessage);
        }
    }
}