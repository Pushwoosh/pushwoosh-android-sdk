/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

//
//  DeviceRegistrar.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.pushwoosh.repository;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

import com.pushwoosh.PushwooshWorkManagerHelper;
import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.RegisterDeviceWorker;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushPermissionStatusTracker;
import com.pushwoosh.notification.event.DeregistrationErrorEvent;
import com.pushwoosh.notification.event.DeregistrationSuccessEvent;
import com.pushwoosh.notification.event.RegistrationErrorEvent;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Register/unregister with the App server.
 */
public class DeviceRegistrar {
    public static final int PLATFORM_SMS = 18;
    public static final int PLATFORM_WHATSAPP = 21;

    private static final String TAG = "DeviceRegistrar";
    private static final int COOLDOWN_MINUTES = 10;

    // One delivery of the device's own token per process: lazyInit may rebuild the registrar.
    private static final ReentrantLock deliveryLock = new ReentrantLock();

    /** Reported when there is no saved token at all: nothing was sent and nothing failed. */
    public static class NothingToDeliverException extends NetworkException {
        NothingToDeliverException() {
            super("no push token saved, nothing to deliver");
        }
    }

    /**
     * Sends the saved push token to the server and applies the outcome (prefs, push permission
     * status, success/error events) synchronously, waiting for the lock shared by all callers
     * delivering the device's own token. Called by {@link RegisterDeviceWorker}; public only
     * because the worker lives in another package.
     *
     * @param tagsJson tags to attach to the request, or {@code null}
     * @return the request result; a success without the server's answer when the token rotated
     *     mid-flight (the newer delivery owns the outcome); a non-transient error when there is no
     *     token or push transport module
     */
    @WorkerThread
    public Result<Void, NetworkException> deliverFromWorker(@Nullable String tagsJson) {
        deliveryLock.lock();
        try {
            return deliverSavedToken(tagsJson);
        } finally {
            deliveryLock.unlock();
        }
    }

    /**
     * True when {@code token} is both the saved push token and the one the server last accepted.
     * Comparing against the saved token too: an app update wipes pushToken, and the device has to
     * re-deliver the token it still holds.
     */
    public boolean isDelivered(@NonNull String token) {
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        if (prefs == null || TextUtils.isEmpty(token)) {
            return false;
        }

        return token.equals(prefs.pushToken().get())
                && token.equals(prefs.deliveredPushToken().get());
    }

    /**
     * Same as {@link #deliverFromWorker(String)}, but gives up instead of waiting for an in-flight
     * delivery: waiting would hold the shared network thread for the whole request timeout. Callers
     * that need a follow-up delivery schedule one on {@code false}.
     *
     * @return {@code false} when a delivery was in flight and nothing was sent
     */
    @WorkerThread
    private boolean tryRegisterSavedToken(@Nullable String tagsJson) {
        if (!deliveryLock.tryLock()) {
            PWLog.debug(TAG, "device registration is already in flight, skipping this one");
            return false;
        }
        try {
            deliverSavedToken(tagsJson);
            return true;
        } finally {
            deliveryLock.unlock();
        }
    }

    private Result<Void, NetworkException> deliverSavedToken(@Nullable String tagsJson) {
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        if (prefs == null) {
            return Result.fromException(new NetworkException("SDK is not initialized"));
        }

        String token = prefs.pushToken().get();
        if (TextUtils.isEmpty(token)) {
            return Result.fromException(new NothingToDeliverException());
        }

        DeviceSpecificProvider provider = DeviceSpecificProvider.getInstance();
        if (provider == null) {
            String errorDescription = "can't register device: no push transport module";
            PWLog.error(TAG, errorDescription);
            EventBus.sendEvent(new RegistrationErrorEvent(errorDescription));
            return Result.fromException(new NetworkException(errorDescription));
        }

        String appCode = prefs.applicationId().get();
        RegisterDeviceRequest request = new RegisterDeviceRequest(token, tagsJson, provider.deviceType());
        Result<Void, NetworkException> result =
                NetworkModule.getRequestManager().sendRequestSync(request);

        boolean appCodeChanged = !TextUtils.isEmpty(appCode)
                && !appCode.equals(prefs.applicationId().get());
        if (!token.equals(prefs.pushToken().get()) || appCodeChanged) {
            // the newer delivery owns the outcome: stamping it here would hide the new code behind the cooldown;
            // a first app code is not a change — nothing re-delivers after it
            return Result.fromData(null);
        }

        if (result.isSuccess()) {
            PushPermissionStatusTracker.onStatusDelivered(request.getSentPushPermissionStatus());
            prefs.registeredOnServer().set(true);
            prefs.deliveredPushToken().set(token);
            prefs.lastPushRegistration().set(new Date().getTime());
            EventBus.sendEvent(new RegistrationSuccessEvent(
                    new RegisterForPushNotificationsResultData(token, areNotificationsEnabled())));
            PWLog.info(TAG, "Registered for push notifications: " + token);
        } else {
            PWLog.error(TAG, "can't register device", result.getException());
            EventBus.sendEvent(new RegistrationErrorEvent("can't register device"));
        }

        return result;
    }

