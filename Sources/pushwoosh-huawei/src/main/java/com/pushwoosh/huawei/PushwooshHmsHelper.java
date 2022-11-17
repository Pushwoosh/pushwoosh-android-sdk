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

public class PushwooshHmsHelper {
    private static final String TAG = "HmsHelper";
    private static final String APP_ID_MISSING_ERROR = "Missing client/app_id key in " +
            "agconnect-services.json. Make sure you have finished setting up your " +
            "application in Huawei AppGallery Connect and client/app_id key is present in " +
            "agconnect-services.json";

    /**
     * if you use custom {@link HmsMessageService}
     * call this method when {@link HmsMessageService#onNewToken(String token)} is invoked
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
     * if you use custom {@link HmsMessageService}
     * call this method when {@link HmsMessageService#onMessageReceived(RemoteMessage)} is invoked
     *
     * @return true if the remoteMessage was sent via Pushwoosh and was successfully processed; otherwise false
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
     * Check if the remoteMessage was sent via Pushwoosh
     *
     * @return true if remoteMessage was sent via Pushwoosh
     */
    public static boolean isPushwooshMessage(RemoteMessage remoteMessage) {
        return RemoteMessageUtils.isPushwooshMessage(remoteMessage);
    }

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

    public interface OnGetTokenAsync {
        void onGetToken(String token);
        void onError(String error);
    }
}
