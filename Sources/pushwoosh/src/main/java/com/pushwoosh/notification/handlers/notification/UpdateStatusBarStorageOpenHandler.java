package com.pushwoosh.notification.handlers.notification;

import android.os.AsyncTask;
import android.os.Bundle;

import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.repository.RepositoryModule;

public class UpdateStatusBarStorageOpenHandler implements PushNotificationOpenHandler {

    @Override
    public void postHandleNotification(Bundle pushBundle) {
        new RemoveNotificationPairFromStorageTask(pushBundle).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    public static class RemoveNotificationPairFromStorageTask extends AsyncTask<Void, Void, Void> {
        Bundle pushBundle;
        String TAG = UpdateStatusBarStorageOpenHandler.class.getSimpleName();

        public RemoveNotificationPairFromStorageTask(Bundle pushBundle) {
            this.pushBundle = pushBundle;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                PushMessage pushMessage = new PushMessage(pushBundle);
                long pushwooshId = pushMessage.getPushwooshNotificationId();

                RepositoryModule.getStatusBarNotificationStorage().remove(pushwooshId);
            } catch (Exception e) {
                PWLog.error(TAG, "Failed to remove database entry: " + e.getMessage());
            }

            return null;
        }
    }
}
