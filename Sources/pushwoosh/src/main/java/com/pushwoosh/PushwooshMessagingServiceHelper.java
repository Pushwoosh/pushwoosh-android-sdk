package com.pushwoosh;

import android.content.Context;
import android.os.Bundle;

import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;

public class PushwooshMessagingServiceHelper {
    public static void onTokenRefresh(String token) {
        Context context = AndroidPlatformModule.getApplicationContext();
        PushwooshInitializer.init(context);

        NotificationRegistrarHelper.onRegisteredForRemoteNotifications(token, null);
        EventBus.sendEvent(new RegistrationSuccessEvent(new RegisterForPushNotificationsResultData(token,true)));
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
