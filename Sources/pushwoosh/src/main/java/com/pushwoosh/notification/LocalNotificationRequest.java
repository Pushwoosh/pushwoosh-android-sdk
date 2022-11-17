package com.pushwoosh.notification;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.repository.DbLocalNotification;
import com.pushwoosh.repository.RepositoryModule;

import java.io.Serializable;

/**
 * Manages local notification schedule.
 */
public class LocalNotificationRequest implements Serializable {
    private int requestId;

    public int getRequestId() {
        return requestId;
    }

    public LocalNotificationRequest(int requestId) {
        this.requestId = requestId;
    }

    /**
     * Cancels local notification associated with this request and unschedules notification if it was not displayed yet.
     */
    public void cancel() {
        unschedule();

        DbLocalNotification dbLocalNotification = RepositoryModule.getLocalNotificationStorage().getLocalNotificationShown(requestId);
        if (dbLocalNotification != null) {
            int notificationId = dbLocalNotification.getNotificationId();
            String notificationTag = dbLocalNotification.getNotificationTag();
            android.app.NotificationManager manager = AndroidPlatformModule.getManagerProvider().getNotificationManager();

            if (manager == null) {
                return;
            }

            manager.cancel(notificationTag, notificationId);
        }
    }

    /**
     * Undo {@link com.pushwoosh.Pushwoosh#scheduleLocalNotification(LocalNotification)}. If notification has been displayed it will not be deleted.
     */
    public void unschedule() {
        LocalNotificationReceiver.cancelNotification(requestId);
    }
}
