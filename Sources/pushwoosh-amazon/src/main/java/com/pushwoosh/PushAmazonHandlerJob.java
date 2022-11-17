package com.pushwoosh;

import android.content.Context;
import android.content.Intent;

import com.amazon.device.messaging.ADMMessageHandlerJobBase;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;

public class PushAmazonHandlerJob extends ADMMessageHandlerJobBase {
    public static final String TAG = "PushAmazonHandlerJob";

    @Override
    protected void onMessage(Context context, Intent intent) {
        PWLog.info(TAG, "Received message");

        NotificationRegistrarHelper.handleMessage(intent.getExtras());
    }

    @Override
    protected void onRegistrationError(Context context, String errorId) {
        PWLog.error(TAG, "Messaging registration error: " + errorId);

        NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(errorId);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        PWLog.info(TAG, "Device registered: regId = " + registrationId);

        NotificationRegistrarHelper.onRegisteredForRemoteNotifications(registrationId);
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        PWLog.info(TAG, "Device unregistered");

        NotificationRegistrarHelper.onUnregisteredFromRemoteNotifications(registrationId);
    }
}
