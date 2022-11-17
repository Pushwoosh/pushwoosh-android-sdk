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

package com.pushwoosh.notification;

import android.app.Notification;
import android.net.Uri;

import com.pushwoosh.BuildConfig;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.internal.preference.PreferenceVibrateTypeValue;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.notification.builder.NotificationBuilder;
import com.pushwoosh.notification.builder.NotificationBuilderManager;
import com.pushwoosh.notification.channel.NotificationChannelManager;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Created by aevstefeev on 12/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@Config(constants = BuildConfig.class, sdk = 25)
@Ignore
@PrepareForTest({RepositoryModule.class, NotificationUtils.class, NotificationBuilderManager.class})
public class PushwooshNotificationFactoryTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void onGenerateNotification() throws Exception {
        String sound = "sound.mp3";
        PowerMockito.mockStatic(NotificationUtils.class);
        PowerMockito.mockStatic(RepositoryModule.class);

        NotificationBuilder builder= NotificationBuilderManager.createNotificationBuilder(RuntimeEnvironment.application, null);
        PowerMockito.mockStatic(NotificationBuilderManager.class);
        PowerMockito.when(NotificationBuilderManager.createNotificationBuilder(RuntimeEnvironment.application, null)).thenReturn(builder);


        Uri soundUri = Uri.fromFile(new File(sound));
        PowerMockito.when(NotificationUtils.getSoundUri(sound)).thenReturn(soundUri);
        NotificationPrefs notificationPrefs = mock(NotificationPrefs.class);
        PreferenceBooleanValue ledEnable = mock(PreferenceBooleanValue.class);
        when(ledEnable.get()).thenReturn(true);
        PreferenceIntValue ledColor = mock(PreferenceIntValue.class);
        when(ledColor.get()).thenReturn(10);
        when(notificationPrefs.ledColor()).thenReturn(ledColor);
        when(notificationPrefs.ledEnabled()).thenReturn(ledEnable);
        PreferenceVibrateTypeValue preferenceVibrateTypeValue = mock(PreferenceVibrateTypeValue.class);
        when(preferenceVibrateTypeValue.get()).thenReturn(VibrateType.fromInt(0));
        when(notificationPrefs.vibrateType()).thenReturn(preferenceVibrateTypeValue);
        PowerMockito.when(RepositoryModule.getNotificationPreferences()).thenReturn(notificationPrefs);

        AndroidPlatformModule.init(RuntimeEnvironment.application, true);

        PushwooshNotificationFactory pushwooshNotificationFactory = new PushwooshNotificationFactory();
        NotificationChannelManager notificationChannelManager = mock(NotificationChannelManager.class);
        Whitebox.setInternalState(pushwooshNotificationFactory, "notificationChannelManager", notificationChannelManager);


        PushMessage pushMessage = new PushMessageTestTool().getPushMessageMock(true);

        List<Action> actionList = new ArrayList<>();
        actionList.add(mock(Action.class));
        actionList.add(mock(Action.class));
        actionList.add(mock(Action.class));
        when(pushMessage.getActions()).thenReturn(actionList);

        Notification notification = pushwooshNotificationFactory.onGenerateNotification(pushMessage);

        assertEquals("ticker_text", notification.tickerText.toString());
        PowerMockito.verifyStatic();
        NotificationUtils.tryToGetBitmapFromInternet("lagre_icon", 64);
        PowerMockito.verifyStatic();
        NotificationUtils.tryToGetBitmapFromInternet("url_picture", -1);
        PowerMockito.verifyStatic();
        NotificationUtils.getSoundUri("sound.mp3");
        PowerMockito.verifyStatic(Mockito.times(3));
        NotificationBuilderManager.addAction(eq(RuntimeEnvironment.application), eq(builder), Mockito.any(Action.class));

        verify(notificationChannelManager).addChannel(pushMessage, "channel", "description");
        verify(notificationChannelManager).addSound(notification, soundUri, false);
        verify(notificationChannelManager).addLED(notification, pushMessage.getLed(), pushMessage.getLedOnMS(), pushMessage.getLedOffMS());
        verify(notificationChannelManager).addVibration(notification, VibrateType.fromInt(0),pushMessage.getVibration());
    }
}