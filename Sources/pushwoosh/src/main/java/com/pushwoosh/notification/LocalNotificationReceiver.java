package com.pushwoosh.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PendingIntentUtils;
import com.pushwoosh.repository.DbLocalNotification;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.RepositoryModule;

import java.util.Set;

public class LocalNotificationReceiver extends BroadcastReceiver {
    public static final String TAG = LocalNotificationReceiver.class.getSimpleName();
    public static final String EXTRA_NOTIFICATION_ID = "local_push_id";
    public static final int WEEK = 1000 * 60 * 60 * 24 * 7;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        try {
            final Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }

            LocalNotificationStorage storage = RepositoryModule.getLocalNotificationStorage();
            String pushId = extras.getString(EXTRA_NOTIFICATION_ID);
            storage.removeLocalNotification(Integer.parseInt(pushId));

            new HandleMessageTask(extras).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            PWLog.exception(e);
        }
    }

    /**
     * Create notification from {@link android.os.Bundle} which will be appeared after {@param seconds} seconds
     *
     * @param extras  data for local notification
     * @param seconds time for schedule
     * @return -1 if failed otherwise request id of notification task
     */
    public static int scheduleNotification(Bundle extras, int seconds) {
        try {
            Context context = AndroidPlatformModule.getApplicationContext();

            if (context == null) {
                PWLog.error(TAG, AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
                return -1;
            }

            LocalNotificationStorage storage = RepositoryModule.getLocalNotificationStorage();
            long offsetMillis = ((long) seconds) * 1000L;
            long triggerAtMillis = System.currentTimeMillis() + offsetMillis;

            int requestId = storage.nextRequestId();

            Intent intent = createIntent(context, requestId, extras);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestId, intent, PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_UPDATE_CURRENT));
            storage.saveLocalNotification(requestId, intent.getExtras(), triggerAtMillis);

            if (scheduleAlarm(triggerAtMillis, pendingIntent)) {
                return requestId;
            } else {
                return -1;
            }

        } catch (Exception e) {
            PWLog.error(TAG, "Creation of local notification failed.", e);
        }
        return -1;
    }

    public static void rescheduleNotification(DbLocalNotification localNotification, long currentTime) {
        try {
            Context context = AndroidPlatformModule.getApplicationContext();

            if (context == null) {
                PWLog.error(TAG, AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
                return;
            }

            if (isOldNotification(localNotification, currentTime)) {
                LocalNotificationStorage storage = RepositoryModule.getLocalNotificationStorage();
                storage.removeLocalNotification(localNotification.getRequestId());
                return;
            }
            int requestId = localNotification.getRequestId();
            long triggerAtMillis = getTriggerTime(localNotification, currentTime);

            Bundle extras = localNotification.getBundle();
            Intent intent = createIntent(context, requestId, extras);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestId, intent, PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_UPDATE_CURRENT));
            scheduleAlarm(triggerAtMillis, pendingIntent);
        } catch (Exception e) {
            PWLog.error(TAG, "Creation of local notification failed.", e);
        }
    }

    private static boolean isOldNotification(DbLocalNotification localNotification, long currentTime) {
        long realDelay = currentTime - localNotification.getTriggerAtMillis();
        return realDelay >= WEEK;
    }

    @NonNull
    private static Intent createIntent(Context context, int requestId, Bundle extras) {
        Intent intent = new Intent(context, LocalNotificationReceiver.class);
        intent.putExtras(extras);
        intent.putExtra(EXTRA_NOTIFICATION_ID, String.valueOf(requestId));
        return intent;
    }

    private static boolean scheduleAlarm(long triggerAtMillis, PendingIntent pendingIntent) {
        try {
            AlarmManager alarmManager = AndroidPlatformModule.getManagerProvider().getAlarmManager();
            if (alarmManager == null) {
                return false;
            }
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } catch (SecurityException e) {
            PWLog.error(TAG, String.format("Too many alarms. Please clear all local alarm to continue use AlarmManager. Local notification will be skipped"));
            return false;
        }
        return true;
    }

    private static long getTriggerTime(DbLocalNotification localNotification, long currentTime) {
        long triggerAtMillis = localNotification.getTriggerAtMillis();
        long delay = Math.max(5000, (triggerAtMillis - currentTime));
        long triggerTime = currentTime + delay;
        return triggerTime;
    }

    public static void cancelAll() {
        LocalNotificationStorage storage = RepositoryModule.getLocalNotificationStorage();
        Set<Integer> requestIds = storage.getRequestIds();
        for (Integer requestId : requestIds) {
            try {
                cancelNotification(requestId);
            } catch (Exception e) {
                PWLog.exception(e);
            }
        }
    }

    public static void cancelNotification(int requestId) {
        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            PWLog.error(TAG, AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
            return;
        }
        LocalNotificationStorage storage = RepositoryModule.getLocalNotificationStorage();
        storage.removeLocalNotification(requestId);

        Intent intent = new Intent(context, LocalNotificationReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, requestId, intent, PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_UPDATE_CURRENT));

        // Get the AlarmManager service
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(sender);
        }
    }

    private static class HandleMessageTask extends AsyncTask<Void, Void, Void> {
        private final Bundle extras;

        HandleMessageTask(Bundle extras) {
            this.extras = extras;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            NotificationServiceExtension notificationServiceExtension = PushwooshPlatform.getInstance().notificationService();
            notificationServiceExtension.handleMessage(extras);
            return null;
        }
    }
}
