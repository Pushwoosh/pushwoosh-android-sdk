package com.pushwoosh;

import android.content.Context;
import android.os.Bundle;

import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;

public class PushwooshMessagingServiceHelper {
    public static void onTokenRefresh(String token) {
        PWLog.noise("PushwooshMessagingServiceHelper", String.format("onTokenRefresh: %s", token));
        NotificationRegistrarHelper.onRegisteredForRemoteNotifications(token, null);
    }

    public static boolean onMessageReceived(Context context, Bundle pushBundle) {
        long receivedAtMillis = System.currentTimeMillis(); // before the SDK-ready queue, which may delay the task
        PWLog.noise("PushwooshMessagingServiceHelper", "onMessageReceived()");
        PushwooshInitializer.init(context);
        SdkStateProvider.getInstance().executeOrQueue(() -> {
            sendMessageDeliveryEvent(pushBundle, receivedAtMillis);
            NotificationRegistrarHelper.handleMessage(pushBundle);
        });
        return true;
    }

    static void sendMessageDeliveryEvent(Bundle pushBundle, long receivedAtMillis) {
        PWLog.noise("PushwooshMessagingServiceHelper", "sendMessageDeliveryEvent()");
        try {
            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle, receivedAtMillis);
        } catch (Throwable t) {
            PWLog.error("Failed to schedule delivery event", t);
        }
    }

    static void sendPushStat(Bundle pushBundle) {
        PWLog.noise("PushwooshMessagingServiceHelper", "sendPushStat()");
        try {
            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);
        } catch (Throwable t) {
            PWLog.error("Failed to schedule open event", t);
        }
    }
}
