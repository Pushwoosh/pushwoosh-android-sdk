package com.pushwoosh;

import android.content.Context;
import android.content.Intent;

import com.amazon.device.messaging.ADMMessageHandlerJobBase;
import com.pushwoosh.amazon.TagsRegistrarHelper;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.tags.TagsBundle;

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

        try {
            NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(errorId);
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to handle registration error", e);
        }
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        PWLog.info(TAG, "Device registered: regId = " + registrationId);

        try {
            TagsBundle tags = TagsRegistrarHelper.tagsBundle;
            String tagsJson = tags != null ? tags.toJson().toString() : null;

            NotificationRegistrarHelper.onRegisteredForRemoteNotifications(registrationId, tagsJson);
            TagsRegistrarHelper.tagsBundle = null;
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to handle registration", e);
        }
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        PWLog.info(TAG, "Device unregistered");

        try {
            NotificationRegistrarHelper.onUnregisteredFromRemoteNotifications(registrationId);
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to handle unregistration", e);
        }
    }
}
