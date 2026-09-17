package com.pushwoosh.liveupdates.internal;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.NotificationUpdateReceiver;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PendingIntentUtils;
import com.pushwoosh.notification.NotificationFactory;
import com.pushwoosh.notification.NotificationIntentHelper;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationFactory;
import com.pushwoosh.repository.RepositoryModule;

/**
 * Builds the tap (content) and swipe-dismiss (delete) {@link PendingIntent}s for a live-update
 * notification by reusing the core notification-open machinery, so a Live Update behaves like a
 * regular push: a tap opens the app, honours deep links, sends open statistics and fires
 * {@code NotificationServiceExtension.onMessageOpened}; a user swipe fires
 * {@code NotificationServiceExtension.onMessageCanceled}. Neither builder throws: any failure is
 * logged and reported as {@code null} so the notification still posts without that intent.
 */
final class LiveUpdateIntents {

    private static final String TAG = "LiveUpdateIntents";

    private LiveUpdateIntents() {}

    /** Tap intent: routes through the (possibly custom) {@link NotificationFactory}, like a regular push. */
    @Nullable static PendingIntent contentIntent(
            @NonNull Context context, @NonNull PushMessage message, @NonNull String activityId) {
        try {
            Intent intent = resolveNotificationFactory().getNotificationIntent(message);
            int flags = PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_CANCEL_CURRENT);
            return PendingIntent.getActivity(context, activityId.hashCode(), intent, flags);
        } catch (Throwable t) {
            PWLog.warn(TAG, "content intent build failed for " + activityId + ": " + t.getMessage());
            return null;
        }
    }

    /**
     * Swipe-dismiss intent: mirrors the core delete intent (ShowNotificationMessageHandler.getDeleteIntent)
     * minus the row/group bookkeeping — live updates are not stored in PushBundleStorage, and
     * NotificationIntentHelper.handleDeleteIntent skips cleanup when those extras are absent.
     */
    @Nullable static PendingIntent deleteIntent(
            @NonNull Context context, @NonNull PushMessage message, @NonNull String activityId) {
        try {
            Intent intent = new Intent(context, NotificationUpdateReceiver.class);
            intent.putExtra(NotificationIntentHelper.EXTRA_IS_DELETE_INTENT, true);
            intent.putExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE, message.toBundle());
            long pushwooshNotificationId = message.getPushwooshNotificationId();
            if (pushwooshNotificationId != -1) {
                intent.putExtra(NotificationIntentHelper.EXTRA_PUSHWOOSH_NOTIFICATION_ID, pushwooshNotificationId);
            }
            // Stable action per activityId: keeps FLAG_CANCEL_CURRENT replacing the prior registration.
            intent.setAction("pw_live_delete:" + activityId);
            int flags = PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_CANCEL_CURRENT);
            return PendingIntent.getBroadcast(context, activityId.hashCode(), intent, flags);
        } catch (Throwable t) {
            PWLog.warn(TAG, "delete intent build failed for " + activityId + ": " + t.getMessage());
            return null;
        }
    }

    /** Mirrors ShowNotificationMessageHandler.provideNotificationFactory(): honour a custom factory. */
    @NonNull private static NotificationFactory resolveNotificationFactory() {
        try {
            Class<?> clazz = RepositoryModule.getNotificationPreferences()
                    .notificationFactoryClass()
                    .get();
            if (clazz != null) {
                return (NotificationFactory) clazz.newInstance();
            }
        } catch (Throwable t) {
            PWLog.noise(TAG, "custom notification factory unavailable, using default: " + t.getMessage());
        }
        return new PushwooshNotificationFactory();
    }
}
