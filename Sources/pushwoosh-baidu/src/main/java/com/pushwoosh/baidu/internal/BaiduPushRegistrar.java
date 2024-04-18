/*
 *
 * Copyright (c) 2019. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.baidu.internal;

import android.Manifest;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.pushwoosh.BootReceiver;
import com.pushwoosh.baidu.Utils;
import com.pushwoosh.baidu.internal.event.BaiduPermissionEvent;
import com.pushwoosh.baidu.internal.utils.BaiduPermissionActivity;
import com.pushwoosh.baidu.prefs.BaiduPrefs;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.RequestPermissionHelper;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.tags.TagsBundle;

import java.util.List;

public class BaiduPushRegistrar implements PushRegistrar, EventListener<BaiduPermissionEvent> {
    private static final String TAG = "[BaiduPushRegistrar]";

    private Context context;
    private BaiduPrefs baiduPrefs;
    private String manifestApiKey;

    public BaiduPushRegistrar(Context context) {
        this.context = context;
        this.baiduPrefs = new BaiduPrefs(context);

        manifestApiKey = Utils.getMetaValue(context, "com.pushwoosh.baidu_api_key");
        EventBus.subscribe(BootReceiver.DeviceBootedEvent.class, new EventListener<BootReceiver.DeviceBootedEvent>() {
            @Override
            public void onReceive(BootReceiver.DeviceBootedEvent event) {
                init();
            }
        });
    }

    @Override
    public void init() {
        if (NotificationRegistrarHelper.isRegisteredForRemoteNotifications()) {
            registerBaidu();
        }
    }

    @Override
    public void checkDevice(String appId) throws Exception {
    }

    @Override
    public void registerPW(TagsBundle tags) {
        if(Utils.checkPermission(context)){
            registerBaidu();
        } else {
            requestPermissions();
        }
    }

    private void requestPermissions() {
        RequestPermissionHelper.requestPermissionsForClass(BaiduPermissionActivity.class, context, new String[]{Manifest.permission.READ_PHONE_STATE});
        EventBus.subscribe(BaiduPermissionEvent.class, this);
    }

    //EventListener<BaiduPermissionEvent>
    @Override
    public void onReceive(BaiduPermissionEvent event) {
        List<String> grantedPermissions = event.getGrantedPermissions();
        if (grantedPermissions.contains( Manifest.permission.READ_PHONE_STATE )){
            registerBaidu();
        } else {
            String errorString = "Permissions READ_PHONE_STATE not granted";
            PWLog.error(TAG, errorString);
            NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(errorString);
        }
        EventBus.unsubscribe(BaiduPermissionEvent.class, this);
    }

    private void registerBaidu() {
        String apiKey = !TextUtils.isEmpty(manifestApiKey) ? manifestApiKey : getApiKeyFromProjectId();

        try {
            PushManager.startWork(context, PushConstants.LOGIN_TYPE_API_KEY, apiKey);
            if (baiduPrefs.isFirstStart().get()) {
                baiduPrefs.isFirstStart().set(false);
                restartPushManager(apiKey);
            }
        } catch (Exception e) {
            PWLog.noise(TAG, e);
        }
    }

    private void restartPushManager(String apiKey) {
        PushManager.stopWork(context);
        new Handler(AndroidPlatformModule.getApplicationContext().getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                PushManager.startWork(context, PushConstants.LOGIN_TYPE_API_KEY, apiKey);
            }
        }, 5000);
    }

    @Override
    public void unregisterPW() {
        try {
            PushManager.stopWork(context);
        } catch (Exception e) {
            PWLog.noise(TAG, e);
        }
    }

    private String getApiKeyFromProjectId() {
        return RepositoryModule.getRegistrationPreferences().projectId().get();
    }
}
