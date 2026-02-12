package com.pushwoosh.huawei;

import android.content.Context;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;

public class PushwooshHmsMessagingService extends HmsMessageService {
    private static final String TAG = "HmsMessageService";

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
            BackgroundExecutor.execute(() -> PushwooshHmsHelper.onMessageReceived(context, remoteMessage));
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
}
