package com.pushwoosh.test.manifest;

import android.app.Notification;

import androidx.annotation.NonNull;

import com.pushwoosh.notification.NotificationFactory;
import com.pushwoosh.notification.PushMessage;

public class FakeNoCtorNotificationFactory extends NotificationFactory {
    @SuppressWarnings("unused")
    public FakeNoCtorNotificationFactory(int unused) {
        super();
    }

    @Override
    public Notification onGenerateNotification(@NonNull PushMessage data) {
        return null;
    }
}
