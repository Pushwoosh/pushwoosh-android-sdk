package com.pushwoosh.firebase.internal.utils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
//import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pushwoosh.Pushwoosh;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

// Firebase-messaging backwards-compatibility helper class

public class FirebaseTokenHelper {

    public static Class<?> tryGetFirebaseIidClass() {
        try {
            return Class.forName("com.google.firebase.iid.FirebaseInstanceId");
        } catch (Exception e) {
            throw new NoClassDefFoundError("com.google.firebase.iid.FirebaseInstanceId class not found");
        }
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static String getFirebaseToken() throws InvocationTargetException, IllegalAccessException,
            NoSuchMethodException, InterruptedException, ExecutionException {
        try {
            Class<?> iidClass = tryGetFirebaseIidClass();
            Method getFirebaseIidInstance = iidClass.getDeclaredMethod("getInstance");
            // if getToken has 2 arguments, then it is FirebaseInstanceId
            Class<?>[] args = {String.class, String.class};
            Method getToken = iidClass.getDeclaredMethod("getToken", args);
            return (String) getToken.invoke(getFirebaseIidInstance.invoke(null), Pushwoosh.getInstance().getSenderId(), "FCM");
        } catch (NoClassDefFoundError | NoSuchMethodException e) {
            try {
                // if getToken has 0 arguments, then it is FirebaseMessaging
                Class<?>[] args = {};
                Method getToken = FirebaseMessaging.class.getDeclaredMethod("getToken", args);
                Task<String> getTokenTask = (Task<String>) getToken.invoke(FirebaseMessaging.getInstance());
                // this code is already executed in a separate thread so it is safe to await for the result
                return Tasks.await(getTokenTask);
            } catch (NoSuchMethodException e1) {
                throw new NoSuchMethodException(FirebaseMessaging.class.getCanonicalName() +
                        " does not have a getToken() method");
            } catch (InterruptedException e1) {
                throw new InterruptedException("Failed to fetch push token from FCM: " + e1.getMessage());
            } catch (ExecutionException e1) {
                throw new ExecutionException(
                        new Throwable("Failed to fetch push token from FCM: " + e1.getMessage()));
            }
        }
    }

//    public static void deleteFirebaseToken() throws InvocationTargetException,
//            IllegalAccessException, NoSuchMethodException {
//        try {
//            Method delete = FirebaseInstanceId.class.getDeclaredMethod("deleteInstanceId");
//            delete.invoke(FirebaseInstanceId.getInstance());
//        } catch (NoClassDefFoundError | NoSuchMethodException e) {
//            try {
//                Method delete = FirebaseMessaging.class.getDeclaredMethod("deleteToken");
//                delete.invoke(FirebaseMessaging.getInstance());
//            } catch (NoSuchMethodException e1) {
//                throw new NoSuchMethodException("Failed to delete Firebase token: " + e1.getMessage());
//            }
//        } catch (IllegalAccessException e) {
//            throw new IllegalAccessException("Failed to delete Firebase token: " + e.getMessage());
//        } catch (IllegalArgumentException e) {
//            throw new IllegalArgumentException("Failed to delete Firebase token: " + e.getMessage());
//        } catch (InvocationTargetException e) {
//            throw new InvocationTargetException(
//                    new Throwable("Failed to delete Firebase token: " + e.getMessage()));
//        }
//    }
}