    /**
     * Schedules a {@link RegisterDeviceWorker} run that delivers the saved token to the server,
     * retrying on transient failure. Replaces any previously scheduled, not-yet-run delivery.
     *
     * @param tagsJson tags to attach to the request, or {@code null}
     */
    private void schedule(@Nullable String tagsJson) {
        try {
            Data.Builder dataBuilder = new Data.Builder();
            if (tagsJson != null) {
                dataBuilder.putString(RegisterDeviceWorker.DATA_TAGS, tagsJson);
            }

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RegisterDeviceWorker.class)
                    .setInputData(dataBuilder.build())
                    .setConstraints(PushwooshWorkManagerHelper.getNetworkAvailableConstraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build();

            // the helper logs its own enqueue failure with the cause
            if (!PushwooshWorkManagerHelper.tryEnqueueOneTimeUniqueWork(
                    request, RegisterDeviceWorker.TAG, ExistingWorkPolicy.REPLACE)) {
                sendScheduleRegistrationFailed();
            }
        } catch (Exception e) {
            PWLog.error(TAG, "can't schedule device registration", e);
            sendScheduleRegistrationFailed();
        }
    }

    private static void sendScheduleRegistrationFailed() {
        EventBus.sendEvent(new RegistrationErrorEvent("can't schedule device registration"));
    }

    /**
     * Cancels a pending {@link RegisterDeviceWorker} run scheduled by {@link #schedule(String)}.
     */
    public void cancelScheduledRegistration() {
        PushwooshWorkManagerHelper.cancelUniqueWork(RegisterDeviceWorker.TAG);
    }

    /**
     * Unregisters the device's own token. Waits for an in-flight delivery of that token so
     * unregisterDevice never overtakes registerDevice on the server.
     */
    @WorkerThread
    public void unregisterWithServer(final String deviceRegistrationId) {
        PWLog.debug(TAG, "Unregistering for pushes...");
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        UnregisterDeviceRequest request =
                new UnregisterDeviceRequest(prefs.applicationId().get());

        deliveryLock.lock();
        try {
            // not in the explicit-appCode overload: after an app code change it would stomp the new app's flag
            prefs.registeredOnServer().set(false);
            prefs.deliveredPushToken().set("");
            reportUnregistration(
                    NetworkModule.getRequestManager().sendRequestSync(request), deviceRegistrationId, prefs);
        } finally {
            deliveryLock.unlock();
        }
    }

    /**
     * @param appCode application code to unregister from — passed explicitly because after an
     *                application code change the prefs already hold the new code by the time this
     *                is called
     */
    public void unregisterWithServer(final String deviceRegistrationId, String baseUrl, String appCode) {
        PWLog.debug(TAG, "Unregistering for pushes...");
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        UnregisterDeviceRequest request = new UnregisterDeviceRequest(appCode);
        NetworkModule.getRequestManager()
                .sendRequest(request, baseUrl, result -> reportUnregistration(result, deviceRegistrationId, prefs));
    }

    private static void reportUnregistration(
            Result<Void, NetworkException> result, String deviceRegistrationId, RegistrationPrefs prefs) {
        if (result.isSuccess()) {
            PWLog.info(TAG, "Unregistered for pushes: " + deviceRegistrationId);
            EventBus.sendEvent(new DeregistrationSuccessEvent(deviceRegistrationId));
            prefs.lastPushRegistration().set(0);
            return;
        }
        String errorDescription =
                result.getException() == null ? "" : result.getException().getMessage();
        if (TextUtils.isEmpty(errorDescription)) {
            errorDescription = "Pushwoosh unregistration error";
        }
        PWLog.error(TAG, "Unregistration error: " + errorDescription);
        EventBus.sendEvent(new DeregistrationErrorEvent(errorDescription));
    }

    /**
     * Reports that the SDK learned a push token — a fresh registration, the app's own token handed
     * to {@code registerExistingToken}, or a transport's {@code onNewToken} — and schedules its
     * delivery to the server.
     *
     * @param token token to deliver
     * @param tagsJson tags to attach to the request, or {@code null}
     */
    public void onTokenReceived(@NonNull String token, @Nullable String tagsJson) {
        PWLog.noise(TAG, "onTokenReceived()");

        if (DeviceSpecificProvider.getInstance() == null) {
            String errorDescription = "can't register device: no push transport module";
            PWLog.error(TAG, errorDescription);
            EventBus.sendEvent(new RegistrationErrorEvent(errorDescription));
            return;
        }

        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        if (prefs == null) {
            String errorDescription = "can't register device: SDK is not initialized";
            PWLog.error(TAG, errorDescription);
            EventBus.sendEvent(new RegistrationErrorEvent(errorDescription));
            return;
        }
        // written before scheduling: the FCM/HMS onNewToken dedup reads this pref in any SDK state
        prefs.pushToken().set(token);
        schedule(tagsJson);
    }

