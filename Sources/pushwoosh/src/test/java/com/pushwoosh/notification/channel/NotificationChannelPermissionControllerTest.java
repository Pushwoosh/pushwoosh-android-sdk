package com.pushwoosh.notification.channel;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.net.Uri;
import android.os.Build;

import com.pushwoosh.PermissionController;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import static junit.framework.TestCase.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = Build.VERSION_CODES.O)
public class NotificationChannelPermissionControllerTest  {
    private PreferenceStringValue testChannelNamePref;
    private NotificationPrefs testPrefs;
    private NotificationChannel testChannel;
    private NotificationManager notificationManager;

    @Before
    public void setup() {
        testChannelNamePref = mock(PreferenceStringValue.class);
        when(testChannelNamePref.get()).thenReturn("test channel");
        testPrefs = mock(NotificationPrefs.class);
        when(testPrefs.channelName()).thenReturn(testChannelNamePref);
        RepositoryModule.setNotificationPreferences(testPrefs);
    }

    @Test
    public void testImportanceNone() {
        testChannel = mock(NotificationChannel.class);
        when(testChannel.getImportance()).thenReturn(NotificationManager.IMPORTANCE_NONE);
        notificationManager = mock(NotificationManager.class);
        when(notificationManager.getNotificationChannel(anyString())).thenReturn(testChannel);

        int bitmask = NotificationChannelPermissionController.getBitMask(notificationManager);
        assertEquals(bitmask, PermissionController.DISABLE_ALL);
    }

    @Test
    public void testImportanceLow() {
        testChannel = mock(NotificationChannel.class);
        when(testChannel.getImportance()).thenReturn(NotificationManager.IMPORTANCE_LOW);
        notificationManager = mock(NotificationManager.class);
        when(notificationManager.getNotificationChannel(anyString())).thenReturn(testChannel);

        int bitmask = NotificationChannelPermissionController.getBitMask(notificationManager);
        assertEquals(bitmask, PermissionController.ENABLE_ALERT);
    }

    @Test
    public void testImportanceHighNoSound() {
        testChannel = mock(NotificationChannel.class);
        when(testChannel.getImportance()).thenReturn(NotificationManager.IMPORTANCE_HIGH);
        when(testChannel.getSound()).thenReturn(null);
        notificationManager = mock(NotificationManager.class);
        when(notificationManager.getNotificationChannel(anyString())).thenReturn(testChannel);

        int bitmask = NotificationChannelPermissionController.getBitMask(notificationManager);
        assertEquals(bitmask, PermissionController.ENABLE_ALERT);
    }

    @Test
    public void testImportanceHighWithSound() {
        testChannel = mock(NotificationChannel.class);
        when(testChannel.getImportance()).thenReturn(NotificationManager.IMPORTANCE_HIGH);
        Uri uri = mock(Uri.class);
        when(testChannel.getSound()).thenReturn(uri);
        notificationManager = mock(NotificationManager.class);
        when(notificationManager.getNotificationChannel(anyString())).thenReturn(testChannel);

        int bitmask = NotificationChannelPermissionController.getBitMask(notificationManager);
        assertEquals(bitmask, PermissionController.ENABLE_ALERT_AND_SOUND);
    }

    @Test
    public void testChannelIsNull() {
        notificationManager = mock(NotificationManager.class);
        when(notificationManager.getNotificationChannel(anyString())).thenReturn(null);

        int bitmask = NotificationChannelPermissionController.getBitMask(notificationManager);
        assertEquals(bitmask, PermissionController.ENABLE_ALERT_AND_SOUND);
    }
}
