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

package com.pushwoosh.firebase;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pushwoosh.PushwooshMessagingServiceHelper;
import com.pushwoosh.firebase.internal.RemoteMessageUtils;
import com.pushwoosh.firebase.internal.mapper.RemoteMessageMapper;
import com.pushwoosh.firebase.internal.specific.FcmDeviceSpecificIniter;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

/**
 * Helper class for integrating Pushwoosh with Firebase Cloud Messaging (FCM) in custom FirebaseMessagingService implementations.
 * <p>
 * By default, Pushwoosh SDK automatically handles Firebase Cloud Messaging without any additional code.
 * However, if your app needs a custom {@link FirebaseMessagingService} (for example, to handle messages from
 * multiple push providers), use this helper class to forward Firebase callbacks to Pushwoosh.
 * <p>
 * <b>Important:</b> Use this helper ONLY if you need a custom {@link FirebaseMessagingService}.
 * For customizing notifications, use {@link com.pushwoosh.notification.NotificationServiceExtension} instead.
 * <p>
 * <b>Critical Integration Steps:</b>
 * <ol>
 * <li>Create your custom {@link FirebaseMessagingService} class</li>
 * <li>Override {@link FirebaseMessagingService#onNewToken(String)} and call {@link #onTokenRefresh(String)}</li>
 * <li>Override {@link FirebaseMessagingService#onMessageReceived(RemoteMessage)} and call {@link #onMessageReceived(Context, RemoteMessage)}</li>
 * <li>Register your service in AndroidManifest.xml</li>
 * </ol>
 * <p>
 * <b>Example - Multiple Push Providers:</b>
 * <pre>
 * {@code
 * public class MyFirebaseMessagingService extends FirebaseMessagingService {
 *
 *     @Override
 *     public void onNewToken(@NonNull String token) {
 *         super.onNewToken(token);
 *
 *         // CRITICAL: Forward to Pushwoosh to keep receiving notifications
 *         PushwooshFcmHelper.onTokenRefresh(token);
 *
 *         // Forward to other providers if needed
 *         OtherProvider.setToken(token);
 *     }
 *
 *     @Override
 *     public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
 *         super.onMessageReceived(remoteMessage);
 *
 *         // CRITICAL: Route Pushwoosh messages to Pushwoosh
 *         if (PushwooshFcmHelper.isPushwooshMessage(remoteMessage)) {
 *             PushwooshFcmHelper.onMessageReceived(this, remoteMessage);
 *         } else {
 *             // Handle other providers
 *             OtherProvider.handleMessage(remoteMessage);
 *         }
 *     }
 * }
 * }
 * </pre>
 * <br>
 * <b>AndroidManifest.xml:</b>
 * <pre>
 * {@code
 * <service
 *     android:name=".MyFirebaseMessagingService"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *     </intent-filter>
 * </service>
 * }
 * </pre>
 *
 * @see FirebaseMessagingService
 * @see #onTokenRefresh(String)
 * @see #onMessageReceived(Context, RemoteMessage)
 * @see #isPushwooshMessage(RemoteMessage)
 */
@SuppressWarnings("WeakerAccess")
public class PushwooshFcmHelper {
    private static final String TAG = "PushwooshFcmHelper";

    /**
     * Notifies Pushwoosh when Firebase Cloud Messaging token is refreshed.
     * <p>
     * <b>CRITICAL:</b> Call this method from your custom {@link FirebaseMessagingService#onNewToken(String)}
     * callback to forward the new FCM token to Pushwoosh. Without this call, Pushwoosh will NOT be able
     * to send notifications to the device after token refresh.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * public void onNewToken(@NonNull String token) {
     *     super.onNewToken(token);
     *
     *     // CRITICAL: Forward token to Pushwoosh
     *     PushwooshFcmHelper.onTokenRefresh(token);
     * }
     * }
     * </pre>
     *
     * @param token new Firebase Cloud Messaging token
     * @see FirebaseMessagingService#onNewToken(String)
     */
    public static void onTokenRefresh(String token) {
        PWLog.noise(TAG, String.format("onTokenRefresh: %s", token));

        if (!isDeviceSpecificOk()) {
            PWLog.error(TAG, "Device specific provider not ready for Firebase");
            return;
        }

        try {
            String previousToken = RepositoryModule.getRegistrationPreferences().pushToken().get();
            if (token == null || token.equals(previousToken)) {
                PWLog.debug(TAG, "token is null or equals previous token");
                return;
            }
            PushwooshMessagingServiceHelper.onTokenRefresh(token);
        } catch (Exception e) {
            PWLog.error(TAG, "can't refresh token", e);
        }
    }

