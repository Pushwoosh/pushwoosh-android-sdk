package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.handlers.notification.PushStatNotificationOpenHandler;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;
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

import java.util.Arrays;
import java.util.List;

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

    @Mock
    private PushStatNotificationOpenHandler pushStatNotificationOpenHandler;

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
        WhiteboxHelper.setInternalState(
                notificationServiceExtension, "notificationOpenHandler", notificationOpenHandler);
        WhiteboxHelper.setInternalState(notificationServiceExtension, "pushMessageHandler", pushMessageHandler);
        WhiteboxHelper.setInternalState(
                notificationServiceExtension, "pushNotificationManager", pushNotificationManager);
        WhiteboxHelper.setInternalState(
                notificationServiceExtension, "pushStatNotificationOpenHandler", pushStatNotificationOpenHandler);
        Mockito.when(configMock.getSendPushStatIfShowForegroundDisabled()).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void handleMessageInternalShouldReturnIfPreHandleMessage() {
        Mockito.when((pushMessageHandler.preHandleMessage(pushBundle))).thenReturn(true);
        notificationServiceExtension.handleMessageInternal(pushBundle);
        verify(pushMessageFactory, never()).createPushMessage(pushBundle);
    }

    @Test
    public void handleMessageInternal_alwaysDelegatesHandlePushMessage_evenWhenHandled() {
        showForeground = true;
        PushMessage message = mock(PushMessage.class);
        Mockito.when(pushMessageFactory.createPushMessage(pushBundle)).thenReturn(message);

        notificationServiceExtension.handleMessageInternal(pushBundle);

        verify(pushMessageHandler).handlePushMessage(message, true);
    }

    @Test
    public void handleMessageInternal_sendsPushStat_whenHandledAndFlagEnabled() {
        showForeground = true;
        Mockito.when(configMock.getSendPushStatIfShowForegroundDisabled()).thenReturn(true);
        PushMessage message = mock(PushMessage.class);
        Mockito.when(pushMessageFactory.createPushMessage(pushBundle)).thenReturn(message);

        notificationServiceExtension.handleMessageInternal(pushBundle);

        verify(pushStatNotificationOpenHandler).postHandleNotification(pushBundle);
        verify(pushMessageHandler).handlePushMessage(message, true);
    }

    @Test
    public void handleMessageInternal_doesNotSendPushStat_whenNotHandled() {
        showForeground = false;
        Mockito.when(configMock.getSendPushStatIfShowForegroundDisabled()).thenReturn(true);
        PushMessage message = mock(PushMessage.class);
        Mockito.when(pushMessageFactory.createPushMessage(pushBundle)).thenReturn(message);

        notificationServiceExtension.handleMessageInternal(pushBundle);

        verify(pushStatNotificationOpenHandler, never()).postHandleNotification(any());
        verify(pushMessageHandler).handlePushMessage(message, false);
    }

    @Test
    public void handleNotification_callsPostHandleInFinally_evenWhenPreHandled() {
        Mockito.when(notificationOpenHandler.preHandleNotification(pushBundle)).thenReturn(true);

        notificationServiceExtension.handleNotification(pushBundle);

        verify(notificationOpenHandler).postHandleNotification(pushBundle);
    }

    @Test
    public void handleNotification_callsOnMessageOpenedInFinally() {
        Mockito.when(notificationOpenHandler.preHandleNotification(pushBundle)).thenReturn(false);
        NotificationServiceExtension sNotificationServiceExtension = spy(notificationServiceExtension);

        sNotificationServiceExtension.handleNotification(pushBundle);

        verify(sNotificationServiceExtension).onMessageOpened(any(PushMessage.class));
    }

    // Integration test for the main happy path: preHandle=false → both setLaunchNotification AND
    // startActivityForPushMessage fire. Collapses two pre-cleanup tests into one assertion of the
    // contract (without it, a refactor that swaps the gate from setLaunch or moves startActivity
    // outside the no-preHandle branch survives all existing tests).
    @Test
    public void handleNotification_preHandleFalse_setsLaunchAndStartsActivity() {
        Mockito.when(notificationOpenHandler.preHandleNotification(pushBundle)).thenReturn(false);
        NotificationServiceExtension sNotificationServiceExtension = spy(notificationServiceExtension);

        sNotificationServiceExtension.handleNotification(pushBundle);

        verify(pushNotificationManager).setLaunchNotification(any(PushMessage.class));
        verify(sNotificationServiceExtension).startActivityForPushMessage(any(PushMessage.class));
    }

    // 2x2 AND-truth-table corner (handled=true, flag=false). Existing tests cover (T,T) and (F,T).
    // Without this corner, mutating `&&` to `||` (or dropping the flag check) silently starts
    // sending push stats for users who opted out via config.
    @Test
    public void handleMessageInternal_handledFlagDisabled_doesNotSendPushStat() {
        showForeground = true;
        Mockito.when(configMock.getSendPushStatIfShowForegroundDisabled()).thenReturn(false);
        PushMessage message = mock(PushMessage.class);
        Mockito.when(pushMessageFactory.createPushMessage(pushBundle)).thenReturn(message);

        notificationServiceExtension.handleMessageInternal(pushBundle);

        verify(pushStatNotificationOpenHandler, never()).postHandleNotification(any());
        verify(pushMessageHandler).handlePushMessage(message, true);
    }

    @Test
    public void handleNotification_skipsPreHandle_whenPreHandleNotificationsWithUrlIsFalse() {
        NotificationServiceExtension noUrlExtension = new NotificationServiceExtensionTestable() {
            @Override
            protected boolean preHandleNotificationsWithUrl() {
                return false;
            }
        };
        WhiteboxHelper.setInternalState(noUrlExtension, "pushMessageFactory", pushMessageFactory);
        WhiteboxHelper.setInternalState(noUrlExtension, "applicationContext", context);
        WhiteboxHelper.setInternalState(noUrlExtension, "notificationOpenHandler", notificationOpenHandler);
        WhiteboxHelper.setInternalState(noUrlExtension, "pushMessageHandler", pushMessageHandler);
        WhiteboxHelper.setInternalState(noUrlExtension, "pushNotificationManager", pushNotificationManager);
        WhiteboxHelper.setInternalState(
                noUrlExtension, "pushStatNotificationOpenHandler", pushStatNotificationOpenHandler);
        Mockito.when(notificationOpenHandler.preHandleNotification(pushBundle)).thenReturn(true);

        noUrlExtension.handleNotification(pushBundle);

        verify(notificationOpenHandler, never()).preHandleNotification(any());
        verify(pushNotificationManager).setLaunchNotification(any());
    }

    @Test
    public void onMessagesGroupOpened_defaultHandlesLastMessage() {
        Bundle lastBundle = new Bundle();
        lastBundle.putString("p", "last");
        PushMessage first = new PushMessage(new Bundle());
        PushMessage last = new PushMessage(lastBundle);
        List<PushMessage> messages = Arrays.asList(first, last);
        NotificationServiceExtension sNotificationServiceExtension = spy(notificationServiceExtension);

        sNotificationServiceExtension.handleNotificationGroup(messages);

        ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
        verify(sNotificationServiceExtension).handleNotification(captor.capture());
        assertEquals("last", captor.getValue().getString("p"));
    }

    // Parameterized default-decision matrix for onMessageReceived, including prefs=null row.
    @Test
    public void onMessageReceived_defaultCombinesPrefsAlertAndForeground() {
        NotificationServiceExtension extension = new NotificationServiceExtension();
        PushMessage message = mock(PushMessage.class);

        // prefs=null row (absorbed from former defaultReturnsFalse_whenNotificationPrefsIsNull test)
        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class)) {
            repo.when(RepositoryModule::getNotificationPreferences).thenReturn(null);
            assertFalse("prefs=null must short-circuit to false", extension.onMessageReceived(message));
        }

        boolean[][] cases = {
            {true, true, true},
            {true, false, false},
            {false, true, false},
            {false, false, false},
        };

        for (boolean[] tc : cases) {
            boolean alert = tc[0];
            boolean foreground = tc[1];
            boolean expected = tc[2];
            String label = "alert=" + alert + ",foreground=" + foreground;

            NotificationPrefs prefs = mock(NotificationPrefs.class);
            PreferenceBooleanValue alertPref = mock(PreferenceBooleanValue.class);
            when(prefs.showPushnotificationAlert()).thenReturn(alertPref);
            when(alertPref.get()).thenReturn(alert);

            try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class);
                    MockedStatic<DeviceUtils> du = mockStatic(DeviceUtils.class)) {
                repo.when(RepositoryModule::getNotificationPreferences).thenReturn(prefs);
                du.when(DeviceUtils::isAppOnForeground).thenReturn(foreground);

                boolean actual = extension.onMessageReceived(message);

                assertEquals(label, expected, actual);
            }
        }
    }
}
