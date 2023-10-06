package com.pushwoosh.firebase.internal.registrar;

import android.content.Context;

import com.pushwoosh.firebase.internal.utils.FirebaseTokenHelper;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

import java.lang.reflect.InvocationTargetException;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class FcmRegistrarWorker extends Worker {
    public static final String DATA_REGISTER = "DATA_REGISTER";
    public static final String DATA_UNREGISTER = "DATA_UNREGISTER";
    public static final String TAG = "FcmRegistrarWorker";

    public FcmRegistrarWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private static void registerPW() {
        String error = "";
        try {
            FirebaseTokenHelper.deleteFirebaseToken();
            final String token = FirebaseTokenHelper.getFirebaseToken();
            if (token != null) {
                PWLog.info(TAG, "FCM token is " + token);
                NotificationRegistrarHelper.onRegisteredForRemoteNotifications(token);
            } else {
                PWLog.info(TAG, "FCM token is empty");
            }
        } catch (IllegalStateException e) {
            PWLog.error(TAG, "FCM registration error: Failed to retrieve token. Is firebase configured correctly?");
            NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(error);
        } catch (InvocationTargetException invocationTargetException) {
            if (invocationTargetException.getTargetException() != null) {
                error = invocationTargetException.getTargetException().getMessage();
                PWLog.error(TAG, "FCM registration error:" + error);
            }
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
        if (register) {
            registerPW();
        } else if (unregister) {
            unregisterPW();
        }
        return Result.success();
    }
}
