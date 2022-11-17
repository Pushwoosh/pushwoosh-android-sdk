package com.pushwoosh.notification;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.builder.NotificationBuilderManager;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.StatusBarNotificationStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationIntentHelper {

    public static final String EXTRA_NOTIFICATION_BUNDLE = "pushBundle";
    public static final String EXTRA_GROUP_ID = "group_id";
    public static final String EXTRA_NOTIFICATION_ROW_ID = "row_id";
    public static final String EXTRA_IS_DELETE_INTENT = "is_delete_intent";
    public static final String EXTRA_IS_SUMMARY_NOTIFICATION = "is_summary_notification";
    public static final String EXTRA_PUSHWOOSH_NOTIFICATION_ID = "pushwoosh_notification_id";
    public static final int SUMMARY_GROUP_ID = 20191017;

    public static void processIntent(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        if (intent.getBooleanExtra(EXTRA_IS_DELETE_INTENT, false)) {
            handleDeleteIntent(intent);
        } else if (intent.getBooleanExtra(EXTRA_IS_SUMMARY_NOTIFICATION, false)) {
            handleNotificationGroup(intent);
        } else {
            handleNotification(intent);
        }
    }

    private static void handleDeleteIntent(Intent intent) {
        if (intent.getBooleanExtra(EXTRA_IS_SUMMARY_NOTIFICATION, false)) {
            new RemoveAllGroupPushBundlesTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            return;
        }
        long rowId = intent.getLongExtra(EXTRA_NOTIFICATION_ROW_ID, 0);
        if (rowId > 0) {
            new RemoveGroupPushBundleTask(rowId).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            new UpdateSummaryNotificationTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
        long pushwooshNotificationID = intent.getLongExtra(EXTRA_PUSHWOOSH_NOTIFICATION_ID, -1);
        if (pushwooshNotificationID != -1) {
            new UpdateNotificationStorageTask(pushwooshNotificationID).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }

    private static void handleNotification(Intent intent) {
        try {
            Bundle pushBundle = intent.getBundleExtra(EXTRA_NOTIFICATION_BUNDLE);
            NotificationServiceExtension notificationServiceExtension = PushwooshPlatform.getInstance().notificationService();
            notificationServiceExtension.handleNotification(pushBundle);
        } catch (Exception e) {
            PWLog.exception(e);
        }

        if (intent.getIntExtra(EXTRA_GROUP_ID, 0) == SUMMARY_GROUP_ID) {
            new UpdateSummaryNotificationTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }

    private static void handleNotificationGroup(Intent intent) {
        new GetGroupPushMessagesTask(pushMessages -> onGetGroupPushMessagesSuccess(pushMessages), () -> onGetGroupPushMessagesFail(intent))
                .executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    private static void onGetGroupPushMessagesSuccess(List<PushMessage> pushMessages) {
        try {
            NotificationServiceExtension notificationServiceExtension = PushwooshPlatform.getInstance().notificationService();
            notificationServiceExtension.handleNotificationGroup(pushMessages);
        } catch (Exception e) {
            PWLog.exception(e);
        }
    }

    private static void onGetGroupPushMessagesFail(Intent intent) {
        handleNotification(intent);
    }

    private static class RemoveAllGroupPushBundlesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            RepositoryModule.getPushBundleStorage().removeGroupPushBundles();
            return null;
        }
    }

    private static class RemoveGroupPushBundleTask extends AsyncTask<Void, Void, Void> {
        private final long rowId;

        public RemoveGroupPushBundleTask(long rowId) {
            this.rowId = rowId;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Context context = AndroidPlatformModule.getApplicationContext();
            if (context == null) {
                PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
                return null;
            }
            RepositoryModule.getPushBundleStorage().removeGroupPushBundle(rowId);
            return null;
        }
    }

    private static class GetGroupPushMessagesTask extends AsyncTask<Void, Void, List<PushMessage>> {
        private final GetGroupPushMessagesSuccessCallback successCallback;
        private final GetGroupPushMessagesFailureCallback failureCallback;

        public GetGroupPushMessagesTask(@NonNull GetGroupPushMessagesSuccessCallback successCallback,
                                        @NonNull GetGroupPushMessagesFailureCallback failureCallback) {
            this.successCallback = successCallback;
            this.failureCallback = failureCallback;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        protected List<PushMessage> doInBackground(Void... voids) {
            try {
                List<PushMessage> pushMessages = getPushMessages();
                NotificationBuilderManager.cancelLastStatusBarNotification();

                return pushMessages;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<PushMessage> pushMessages) {
            super.onPostExecute(pushMessages);
            if (pushMessages != null && !pushMessages.isEmpty()) {
                successCallback.onSuccess(pushMessages);
            } else {
                failureCallback.onFail();
            }
        }

        private List<PushMessage> getPushMessages() {
            List<Bundle> pushBundles = RepositoryModule.getPushBundleStorage().getGroupPushBundles();
            if (pushBundles == null || pushBundles.isEmpty()) {
                return Collections.emptyList();
            }
            List<PushMessage> pushMessagesList = new ArrayList<>();
            for (Bundle pushBundle : pushBundles) {
                pushMessagesList.add(new PushMessage(pushBundle));
            }
            return pushMessagesList;
        }
    }

    private static class UpdateSummaryNotificationTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                List<StatusBarNotification> activeNotifications = NotificationBuilderManager.getActiveNotifications();
                if (activeNotifications == null) {
                    return null;
                }
                /*if group summary had 2 notifications, and one was closed, it will still exist,
                but the last notification will be shown separately, so we need to cancel summary here */
                if (activeNotifications.isEmpty()) {
                    NotificationBuilderManager.cancelGroupSummary();
                    return null;
                }
                Notification summaryNotification =
                        SummaryNotificationUtils.getSummaryNotification(activeNotifications.size(),
                                SummaryNotificationFactory.NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID);
                if (summaryNotification == null) {
                    return null;
                }
                SummaryNotificationUtils.fireSummaryNotification(summaryNotification);
            }
            return null;
        }
    }

    private static class UpdateNotificationStorageTask extends AsyncTask<Void, Void, Void> {
        private final long notificationId;

        public UpdateNotificationStorageTask(long notificationId) {
            this.notificationId = notificationId;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            StatusBarNotificationStorage storage = RepositoryModule.getStatusBarNotificationStorage();
            try {
                storage.remove(notificationId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private interface GetGroupPushMessagesSuccessCallback {
        void onSuccess(List<PushMessage> pushMessages);
    }

    private interface GetGroupPushMessagesFailureCallback {
        void onFail();
    }
}