    /**
     * Processes incoming Firebase Cloud Messaging push notifications for Pushwoosh.
     * <p>
     * <b>CRITICAL:</b> Call this method from your custom {@link FirebaseMessagingService#onMessageReceived(RemoteMessage)}
     * callback to let Pushwoosh handle its messages. Without this call, Pushwoosh notifications will NOT be displayed.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
     *     super.onMessageReceived(remoteMessage);
     *
     *     // CRITICAL: Route Pushwoosh messages to Pushwoosh
     *     if (PushwooshFcmHelper.isPushwooshMessage(remoteMessage)) {
     *         PushwooshFcmHelper.onMessageReceived(this, remoteMessage);
     *     } else {
     *         // Handle other providers
     *     }
     * }
     * }
     * </pre>
     *
     * @param context application or service context
     * @param remoteMessage Firebase Cloud Messaging remote message
     * @return true if the message was sent via Pushwoosh and was successfully processed; false otherwise
     * @see FirebaseMessagingService#onMessageReceived(RemoteMessage)
     * @see #isPushwooshMessage(RemoteMessage)
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean onMessageReceived(Context context, RemoteMessage remoteMessage) {
        PWLog.noise(TAG, "onMessageReceived()");

        if (!isDeviceSpecificOk()) {
            PWLog.error(TAG, "Device specific provider not ready for Firebase");
            return false;
        }

        if (!isPushwooshMessage(remoteMessage)) {
            PWLog.warn(TAG, "skip onMessageReceived: message is not belongs to Pushwoosh");
            return false;
        }

        try {
            PWLog.info(TAG, String.format("Received message: %s from: %s", remoteMessage.getData(), remoteMessage.getFrom()));

            Bundle pushBundle = RemoteMessageMapper.mapToBundle(remoteMessage);

            return PushwooshMessagingServiceHelper.onMessageReceived(context, pushBundle);
        } catch (Exception e) {
            PWLog.error(TAG, "can't handle onMessageReceived", e);
            return false;
        }
    }

    private static boolean ensureDeviceSpecificProviderInitialized() {
        try {
            if (DeviceSpecificProvider.getInstance() == null) {
                new DeviceSpecificProvider.Builder()
                        .setDeviceSpecific(FcmDeviceSpecificIniter.create())
                        .build(true);
                return DeviceSpecificProvider.getInstance() != null;
            }
        } catch (Exception e) {
            PWLog.error(TAG, "can't initialize DeviceSpecificProvider", e);
            return false;
        }
        return true;
    }

    private static boolean isDeviceSpecificOk() {
        if (!ensureDeviceSpecificProviderInitialized()) {
            PWLog.error(TAG, "DeviceSpecificProvider not initialized");
            return false;
        }

        if (!DeviceSpecificProvider.getInstance().isFirebase()) {
            PWLog.error(TAG, "Device specific is not Firebase");
            return false;
        }

        return true;
    }

    /**
     * Checks whether a Firebase Cloud Messaging message was sent through Pushwoosh.
     * <p>
     * Use this method to determine if an incoming FCM message should be handled by Pushwoosh or by
     * another push notification provider.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
     *     if (PushwooshFcmHelper.isPushwooshMessage(remoteMessage)) {
     *         PushwooshFcmHelper.onMessageReceived(this, remoteMessage);
     *     } else {
     *         // Handle other providers
     *     }
     * }
     * }
     * </pre>
     *
     * @param remoteMessage Firebase Cloud Messaging remote message to check
     * @return true if the message was sent via Pushwoosh; false otherwise
     * @see #onMessageReceived(Context, RemoteMessage)
     */
    public static boolean isPushwooshMessage(RemoteMessage remoteMessage) {
        return RemoteMessageUtils.isPushwooshMessage(remoteMessage);
    }

    /**
     * Converts a Firebase Cloud Messaging RemoteMessage to an Android Bundle.
     * <p>
     * Use this utility method when you need to pass message data to other Android components
     * or implement custom message processing logic.
     *
     * @param remoteMessage Firebase Cloud Messaging remote message to convert
     * @return Bundle containing all data from the remote message
     * @see RemoteMessage
     * @see #onMessageReceived(Context, RemoteMessage)
     */
    public static Bundle messageToBundle(RemoteMessage remoteMessage) {
        return RemoteMessageMapper.mapToBundle(remoteMessage);
    }
}
