package com.pushwoosh.notification.handlers.notification;

import android.os.Bundle;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class PushStatNotificationOpenHandlerTest {

    private PushStatNotificationOpenHandler pushStatNotificationOpenHandler;
    private PlatformTestManager platformTestManager;
    
    @Mock
    private Bundle bundle;
    @Mock
    private PushwooshRepository pushwooshRepositoryMock;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        
        pushStatNotificationOpenHandler = new PushStatNotificationOpenHandler();
        
        // Mock PushwooshPlatform repository
        WhiteboxHelper.setInternalState(PushwooshPlatform.getInstance(), "pushwooshRepository", pushwooshRepositoryMock);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    /**
     * Test verifies that push statistics are not sent when handling a local push notification.
     * Local pushes are those that were created by the app itself, not received from Pushwoosh server.
     */
    @Test
    public void postHandleNotification_whenLocalPush_shouldNotSendPushStat() {
        // Given
        when(bundle.getBoolean("local", false)).thenReturn(true);
        when(bundle.getString("silent")).thenReturn("false");
        when(bundle.getString("p")).thenReturn("test_hash");
        when(bundle.getString("md")).thenReturn("test_metadata");
        
        // When
        pushStatNotificationOpenHandler.postHandleNotification(bundle);
        
        // Then
        verify(pushwooshRepositoryMock, never()).setCurrentSessionHash(anyString());
        verify(pushwooshRepositoryMock, never()).sendPushOpened(anyString(), anyString());
    }

    /**
     * Test verifies that push statistics are not sent when handling a silent push notification.
     * Silent pushes are those that should not be displayed to the user and are used for background data updates.
     */
    @Test
    public void postHandleNotification_whenSilentPush_shouldNotSendPushStat() {
        // Given
        when(bundle.getBoolean("local", false)).thenReturn(false);
        when(bundle.getString("silent")).thenReturn("true");
        when(bundle.getString("p")).thenReturn("test_hash");
        when(bundle.getString("md")).thenReturn("test_metadata");
        
        // When
        pushStatNotificationOpenHandler.postHandleNotification(bundle);
        
        // Then
        verify(pushwooshRepositoryMock, never()).setCurrentSessionHash(anyString());
        verify(pushwooshRepositoryMock, never()).sendPushOpened(anyString(), anyString());
    }

    /**
     * Test verifies that push statistics are sent when handling a regular push notification.
     * Regular pushes are those that are received from Pushwoosh server and should be displayed to the user.
     */
    @Test
    public void postHandleNotification_whenRegularPush_shouldSendPushStat() {
        // Given
        String pushHash = "test_push_hash";
        String metadata = "test_metadata";
        when(bundle.getBoolean("local", false)).thenReturn(false);
        when(bundle.getString("silent")).thenReturn("false");
        when(bundle.getString("p")).thenReturn(pushHash);
        when(bundle.getString("md")).thenReturn(metadata);
        
        // When
        pushStatNotificationOpenHandler.postHandleNotification(bundle);
        
        // Then
        verify(pushwooshRepositoryMock).setCurrentSessionHash(eq(pushHash));
        verify(pushwooshRepositoryMock).sendPushOpened(eq(pushHash), eq(metadata));
    }

    /**
     * Test verifies that push statistics are sent when handling a push notification with null silent flag.
     * This is a special case where the silent flag is not set, which should be treated as a regular push.
     */
    @Test
    public void postHandleNotification_whenNullSilentValue_shouldSendPushStat() {
        // Given
        String pushHash = "test_push_hash";
        String metadata = "test_metadata";
        when(bundle.getBoolean("local", false)).thenReturn(false);
        when(bundle.getString("silent")).thenReturn(null);
        when(bundle.getString("p")).thenReturn(pushHash);
        when(bundle.getString("md")).thenReturn(metadata);
        
        // When
        pushStatNotificationOpenHandler.postHandleNotification(bundle);
        
        // Then
        verify(pushwooshRepositoryMock).setCurrentSessionHash(eq(pushHash));
        verify(pushwooshRepositoryMock).sendPushOpened(eq(pushHash), eq(metadata));
    }

    /**
     * Test verifies that push statistics are sent when handling a push notification with null push hash.
     * Push hash is required for sending statistics, so if it's missing, no statistics should be sent.
     */
    @Test
    public void postHandleNotification_whenNullPushHash_shouldNotSendPushStat() {
        // Given
        when(bundle.getBoolean("local", false)).thenReturn(false);
        when(bundle.getString("silent")).thenReturn("false");
        when(bundle.getString("p")).thenReturn(null);
        when(bundle.getString("md")).thenReturn("test_metadata");
        
        // When
        pushStatNotificationOpenHandler.postHandleNotification(bundle);
        
        // Then
        verify(pushwooshRepositoryMock).setCurrentSessionHash(any());
        verify(pushwooshRepositoryMock).sendPushOpened(any(), eq("test_metadata"));
    }
}