package com.pushwoosh.firebase.internal.registrar;

import android.content.Context;
import android.text.TextUtils;

import com.pushwoosh.firebase.internal.utils.FirebaseTokenHelper;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.tags.TagsBundle;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class FcmRegistrarWorker extends Worker {
    public static final String DATA_REGISTER = "DATA_REGISTER";
    public static final String DATA_UNREGISTER = "DATA_UNREGISTER";
    public static final String DATA_TAGS = "DATA_TAGS";
    public static final String TAG = "FcmRegistrarWorker";
    public static final String PERIODIC_WORK_NAME = "FcmPeriodicRegistrarWorker";

    public FcmRegistrarWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private static void registerPW(String tagsJson) {
        String error = "";
        try {
            String savedPushToken = RepositoryModule.getRegistrationPreferences().pushToken().get();
            if (!TextUtils.isEmpty(savedPushToken)) {
                FirebaseTokenHelper.deleteFirebaseToken();
            }
            final String token = FirebaseTokenHelper.getFirebaseToken();
            if (token != null) {
                PWLog.info(TAG, "FCM token is " + token);
                NotificationRegistrarHelper.onRegisteredForRemoteNotifications(token, tagsJson);
            } else {
                PWLog.info(TAG, "FCM token is empty");
            }
        } catch (IllegalStateException e) {
            PWLog.error(TAG, "FCM registration error: Failed to retrieve token. Is firebase configured correctly?");
            NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(error);
        } catch (Exception e) {
            error = e.getMessage();
            PWLog.error(TAG, "FCM registration error:" + error);
        }
    }

    private static void unregisterPW() {
        try {
            String token = RepositoryModule.getRegistrationPreferences().pushToken().get();
            NotificationRegistrarHelper.onUnregisteredFromRemoteNotifications(token);
        } catch (Exception e) {
            PWLog.error(TAG, "Fcm deregistration error", e);
            NotificationRegistrarHelper.onFailedToUnregisterFromRemoteNotifications(e.getMessage());
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean register = getInputData().getBoolean(DATA_REGISTER, false);
        boolean unregister = getInputData().getBoolean(DATA_UNREGISTER, false);
        String tagsJson = getInputData().getString(DATA_TAGS);
        if (register) {
            PWLog.debug("do unique register work");
            registerPW(tagsJson);
        } else if (unregister) {
            unregisterPW();
        }
        return Result.success();
    }
}
