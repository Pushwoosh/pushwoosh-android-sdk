package com.pushwoosh.notification;

import android.content.Context;
import android.os.Bundle;

import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class NotificationServiceExtensionTest {
    private Bundle pushBundle;
    private NotificationServiceExtension notificationServiceExtension;

    private boolean showForeground;

    @Mock
    private Context context;
    @Mock
    private NotificationOpenHandler notificationOpenHandler;
    @Mock
    private PushMessageHandler pushMessageHandler;
    @Mock
    private PushMessageFactory pushMessageFactory;
    @Mock
    private PushwooshNotificationManager pushNotificationManager;

    private Config configMock;

    private PlatformTestManager platformTestManager;

    public class NotificationServiceExtensionTestable extends NotificationServiceExtension {

        @Override
        protected boolean onMessageReceived(PushMessage data) {
            return showForeground;
        }
    }

    @Before
    public void setUp() throws Exception {
        configMock = MockConfig.createMock();
        platformTestManager = new PlatformTestManager(configMock);
        MockitoAnnotations.initMocks(this);
        pushBundle = new Bundle();
        notificationServiceExtension = new NotificationServiceExtensionTestable();
        WhiteboxHelper.setInternalState(notificationServiceExtension, "pushMessageFactory", pushMessageFactory);
        WhiteboxHelper.setInternalState(notificationServiceExtension, "applicationContext", context);
        WhiteboxHelper.setInternalState(notificationServiceExtension, "notificationOpenHandler", notificationOpenHandler);
        WhiteboxHelper.setInternalState(notificationServiceExtension, "pushMessageHandler", pushMessageHandler);
        WhiteboxHelper.setInternalState(notificationServiceExtension,"pushNotificationManager", pushNotificationManager);
        Mockito.when(configMock.getSendPushStatIfShowForegroundDisabled()).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void handleMessageInternalShouldNotSendStatIfFlagDisabledAndForegroundEnabled(){
        showForeground = true;
        Mockito.when(configMock.getSendPushStatIfShowForegroundDisabled()).thenReturn(false);

        notificationServiceExtension.handleMessageInternal(pushBundle);

        verify(notificationOpenHandler, never()).postHandleNotification(pushBundle);
    }

    @Test
    public void  handleMessageInternalShouldReturnIfEmptyMessage(){
        pushBundle = null;
        notificationServiceExtension.handleMessageInternal(pushBundle);
        verify(pushMessageFactory, never()).createPushMessage(pushBundle);
    }

    @Test
    public void handleMessageInternalShouldReturnIfPreHandleMessage(){
        Mockito.when((pushMessageHandler.preHandleMessage(pushBundle))).thenReturn(true);
        notificationServiceExtension.handleMessageInternal(pushBundle);
        verify(pushMessageFactory, never()).createPushMessage(pushBundle);
    }

    @Test
    public void handleMessageInternalShouldCreatePushMessageIfNotEmptyNotPreHandled(){
        Mockito.when((pushMessageHandler.preHandleMessage(pushBundle))).thenReturn(false);
        notificationServiceExtension.handleMessageInternal(pushBundle);
        verify(pushMessageFactory).createPushMessage(pushBundle);
    }

    @Test
    public void handleMessageShouldCallHandleMessageInternal(){
        NotificationServiceExtension sNotificationServiceExtension = spy(notificationServiceExtension);
        sNotificationServiceExtension.handleMessage(pushBundle);
        verify(sNotificationServiceExtension).handleMessageInternal(pushBundle);
    }

    @Test
    public void handleNotificationShouldReturnIfEmptyBundle(){
        pushBundle = null;
        notificationServiceExtension.handleNotification(pushBundle);
        verify(notificationOpenHandler, never()).postHandleNotification(pushBundle);
    }

    @Test
    public void handleNotificationShouldNotSetLaunchNotificationIfPreHandleMessage(){
        Mockito.when((notificationOpenHandler.preHandleNotification(pushBundle))).thenReturn(true);
        notificationServiceExtension.handleNotification(pushBundle);
        verify(pushNotificationManager, never()).setLaunchNotification(any());
    }

    @Test
    public void handleNotificationShouldSetLaunchNotificationIfNotPreHandledMessage(){
        Mockito.when((notificationOpenHandler.preHandleNotification(pushBundle))).thenReturn(false);
        notificationServiceExtension.handleNotification(pushBundle);
        verify(pushNotificationManager).setLaunchNotification(any());
    }

    @Test
    public void handleNotificationShouldNotStartActivityIfPreHandleMessage(){
        Mockito.when((notificationOpenHandler.preHandleNotification(pushBundle))).thenReturn(true);
        NotificationServiceExtension sNotificationServiceExtension = spy(notificationServiceExtension);
        sNotificationServiceExtension.handleNotification(pushBundle);
        verify(sNotificationServiceExtension, never()).startActivityForPushMessage(any());
    }

    @Test
    public void handleNotificationShouldStartActivityIfNotPreHandledMessage(){
        Mockito.when((notificationOpenHandler.preHandleNotification(pushBundle))).thenReturn(false);
        NotificationServiceExtension sNotificationServiceExtension = spy(notificationServiceExtension);
        sNotificationServiceExtension.handleNotification(pushBundle);
        verify(sNotificationServiceExtension).startActivityForPushMessage(any());
    }

}