package com.pushwoosh;

import android.content.Context;
import android.os.Bundle;

import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushBundleDataProvider;


public class PushwooshMessagingServiceHelper {
    public static void onTokenRefresh(String token) {
        PWLog.noise("PushwooshMessagingServiceHelper", String.format("onTokenRefresh: %s", token));
        NotificationRegistrarHelper.onRegisteredForRemoteNotifications(token, null);
    }

    public static boolean onMessageReceived(Context context, Bundle pushBundle) {
        PushwooshInitializer.init(context);
        sendMessageDeliveryEvent(pushBundle);
        NotificationRegistrarHelper.handleMessage(pushBundle);

        return true;
    }

    static void sendMessageDeliveryEvent(Bundle pushBundle) {
        try {
            String pushHash = PushBundleDataProvider.getPushHash(pushBundle);
            String pushMetaData = PushBundleDataProvider.getPushMetadata(pushBundle);
            PushwooshPlatform.getInstance().pushwooshRepository().sendPushDelivered(pushHash, pushMetaData);
        } catch(Throwable t) {
            PWLog.error("/messageDeliveryEvent was not sent. Exception occurred " + t.getClass().getCanonicalName() + ". " + t.getMessage());
        }
    }

    static void sendPushStat(Bundle pushBundle) {
        try {
            String pushHash = PushBundleDataProvider.getPushHash(pushBundle);
            String metadata = PushBundleDataProvider.getPushMetadata(pushBundle);
            PushwooshPlatform.getInstance().pushwooshRepository().sendPushOpened(pushHash, metadata);
        } catch (Throwable t) {
            PWLog.error("/pushStat request was not sent. Exception occurred " + t.getClass().getCanonicalName() + ". " + t.getMessage());
        }
    }
}
