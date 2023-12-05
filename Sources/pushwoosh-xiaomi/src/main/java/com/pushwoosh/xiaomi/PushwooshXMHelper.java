package com.pushwoosh.xiaomi;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.pushwoosh.PushwooshMessagingServiceHelper;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.xiaomi.internal.registrar.XiaomiPushRegistrar;
import com.pushwoosh.xiaomi.internal.specific.XiaomiDeviceSpecificIniter;
import com.xiaomi.mipush.sdk.MiPushMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PushwooshXMHelper {
    private static final String TAG = "XMHelper";

    public static void onTokenRefresh(@Nullable String token) {
        if (TextUtils.equals(token, RepositoryModule.getRegistrationPreferences().pushToken().get())) {
            return;
        }
        if (DeviceSpecificProvider.getInstance().pushRegistrar() instanceof XiaomiPushRegistrar) {
            PWLog.debug(TAG, "onTokenRefresh");
            PushwooshMessagingServiceHelper.onTokenRefresh(token);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean onMessageReceived(Context context, MiPushMessage remoteMessage) {
        try {
            if (DeviceSpecificProvider.getInstance() == null) {
                new DeviceSpecificProvider.Builder()
                        .setDeviceSpecific(XiaomiDeviceSpecificIniter.create(new XiaomiPushRegistrar()))
                        .build(true);
            }

            if (!isPushwooshMessage(remoteMessage) || !DeviceSpecificProvider.getInstance().isXiaomi()) {
                return false;
            }
        } catch (NullPointerException e) {
            PWLog.error("Xiaomi provider is not initialized, unsafe to handle received push");
            return false;
        }

        if (isPushwooshMessage(remoteMessage) || DeviceSpecificProvider.isInited() ||
                DeviceSpecificProvider.getInstance().isXiaomi()) {
            Bundle messageBundle = miPushMessageToBundle(remoteMessage);
            return PushwooshMessagingServiceHelper.onMessageReceived(context, messageBundle);
        }

        return false;
    }

    private static Bundle miPushMessageToBundle(MiPushMessage miPushMessage) {
        String payload = miPushMessage.getContent();
        Bundle bundle = new Bundle();
        try {
            JSONObject jsonObject = new JSONObject(payload);
            Map<String, String> payloadMap = new HashMap<>();

            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);

                payloadMap.put(key,value.toString());
            }

            for (Map.Entry<String, String> entry : payloadMap.entrySet()) {
                bundle.putString(entry.getKey(),entry.getValue());
            }

            return bundle;

        } catch (JSONException e) {
            PWLog.error("Failed to parse MiPush message", e);
            return new Bundle();
        }
    }

    private static boolean isPushwooshMessage(MiPushMessage remoteMessage) {
        try {
            JSONObject jsonObject = new JSONObject(remoteMessage.getContent());
            return jsonObject.optInt("pw_msg") != 0;
        } catch (JSONException e) {
            PWLog.error("Failed to parse MiPush message", e);
            return false;
        }
    }
}