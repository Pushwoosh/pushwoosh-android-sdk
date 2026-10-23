package com.pushwoosh.firebase.internal.registrar;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.pushwoosh.firebase.internal.utils.FirebaseTokenHelper;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.work.BasePushwooshWorker;
import com.pushwoosh.repository.RepositoryModule;

public class FcmRegistrarWorker extends BasePushwooshWorker {
    public static final String DATA_REGISTER = "DATA_REGISTER";
    public static final String DATA_UNREGISTER = "DATA_UNREGISTER";
    public static final String DATA_TAGS = "DATA_TAGS";
    public static final String TAG = "FcmRegistrarWorker";
    public static final String PERIODIC_WORK_NAME = "FcmPeriodicRegistrarWorker";

    public FcmRegistrarWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull @Override
    protected String getLogTag() {
        return TAG;
    }

    private static Result registerPW(String tagsJson, boolean firstAttempt) {
        try {
            String savedPushToken =
                    RepositoryModule.getRegistrationPreferences().pushToken().get();
            // not on retries: by then the live token may be a newer one from onNewToken
            if (firstAttempt && !TextUtils.isEmpty(savedPushToken)) {
                FirebaseTokenHelper.deleteFirebaseToken();
            }
            final String token = FirebaseTokenHelper.getFirebaseToken();
            if (TextUtils.isEmpty(token)) {
                PWLog.info(TAG, "FCM token is empty");
                NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications("FCM token is empty");
                return Result.retry();
            }
            PWLog.debug(TAG, "FCM token is " + token);
            NotificationRegistrarHelper.onRegisteredForRemoteNotifications(token, tagsJson);
            return Result.success();
        } catch (IllegalStateException e) {
            String error = "Failed to retrieve token. Is firebase configured correctly?";
            PWLog.error(TAG, "FCM registration error: " + error);
            NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(error);
            return Result.success();
        } catch (Exception e) {
            PWLog.error(TAG, "FCM registration error", e);
            NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(e.getMessage());
            return Result.retry();
        }
    }

    private static void unregisterPW() {
        try {
            String token =
                    RepositoryModule.getRegistrationPreferences().pushToken().get();
            NotificationRegistrarHelper.onUnregisteredFromRemoteNotifications(token);
        } catch (Exception e) {
            PWLog.error(TAG, "Fcm deregistration error", e);
            NotificationRegistrarHelper.onFailedToUnregisterFromRemoteNotifications(e.getMessage());
        }
    }

    @NonNull @Override
    public Result doWork() {
        boolean register = getInputData().getBoolean(DATA_REGISTER, false);
        boolean unregister = getInputData().getBoolean(DATA_UNREGISTER, false);
        String tagsJson = getInputData().getString(DATA_TAGS);
        if (register) {
            return registerPW(tagsJson, getRunAttemptCount() == 0);
        } else if (unregister) {
            unregisterPW();
        }
        return Result.success();
    }
}