    /**
     * Reports that the app was opened and, respecting the cooldown of {@link #neededToRequestPushwooshServer()},
     * sends the saved token to the server when there is one to deliver.
     */
    public void onAppOpened() {
        PWLog.noise(TAG, "onAppOpened()");

        if (!hasSavedToken()) {
            return;
        }

        if (neededToRequestPushwooshServer()) {
            sendNow(null);
        }
    }

    /**
     * Reports that the application code changed and, when there is a token to deliver, sends it right
     * away, ignoring the cooldown of {@link #onAppOpened()}: the device is re-registering against a
     * different application, not repeating a recent registration against the same one.
     */
    public void onAppCodeChanged() {
        PWLog.noise(TAG, "onAppCodeChanged()");

        if (!hasSavedToken()) {
            return;
        }

        sendNow(null);
    }

    /**
     * Reports that the language changed. Zeroes {@code lastPushRegistration} first — the freshness
     * check {@code PushwooshNotificationManager} runs before registering reads it even when there is
     * no token to send — then, when there is a token to deliver, sends it right away, ignoring the
     * cooldown of {@link #onAppOpened()}.
     */
    public void onLanguageChanged() {
        PWLog.noise(TAG, "onLanguageChanged()");

        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        if (prefs == null) {
            return;
        }

        prefs.lastPushRegistration().set(0);

        if (!hasSavedToken()) {
            return;
        }

        sendNow(null);
    }

    /**
     * Reports that server communication resumed and, when there is a token to deliver, schedules a
     * {@link RegisterDeviceWorker} run instead of sending right away.
     */
    public void onServerCommunicationStarted() {
        PWLog.noise(TAG, "onServerCommunicationStarted()");

        if (!hasSavedToken()) {
            return;
        }

        schedule(null);
    }

    /**
     * Reports that the push permission status changed and, when there is a token to deliver, sends it
     * right away, bypassing the cooldown of {@link #onAppOpened()}.
     *
     * @param onDone runs once delivery is settled; right away when there is no push token
     */
    public void onPermissionStatusChanged(@NonNull Runnable onDone) {
        PWLog.noise(TAG, "onPermissionStatusChanged()");

        if (!hasSavedToken()) {
            onDone.run();
            return;
        }
        sendNow(onDone);
    }

    private boolean hasSavedToken() {
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        return prefs != null && !TextUtils.isEmpty(prefs.pushToken().get());
    }

    /**
     * Sends registerDevice right away. When a delivery is already in flight, schedules another one
     * after it instead of waiting on the shared network thread for the in-flight request's timeout:
     * that request was built from the previous prefs.
     *
     * @param onDone runs when the request completes or the follow-up delivery is scheduled
     */
    private void sendNow(@Nullable Runnable onDone) {
        BackgroundExecutor.network(() -> {
            try {
                if (!tryRegisterSavedToken(null)) {
                    schedule(null);
                }
            } finally {
                if (onDone != null) {
                    onDone.run();
                }
            }
        });
    }

    private static boolean neededToRequestPushwooshServer() {
        RegistrationPrefs registrationPrefs = RepositoryModule.getRegistrationPreferences();
        Calendar nowTime = Calendar.getInstance();
        Calendar tenMinutesBefore = Calendar.getInstance();
        tenMinutesBefore.add(Calendar.MINUTE, -COOLDOWN_MINUTES);

        Calendar lastPushWooshRegistrationTime = Calendar.getInstance();
        lastPushWooshRegistrationTime.setTime(
                new Date(registrationPrefs.lastPushRegistration().get()));

        if (tenMinutesBefore.before(lastPushWooshRegistrationTime) && lastPushWooshRegistrationTime.before(nowTime)) {
            // tenMinutesBefore <= lastPushWooshRegistrationTime <= nowTime
            return false;
        }
        return true;
    }

    public static boolean areNotificationsEnabled() {
        try {
            Context context = AndroidPlatformModule.getApplicationContext();
            if (context == null) {
                PWLog.warn(TAG, "areNotificationsEnabled: context is null");
                return true;
            }

            if (Build.VERSION.SDK_INT >= 33) {
                return ActivityCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                        == PackageManager.PERMISSION_GRANTED;
            } else {
                return NotificationManagerCompat.from(context).areNotificationsEnabled();
            }

        } catch (Exception e) {
            PWLog.exception(e);
            return true; // fall back to default behavior
        }
    }
}
