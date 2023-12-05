package com.pushwoosh.firebase.internal.utils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.concurrent.ExecutionException;

public class FirebaseTokenHelper {

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static String getFirebaseToken() throws ExecutionException, InterruptedException {
        try {
            Task<String> getTokenTask = FirebaseMessaging.getInstance().getToken();
            return Tasks.await(getTokenTask);
        } catch (InterruptedException e1) {
            throw new InterruptedException("Failed to fetch push token from FCM: " + e1.getMessage());
        } catch (ExecutionException e1) {
            throw new ExecutionException(
                    new Throwable("Failed to fetch push token from FCM: " + e1.getMessage()));
        }
    }

    public static void deleteFirebaseToken() throws ExecutionException, InterruptedException {
        try {
            Task<Void> deleteTokenTask = FirebaseMessaging.getInstance().deleteToken();
            Tasks.await(deleteTokenTask);
        }  catch (InterruptedException e) {
            throw new InterruptedException("Failed to delete Firebase token: " + e.getMessage());
        } catch (ExecutionException e) {
            throw new ExecutionException(
                    new Throwable("Failed to delete Firebase token: " + e.getMessage()));
        }
    }
}
