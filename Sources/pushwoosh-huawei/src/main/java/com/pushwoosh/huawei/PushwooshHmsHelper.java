package com.pushwoosh.huawei;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.pushwoosh.PushwooshMessagingServiceHelper;
import com.pushwoosh.huawei.internal.mapper.RemoteMessageMapper;
import com.pushwoosh.huawei.internal.registrar.HuaweiPushRegistrar;
import com.pushwoosh.huawei.utils.RemoteMessageUtils;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

import java.util.Map;

import androidx.annotation.Nullable;

/**
 * Helper class for integrating Pushwoosh with Huawei Mobile Services (HMS) Push Kit in custom HmsMessageService implementations.
 * <p>
 * By default, Pushwoosh SDK automatically handles Huawei Mobile Services push notifications without any additional code.
 * However, if your app needs a custom {@link HmsMessageService} (for example, to handle messages from multiple push providers),
 * use this helper class to forward HMS callbacks to Pushwoosh.
 * <p>
 * <b>Important:</b> Use this helper ONLY if you need a custom {@link HmsMessageService}.
 * For customizing notifications, use {@link com.pushwoosh.notification.NotificationServiceExtension} instead.
 * <p>
 * <b>Critical Integration Steps:</b>
 * <ol>
 * <li>Add HMS Core SDK dependencies and configure agconnect-services.json</li>
 * <li>Create your custom {@link HmsMessageService} class</li>
 * <li>Override {@link HmsMessageService#onNewToken(String)} and call {@link #onTokenRefresh(String)}</li>
 * <li>Override {@link HmsMessageService#onMessageReceived(RemoteMessage)} and call {@link #onMessageReceived(Context, RemoteMessage)}</li>
 * <li>Register your service in AndroidManifest.xml</li>
 * </ol>
 * <p>
 * <b>Example - Multiple Push Providers:</b>
 * <pre>
 * {@code
 * public class MyHmsMessageService extends HmsMessageService {
 *
 *     @Override
 *     public void onNewToken(String token) {
 *         super.onNewToken(token);
 *
 *         // CRITICAL: Forward to Pushwoosh to keep receiving notifications
 *         PushwooshHmsHelper.onTokenRefresh(token);
 *
 *         // Forward to other providers if needed
 *         OtherProvider.setToken(token);
 *     }
 *
 *     @Override
 *     public void onMessageReceived(RemoteMessage remoteMessage) {
 *         super.onMessageReceived(remoteMessage);
 *
 *         // CRITICAL: Route Pushwoosh messages to Pushwoosh
 *         if (PushwooshHmsHelper.isPushwooshMessage(remoteMessage)) {
 *             PushwooshHmsHelper.onMessageReceived(this, remoteMessage);
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
 *     android:name=".MyHmsMessageService"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="com.huawei.push.action.MESSAGING_EVENT" />
 *     </intent-filter>
 * </service>
 * }
 * </pre>
 *
 * @see HmsMessageService
 * @see #onTokenRefresh(String)
 * @see #onMessageReceived(Context, RemoteMessage)
 * @see #isPushwooshMessage(RemoteMessage)
 * @see GetTokenAsync
 */
public class PushwooshHmsHelper {
    private static final String TAG = "HmsHelper";
    private static final String APP_ID_MISSING_ERROR = "Missing client/app_id key in " +
            "agconnect-services.json. Make sure you have finished setting up your " +
            "application in Huawei AppGallery Connect and client/app_id key is present in " +
            "agconnect-services.json";

    /**
     * Notifies Pushwoosh when Huawei Mobile Services push token is refreshed.
     * <p>
     * <b>CRITICAL:</b> Call this method from your custom {@link HmsMessageService#onNewToken(String)}
     * callback to forward the new HMS token to Pushwoosh. Without this call, Pushwoosh will NOT be able
     * to send notifications to the device after token refresh.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * public void onNewToken(String token) {
     *     super.onNewToken(token);
     *
     *     // CRITICAL: Forward token to Pushwoosh
     *     PushwooshHmsHelper.onTokenRefresh(token);
     * }
     * }
     * </pre>
     *
     * @param token new Huawei Mobile Services push token (can be null)
     * @see HmsMessageService#onNewToken(String)
     */
    public static void onTokenRefresh(@Nullable String token) {
        if (TextUtils.equals(token, RepositoryModule.getRegistrationPreferences().pushToken().get())) {
            return;
        }
        if (DeviceSpecificProvider.getInstance().pushRegistrar() instanceof HuaweiPushRegistrar) {
            PWLog.debug(TAG, "onTokenRefresh");
            PushwooshMessagingServiceHelper.onTokenRefresh(token);
        }
    }

