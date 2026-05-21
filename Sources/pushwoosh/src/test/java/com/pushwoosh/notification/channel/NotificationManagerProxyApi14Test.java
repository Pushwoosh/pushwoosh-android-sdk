/*
 *
 * Copyright (c) 2024. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.notification.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.media.AudioManager;
import android.net.Uri;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.notification.VibrateType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class NotificationManagerProxyApi14Test {

    @Mock
    private AudioManager audioManager;

    @Mock
    private ManagerProvider managerProvider;

    private AutoCloseable mocks;
    private NotificationManagerProxyApi14 proxy;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        proxy = new NotificationManagerProxyApi14();
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // Verifies that addSound with isDefault=true assigns the custom Uri and OR's DEFAULT_SOUND
    // into notification.defaults without overwriting unrelated bits.
    @Test
    public void addSound_isDefaultTrue_setsCustomUriAndOrsDefaultSoundFlag() {
        Notification notification = new Notification();
        notification.defaults = Notification.DEFAULT_LIGHTS;
        Uri customSound = mock(Uri.class);

        proxy.addSound(notification, customSound, true);

        assertSame(customSound, notification.sound);
        assertNotEquals(0, notification.defaults & Notification.DEFAULT_SOUND);
        assertNotEquals(0, notification.defaults & Notification.DEFAULT_LIGHTS);
    }

    // Verifies that addSound with isDefault=false assigns the custom Uri but does not touch defaults.
    @Test
    public void addSound_isDefaultFalse_setsCustomUriAndLeavesDefaultsUntouched() {
        Notification notification = new Notification();
        notification.defaults = 0;
        Uri customSound = mock(Uri.class);

        proxy.addSound(notification, customSound, false);

        assertSame(customSound, notification.sound);
        assertEquals(0, notification.defaults);
    }

    // Verifies that addSound with null customSound and isDefault=true sets sound to null and still
    // OR's DEFAULT_SOUND so the system falls back to default sound.
    @Test
    public void addSound_nullSoundAndIsDefaultTrue_assignsNullSoundAndKeepsDefaultSoundFlag() {
        Notification notification = new Notification();
        notification.defaults = 0;

        proxy.addSound(notification, null, true);

        assertNull(notification.sound);
        assertNotEquals(0, notification.defaults & Notification.DEFAULT_SOUND);
    }

    // Verifies that addVibration returns without touching the notification when AudioManager is unavailable.
    @Test
    public void addVibration_audioManagerNull_doesNothing() {
        Notification notification = new Notification();
        notification.defaults = 0;

        try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {
            when(managerProvider.getAudioManager()).thenReturn(null);
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);

            proxy.addVibration(notification, VibrateType.ALWAYS, true);
        }

        assertEquals(0, notification.defaults);
    }

    // Verifies that vibration=true with permission OR's DEFAULT_VIBRATE even when VibrateType is NO_VIBRATE
    // (explicit vibration flag overrides the type setting).
    @Test
    public void addVibration_vibrationTrueAndPermission_overridesNoVibrate() {
        Notification notification = new Notification();
        notification.defaults = 0;
        when(audioManager.getRingerMode()).thenReturn(AudioManager.RINGER_MODE_NORMAL);
        when(managerProvider.getAudioManager()).thenReturn(audioManager);

        try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<NotificationUtils> utilsMock = Mockito.mockStatic(NotificationUtils.class)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            utilsMock.when(NotificationUtils::phoneHaveVibratePermission).thenReturn(true);

            proxy.addVibration(notification, VibrateType.NO_VIBRATE, true);
        }

        assertNotEquals(0, notification.defaults & Notification.DEFAULT_VIBRATE);
    }

    // Verifies that VibrateType.ALWAYS with permission OR's DEFAULT_VIBRATE regardless of ringer mode.
    @Test
    public void addVibration_vibrateTypeAlwaysWithPermission_setsDefaultVibrateInSilentMode() {
        Notification notification = new Notification();
        notification.defaults = 0;
        when(audioManager.getRingerMode()).thenReturn(AudioManager.RINGER_MODE_SILENT);
        when(managerProvider.getAudioManager()).thenReturn(audioManager);

        try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<NotificationUtils> utilsMock = Mockito.mockStatic(NotificationUtils.class)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            utilsMock.when(NotificationUtils::phoneHaveVibratePermission).thenReturn(true);

            proxy.addVibration(notification, VibrateType.ALWAYS, false);
        }

        assertNotEquals(0, notification.defaults & Notification.DEFAULT_VIBRATE);
    }

    // Verifies that DEFAULT_MODE with RINGER_MODE_VIBRATE and permission OR's DEFAULT_VIBRATE.
    @Test
    public void addVibration_defaultModeWithRingerVibrateAndPermission_setsDefaultVibrate() {
        Notification notification = new Notification();
        notification.defaults = 0;
        when(audioManager.getRingerMode()).thenReturn(AudioManager.RINGER_MODE_VIBRATE);
        when(managerProvider.getAudioManager()).thenReturn(audioManager);

        try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<NotificationUtils> utilsMock = Mockito.mockStatic(NotificationUtils.class)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            utilsMock.when(NotificationUtils::phoneHaveVibratePermission).thenReturn(true);

            proxy.addVibration(notification, VibrateType.DEFAULT_MODE, false);
        }

        assertNotEquals(0, notification.defaults & Notification.DEFAULT_VIBRATE);
    }

    // Verifies that DEFAULT_MODE with RINGER_MODE_NORMAL and vibration=false leaves defaults untouched.
    @Test
    public void addVibration_defaultModeWithRingerNormalAndNoVibration_leavesDefaultsUntouched() {
        Notification notification = new Notification();
        notification.defaults = 0;
        when(audioManager.getRingerMode()).thenReturn(AudioManager.RINGER_MODE_NORMAL);
        when(managerProvider.getAudioManager()).thenReturn(audioManager);

        try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<NotificationUtils> utilsMock = Mockito.mockStatic(NotificationUtils.class)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            utilsMock.when(NotificationUtils::phoneHaveVibratePermission).thenReturn(true);

            proxy.addVibration(notification, VibrateType.DEFAULT_MODE, false);
        }

        assertEquals(0, notification.defaults);
    }

    // Verifies that NO_VIBRATE with RINGER_MODE_VIBRATE and vibration=false leaves defaults untouched.
    @Test
    public void addVibration_noVibrateWithRingerVibrateAndNoVibration_leavesDefaultsUntouched() {
        Notification notification = new Notification();
        notification.defaults = 0;
        when(audioManager.getRingerMode()).thenReturn(AudioManager.RINGER_MODE_VIBRATE);
        when(managerProvider.getAudioManager()).thenReturn(audioManager);

        try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<NotificationUtils> utilsMock = Mockito.mockStatic(NotificationUtils.class)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            utilsMock.when(NotificationUtils::phoneHaveVibratePermission).thenReturn(true);

            proxy.addVibration(notification, VibrateType.NO_VIBRATE, false);
        }

        assertEquals(0, notification.defaults);
    }

    // Verifies that the VIBRATE permission gate prevents the DEFAULT_VIBRATE flag even when other
    // conditions would enable it.
    @Test
    public void addVibration_noPermission_doesNotSetDefaultVibrate() {
        Notification notification = new Notification();
        notification.defaults = 0;
        when(audioManager.getRingerMode()).thenReturn(AudioManager.RINGER_MODE_VIBRATE);
        when(managerProvider.getAudioManager()).thenReturn(audioManager);

        try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<NotificationUtils> utilsMock = Mockito.mockStatic(NotificationUtils.class)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            utilsMock.when(NotificationUtils::phoneHaveVibratePermission).thenReturn(false);

            proxy.addVibration(notification, VibrateType.ALWAYS, true);
        }

        assertEquals(0, notification.defaults);
    }
}
