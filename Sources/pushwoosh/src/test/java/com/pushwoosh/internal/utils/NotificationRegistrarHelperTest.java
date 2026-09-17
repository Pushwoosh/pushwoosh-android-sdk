package com.pushwoosh.internal.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class NotificationRegistrarHelperTest {
    private static final String TOKEN = "transport-token";

    private PlatformTestManager platformTestManager;
    private RegistrationPrefs registrationPrefs;
    private AutoCloseable mocks;

    @Mock
    private PushwooshPlatform pushwooshPlatformMock;

    @Mock
    private PushwooshNotificationManager notificationManagerMock;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        platformTestManager = new PlatformTestManager(MockConfig.createMock());
        platformTestManager.setUp();
        registrationPrefs = platformTestManager.getRegistrationPrefs();
        when(pushwooshPlatformMock.notificationManager()).thenReturn(notificationManagerMock);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
        mocks.close();
    }

    // Spec test 7 (guard closed): token from a transport is dropped while the SDK does not manage the token.
    @Test
    public void tokenIgnoredWhenNotRegisteredForPush() {
        registrationPrefs.isRegisteredForPush().set(false);

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            NotificationRegistrarHelper.onRegisteredForRemoteNotifications(TOKEN, null);

            verify(notificationManagerMock, never()).onTokenReceived(anyString(), any(), anyBoolean());
        }
    }

    // Spec test 7 (guard open): token forwarded to the manager without retries.
    @Test
    public void tokenForwardedWithoutRetriesWhenRegisteredForPush() {
        registrationPrefs.isRegisteredForPush().set(true);

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            NotificationRegistrarHelper.onRegisteredForRemoteNotifications(TOKEN, "{}");

            verify(notificationManagerMock).onTokenReceived(TOKEN, "{}", false);
        }
    }
}