    /**
     * Processes incoming Huawei Mobile Services push notifications for Pushwoosh.
     * <p>
     * <b>CRITICAL:</b> Call this method from your custom {@link HmsMessageService#onMessageReceived(RemoteMessage)}
     * callback to let Pushwoosh handle its messages. Without this call, Pushwoosh notifications will NOT be displayed.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * public void onMessageReceived(RemoteMessage remoteMessage) {
     *     super.onMessageReceived(remoteMessage);
     *
     *     // CRITICAL: Route Pushwoosh messages to Pushwoosh
     *     if (PushwooshHmsHelper.isPushwooshMessage(remoteMessage)) {
     *         PushwooshHmsHelper.onMessageReceived(this, remoteMessage);
     *     } else {
     *         // Handle other providers
     *     }
     * }
     * }
     * </pre>
     *
     * @param context application or service context
     * @param remoteMessage Huawei Mobile Services remote message
     * @return true if the message was sent via Pushwoosh and was successfully processed; false otherwise
     * @see HmsMessageService#onMessageReceived(RemoteMessage)
     * @see #isPushwooshMessage(RemoteMessage)
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean onMessageReceived(Context context, RemoteMessage remoteMessage) {
        if (!isPushwooshMessage(remoteMessage) || !DeviceSpecificProvider.isInited() ||
                !DeviceSpecificProvider.getInstance().isHuawei()) {
            return false;
        }

        String from = remoteMessage.getFrom();
        Map<String, String> data = remoteMessage.getDataOfMap();
        PWLog.info(TAG, "Received message: " + data.toString() + " from: " + from);
        Bundle pushBundle = RemoteMessageMapper.mapToBundle(remoteMessage);
        return PushwooshMessagingServiceHelper.onMessageReceived(context, pushBundle);
    }

    /**
     * Checks whether a Huawei Mobile Services message was sent through Pushwoosh.
     * <p>
     * Use this method to determine if an incoming HMS message should be handled by Pushwoosh or by
     * another push notification provider.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * public void onMessageReceived(RemoteMessage remoteMessage) {
     *     if (PushwooshHmsHelper.isPushwooshMessage(remoteMessage)) {
     *         PushwooshHmsHelper.onMessageReceived(this, remoteMessage);
     *     } else {
     *         // Handle other providers
     *     }
     * }
     * }
     * </pre>
     *
     * @param remoteMessage Huawei Mobile Services remote message to check
     * @return true if the message was sent via Pushwoosh; false otherwise
     * @see #onMessageReceived(Context, RemoteMessage)
     */
    public static boolean isPushwooshMessage(RemoteMessage remoteMessage) {
        return RemoteMessageUtils.isPushwooshMessage(remoteMessage);
    }

    /**
     * AsyncTask for retrieving Huawei Mobile Services push token asynchronously.
     * <p>
     * Use this class when you need to manually retrieve the HMS push token for advanced scenarios
     * such as manual registration or sending the token to your backend server.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * new PushwooshHmsHelper.GetTokenAsync(new PushwooshHmsHelper.OnGetTokenAsync() {
     *
     *     @Override
     *     public void onGetToken(String token) {
     *         // Token retrieved successfully
     *         PushwooshHmsHelper.onTokenRefresh(token);
     *     }
     *
     *     @Override
     *     public void onError(String error) {
     *         // Token retrieval failed
     *         Log.e("HMS", "Failed: " + error);
     *     }
     * }).execute();
     * }
     * </pre>
     *
     * @see OnGetTokenAsync
     * @see #onTokenRefresh(String)
     */
    public static class GetTokenAsync extends AsyncTask<Void, Void, GetTokenAsync.GetTokenAsyncResult> {
        private final OnGetTokenAsync onGetTokenAsync;

        public GetTokenAsync(OnGetTokenAsync onGetTokenAsync) {
            this.onGetTokenAsync = onGetTokenAsync;
        }

        @Override
        protected GetTokenAsyncResult doInBackground(Void... voids) {
            Context context = AndroidPlatformModule.getApplicationContext();
            if (context == null) {
                return new GetTokenAsyncResult(null, AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
            }
            try {
                String appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id");
                if (TextUtils.isEmpty(appId)) {
                    return new GetTokenAsyncResult(null, APP_ID_MISSING_ERROR);
                } else {
                    return new GetTokenAsyncResult(HmsInstanceId.getInstance(context).getToken(appId, "HCM"), null);
                }
            } catch (ApiException e) {
                String error = "Status code: " + e.getStatusCode() + ". Message: " + e.getMessage();
                return new GetTokenAsyncResult(null, "HCM registration error:" + error);
            }
        }

        @Override
        protected void onPostExecute(GetTokenAsyncResult result) {
            super.onPostExecute(result);
            if (onGetTokenAsync != null) {
                if (TextUtils.isEmpty(result.getToken())) {
                    if (result.getError() != null) {
                        onGetTokenAsync.onError(result.getError());
                    }
                } else {
                    onGetTokenAsync.onGetToken(result.getToken());
                }
            }
        }

        private static class GetTokenAsyncResult {
            private final String token;
            private final String error;

            GetTokenAsyncResult(String token, String error) {
                this.token = token;
                this.error = error;
            }

            String getToken() {
                return token;
            }

            String getError() {
                return error;
            }
        }
    }

    /**
     * Callback interface for asynchronous HMS token retrieval.
     * <p>
     * Implement this interface to receive callbacks when the HMS token is successfully retrieved
     * or when an error occurs during token retrieval.
     *
     * @see GetTokenAsync
     */
    public interface OnGetTokenAsync {
        /**
         * Called when HMS push token is successfully retrieved.
         * <p>
         * This callback is executed on the main thread after the token has been retrieved from
         * Huawei Mobile Services. Use this to store the token, send it to your server, or forward
         * it to Pushwoosh.
         *
         * @param token the retrieved HMS push token
         */
        void onGetToken(String token);

        /**
         * Called when HMS push token retrieval fails.
         * <p>
         * This callback is executed on the main thread if token retrieval encounters an error.
         * Common errors include missing agconnect-services.json configuration or HMS API failures.
         * <p>
         * <b>Common error messages:</b>
         * <ul>
         * <li>"Missing client/app_id key in agconnect-services.json" - Configuration error</li>
         * <li>"Status code: XXXX" - HMS API error (see Huawei documentation for error codes)</li>
         * <li>"Context is null" - Application context not available</li>
         * </ul>
         *
         * @param error error message describing what went wrong
         */
        void onError(String error);
    }
}
