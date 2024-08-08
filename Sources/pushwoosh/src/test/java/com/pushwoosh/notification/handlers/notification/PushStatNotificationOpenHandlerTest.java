package com.pushwoosh.notification.handlers.notification;

import android.os.Bundle;

import com.pushwoosh.internal.command.CommandApplayer;
import com.pushwoosh.notification.NotificationServiceExtension;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class PushStatNotificationOpenHandlerTest {

    private PushStatNotificationOpenHandler pushStatNotificationOpenHandler;
    @Mock
    private Bundle bundle;
    @Mock
    private CommandApplayer commandApplayer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        pushStatNotificationOpenHandler = new PushStatNotificationOpenHandler(commandApplayer);
    }

    @Test
    public void postHandleNotificationIfLocalShouldNotSendPushStat() {
        when(bundle.getBoolean("local", false)).thenReturn(true);
        when(bundle.getString("silent")).thenReturn("false");
        pushStatNotificationOpenHandler.postHandleNotification(bundle);
        verify(commandApplayer, never()).applyCommand(any(), any());
    }

    @Test
    public void postHandleNotificationIfNotLocalShouldSend() {
        when(bundle.getBoolean("local", false)).thenReturn(false);
        when(bundle.getString("silent")).thenReturn("false");
        pushStatNotificationOpenHandler.postHandleNotification(bundle);
        verify(commandApplayer).applyCommand(any(), any());
    }

    @Test
    public void postHandleNotificationIfSilentShouldNotSendPushStat() {
        when(bundle.getBoolean("local", false)).thenReturn(false);
        when(bundle.getString("silent")).thenReturn("true");
        pushStatNotificationOpenHandler.postHandleNotification(bundle);
        verify(commandApplayer, never()).applyCommand(any(), any());
    }

}