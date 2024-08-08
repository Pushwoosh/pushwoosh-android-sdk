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
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.resource.ContextResourceProvider;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.internal.preference.PreferenceVibrateTypeValue;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.notification.builder.AppIconHelper;
import com.pushwoosh.notification.builder.NotificationBuilder;
import com.pushwoosh.notification.builder.NotificationBuilderManager;
import com.pushwoosh.notification.channel.NotificationChannelInfoProvider;
import com.pushwoosh.notification.channel.NotificationChannelManager;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.Nullable;

/**
 * Created by aevstefeev on 12/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml", sdk = 26)
public class PushwooshNotificationFactoryTest {

    @Test
    public void onGenerateNotification() throws Exception {
        try (
                MockedStatic<AndroidPlatformModule> androidPlatformModuleMockedStatic = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<NotificationUtils> notificationUtilsMockedStatic = Mockito.mockStatic(NotificationUtils.class);
                MockedStatic<RepositoryModule> repositoryModuleMockedStatic = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<NotificationBuilderManager> builderManagerMockedStatic = Mockito.mockStatic(NotificationBuilderManager.class);
                MockedStatic<NotificationChannelInfoProvider> channelInfoProviderMockedStatic = Mockito.mockStatic(NotificationChannelInfoProvider.class)
        ) {
            String sound = "sound.mp3";

            androidPlatformModuleMockedStatic.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);
            androidPlatformModuleMockedStatic.when(AndroidPlatformModule::getResourceProvider).thenReturn(new ContextResourceProvider(RuntimeEnvironment.application));

            NotificationBuilder notificationBuilder = getNotificationBuilder();
            builderManagerMockedStatic.when(()-> NotificationBuilderManager.createNotificationBuilder(RuntimeEnvironment.getApplication().getApplicationContext(), null))
                    .thenReturn(notificationBuilder);

            Uri soundUri = Uri.fromFile(new File(sound));
            notificationUtilsMockedStatic.when(() -> NotificationUtils.getSoundUri(sound)).thenReturn(soundUri);

            NotificationPrefs notificationPrefsMock = mock(NotificationPrefs.class);

            PreferenceBooleanValue ledEnable = mock(PreferenceBooleanValue.class);
            when(ledEnable.get()).thenReturn(true);

            PreferenceIntValue ledColor = mock(PreferenceIntValue.class);
            when(ledColor.get()).thenReturn(10);

            when(notificationPrefsMock.ledColor()).thenReturn(ledColor);
            when(notificationPrefsMock.ledEnabled()).thenReturn(ledEnable);

            PreferenceVibrateTypeValue preferenceVibrateTypeValue = mock(PreferenceVibrateTypeValue.class);
            when(preferenceVibrateTypeValue.get()).thenReturn(VibrateType.fromInt(0));
            when(notificationPrefsMock.vibrateType()).thenReturn(preferenceVibrateTypeValue);

            repositoryModuleMockedStatic.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefsMock);

            AndroidPlatformModule.init(RuntimeEnvironment.application, true);

            PushwooshNotificationFactory pushwooshNotificationFactory = new PushwooshNotificationFactory();
            NotificationChannelManager notificationChannelManagerMock = mock(NotificationChannelManager.class);
            WhiteboxHelper.setInternalState(pushwooshNotificationFactory, "notificationChannelManager", notificationChannelManagerMock);

            PushMessage pushMessage = new PushMessageTestTool().getPushMessageMock(true);

            when(pushMessage.toJson()).thenReturn(new JSONObject());

            List<Action> actionList = new ArrayList<>();
            actionList.add(mock(Action.class));
            actionList.add(mock(Action.class));
            actionList.add(mock(Action.class));
            when(pushMessage.getActions()).thenReturn(actionList);

            channelInfoProviderMockedStatic.when(() -> NotificationChannelInfoProvider.getChannelName(pushMessage)).thenReturn("channel");


            Notification notification = pushwooshNotificationFactory.onGenerateNotification(pushMessage);

            assertEquals("ticker_text", notification.tickerText.toString());

            NotificationUtils.tryToGetBitmapFromInternet("lagre_icon", 64);
            notificationUtilsMockedStatic.verify(()-> NotificationUtils.tryToGetBitmapFromInternet("lagre_icon", 64), times(1));
            notificationUtilsMockedStatic.verify(()-> NotificationUtils.tryToGetBitmapFromInternet("url_picture", -1), times(1));
            notificationUtilsMockedStatic.verify(()-> NotificationUtils.getSoundUri("sound.mp3"), times(1));

            verify(notificationChannelManagerMock).addChannel(pushMessage, "channel", "");
            verify(notificationChannelManagerMock).addSound(notification, soundUri, false);
            verify(notificationChannelManagerMock).addLED(notification, pushMessage.getLed(), pushMessage.getLedOnMS(), pushMessage.getLedOffMS());
            verify(notificationChannelManagerMock).addVibration(notification, VibrateType.fromInt(0),pushMessage.getVibration());
        }
    }

    private NotificationBuilder getNotificationBuilder() {
        return new NotificationBuilder() {
            public final Notification.Builder builder = new Notification.Builder(RuntimeEnvironment.getApplication().getApplicationContext(), "channel");
            @Override
            public NotificationBuilder setContentTitle(CharSequence title) {
                builder.setContentTitle(title);
                return this;
            }

            @Override
            public NotificationBuilder setContentText(CharSequence text) {
                builder.setContentText(text);
                return this;
            }

            @Override
            public NotificationBuilder setSmallIcon(int smallIcon) {
                builder.setSmallIcon(smallIcon);
                if (smallIcon == -1) {
                    builder.setSmallIcon(AppIconHelper.getAppIcon(
                            AndroidPlatformModule.getApplicationContext(),
                            AndroidPlatformModule.getAppInfoProvider().getPackageName())
                    );
                }
                return this;
            }

            @Override
            public NotificationBuilder setTicker(CharSequence ticker) {
                builder.setTicker(ticker);
                return this;
            }

            @Override
            public NotificationBuilder setWhen(long time) {
                builder.setWhen(time);
                builder.setShowWhen(true);
                return this;
            }

            @Override
            public NotificationBuilder setStyle(@Nullable Bitmap bigPicture, CharSequence text) {
                final Notification.Style style;

                if (bigPicture != null) {
                    //Images should be ? 450dp wide, ~2:1 aspect (see slide 52)
                    //The image will be centerCropped
                    //here: http://commondatastorage.googleapis.com/io2012/presentations/live%20to%20website/105.pdf
                    style = new Notification.BigPictureStyle()
                            .bigPicture(bigPicture)
                            .setSummaryText(text);
                } else {
                    style = new Notification.BigTextStyle()
                            .bigText(text);
                }

                builder.setStyle(style);
                return this;
            }

            @Override
            public NotificationBuilder setColor(Integer iconBackgroundColor) {
                if (iconBackgroundColor != null) {
                    builder.setColor(iconBackgroundColor);
                }
                return this;
            }

            @Override
            public NotificationBuilder setLargeIcon(Bitmap largeIcon) {
                if (null != largeIcon) {
                    builder.setLargeIcon(largeIcon);
                }
                return this;
            }

            @Override
            public NotificationBuilder setPriority(int priority) {
                builder.setPriority(priority);
                return this;
            }

            @Override
            public NotificationBuilder setVisibility(int visibility) {
                builder.setVisibility(visibility);
                return this;
            }

            @Override
            public NotificationBuilder addAction(int icon, CharSequence title, PendingIntent intent) {
                builder.addAction(new Notification.Action(icon, title, intent));
                return this;
            }

            @Override
            public NotificationBuilder setLed(int arg, int ledOnMs, int ledOffMs) {
                return this;
            }

            @Override
            public NotificationBuilder setExtras(Bundle extras) {
                builder.setExtras(extras);
                return this;
            }

            @Override
            public NotificationBuilder setGroup(String groupId) {
                builder.setGroup(groupId);
                return this;
            }

            @Override
            public Notification build() {
                return builder.build();
            }
        };
    }
}