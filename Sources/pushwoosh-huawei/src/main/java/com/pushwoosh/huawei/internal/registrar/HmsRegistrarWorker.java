package com.pushwoosh.huawei.internal.registrar;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.pushwoosh.PushwooshMessagingServiceHelper;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.huawei.PushwooshHmsHelper;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.RepositoryModule;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static com.pushwoosh.internal.platform.AndroidPlatformModule.NULL_CONTEXT_MESSAGE;

public class HmsRegistrarWorker extends Worker {
    public static final String DATA_REGISTER = "DATA_REGISTER";
    public static final String DATA_UNREGISTER = "DATA_UNREGISTER";
    public static final String DATA_TAGS = "DATA_TAGS";
    public static final String TAG = "HmsRegistrarWorker";

    private Context context;

    public HmsRegistrarWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    private static void registerPW(Context context, String tagsJson) {
        new PushwooshHmsHelper.GetTokenAsync(new PushwooshHmsHelper.OnGetTokenAsync() {
            @Override
            public void onGetToken(String token) {
                if (token != null) {
                    PWLog.info(TAG, "HCM token is " + token);
                    NotificationRegistrarHelper.onRegisteredForRemoteNotifications(token, tagsJson);
                } else {
                    PWLog.info(TAG, "HCM token is empty");
                }
            }
            @Override
            public void onError(String error) {
                PWLog.error(TAG, error);
                PWLog.error(TAG, "HCM registration error: Failed to retrieve token. Is Huawei SDK configured correctly?");
                NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(error);
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static void unregisterPW(Context context) {
        try {
            // read from agconnect-services.json
            String appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id");

            HmsInstanceId.getInstance(context).deleteToken(appId, "HCM");
            PWLog.debug(TAG, "HCM deleteToken success.");

            String token = RepositoryModule.getRegistrationPreferences().pushToken().get();
            NotificationRegistrarHelper.onUnregisteredFromRemoteNotifications(token);
        } catch (Exception e) {
            PWLog.error(TAG, "HCM deleteToken failed.", e);

            NotificationRegistrarHelper.onFailedToUnregisterFromRemoteNotifications(e.getMessage());
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean register = getInputData().getBoolean(DATA_REGISTER, false);
        boolean unregister = getInputData().getBoolean(DATA_UNREGISTER, false);
        String tags = getInputData().getString(DATA_TAGS);
        Context context = this.context;

        if (context == null) {
            PWLog.error(NULL_CONTEXT_MESSAGE);
            return Result.success();
        }

        if (register) {
            registerPW(context, tags);
        } else if (unregister) {
            unregisterPW(context);
        }

        return Result.success();
    }
}
