package com.pushwoosh.huawei;

import android.content.Context;
import android.os.AsyncTask;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;

public class PushwooshHmsMessagingService extends HmsMessageService {
    private final static String TAG = "HmsMessageService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        PWLog.noise(TAG, "onMessageReceived()");
        try {
            super.onMessageReceived(remoteMessage);

            Context context = AndroidPlatformModule.getApplicationContext();
            if (context == null) {
                PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
                return;
            }
            new onMessageReceivedTask(context, remoteMessage).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to handle message", e);
        }
    }

    @Override
    public void onNewToken(String token) {
        PWLog.noise(TAG, String.format("onNewToken(): %s", token));
        try {
            super.onNewToken(token);
            PushwooshHmsHelper.onTokenRefresh(token);
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to handle new token", e);
        }
    }

    private static class onMessageReceivedTask extends AsyncTask<Void, Void, Void> {
        RemoteMessage remoteMessage;
        Context context;

        public onMessageReceivedTask(Context context, RemoteMessage remoteMessage) {
            this.remoteMessage = remoteMessage;
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            PushwooshHmsHelper.onMessageReceived(context, remoteMessage);
            return null;
        }
    }
}
