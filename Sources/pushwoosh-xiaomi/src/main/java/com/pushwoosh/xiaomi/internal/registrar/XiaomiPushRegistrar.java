package com.pushwoosh.xiaomi.internal.registrar;

import static com.pushwoosh.internal.platform.AndroidPlatformModule.getApplicationContext;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;

import androidx.annotation.Nullable;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.tags.TagsBundle;
import com.xiaomi.channel.commonutils.android.Region;
import com.xiaomi.channel.commonutils.logger.LoggerInterface;
import com.xiaomi.mipush.sdk.Logger;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.util.List;

public class XiaomiPushRegistrar implements PushRegistrar {

    private Impl impl;
    @Override
    public void init() {
        impl = new Impl();
        impl.registerPW(null);

        //Enable Log in case of errors
        LoggerInterface newLogger = new LoggerInterface() {
            private final String TAG = "XMPush";
            @Override
            public void setTag(String tag) {
            }
            @Override
            public void log(String content, Throwable t) {
                android.util.Log.d(TAG, content, t);
            }
            @Override
            public void log(String content) {
                android.util.Log.d(TAG, content);
            }
        };
        Logger.setLogger(getApplicationContext(), newLogger);

    }

    @Override
    public void checkDevice(String appId) throws Exception {
        impl.checkDevice(appId);
    }

    @Override
    public void registerPW(TagsBundle tags) {
        impl.registerPW(tags);
    }

    @Override
    public void unregisterPW() {
        impl.unregisterPW();
    }

    private static class Impl {
        private static final String TAG = "PushRegistrarXM";

        @Nullable
        private final Context context;
        private final RegistrationPrefs registrationPrefs;

        private Impl() {
            context = AndroidPlatformModule.getApplicationContext();
            registrationPrefs = RepositoryModule.getRegistrationPreferences();
        }

        void checkDevice(final String appId) throws Exception {
            GeneralUtils.checkNotNullOrEmpty(appId, "mAppId");
        }

        void registerPW(TagsBundle tagsBundle) {
            MiPushClient.setRegion(getAppRegionFromPrefs());
            if(isMainProcess()) {
                if (context != null ) {
                    final String XM_APP_ID = registrationPrefs.xiaomiAppId().get();
                    final String XM_APP_KEY = registrationPrefs.xiaomiAppKey().get();
                    MiPushClient.registerPush(context, XM_APP_ID, XM_APP_KEY);
                }
            }
        }

        void unregisterPW() {
            if(isMainProcess()) {
                if (context != null ) {
                    String token = RepositoryModule.getRegistrationPreferences().pushToken().get();
                    NotificationRegistrarHelper.onUnregisteredFromRemoteNotifications(token);
                    MiPushClient.unregisterPush(context);

                }
            }
        }

        //Judge if it's in the main process
        private boolean isMainProcess() {
            if (context != null ) {
            ActivityManager am =
                    ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
            List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();

                String mainProcessName = context.getApplicationInfo().processName;
                int myPid = Process.myPid();
                for (ActivityManager.RunningAppProcessInfo info : processInfos) {
                    if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static Region getAppRegionFromPrefs() {
        final String regionPref = RepositoryModule.getRegistrationPreferences().xiaomiAppRegion().get().toLowerCase();
        PWLog.info("Xiaomi region is: "+regionPref);
        Region result;
        switch (regionPref) {
            case "europe": {
                result = Region.Europe;
                break;
            }
            case "russia": {
                result = Region.Russia;
                break;
            }
            case "india": {
                result = Region.India;
                break;
            }
            default: {
                result = Region.Global;
                break;
            }
        }
        return result;
    }
}
