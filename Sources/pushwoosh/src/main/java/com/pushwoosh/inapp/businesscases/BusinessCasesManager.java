/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.inapp.businesscases;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.core.app.NotificationManagerCompat;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.utils.AppVersionProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.TimeProvider;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by kai on 31.01.2018.
 */

public class BusinessCasesManager {

    public static final String TAG = BusinessCasesManager.class.getSimpleName();

    public static final String WELCOME_CASE = "welcome-inapp";
    public static final String APP_UPDATE_CASE = "app-update-message";
    public static final String PUSH_RECOVER_CASE = "push-unregister";
    public static final int NO_CAPPING = 0;
    public static final float DEFAULT_CAPPING = 1F;
    public static final String KEY_COM_PUSHWOOSH_BUSINESS_CASES_FREQUENCY_CAPPING = "com.pushwoosh.in_app_business_solutions_capping";

    public static final String WRONG_FORMAT_CAPPING_MESSAGE = "wrong format capping, capping must be positive number";

    private Map<String, BusinessCase> businessCases;
    private SharedPreferences prefs;
    private HandlerThread backgroundThread;

    public BusinessCasesManager(PrefsProvider prefsProvider,
                                AppInfoProvider appInfoProvider,
                                TimeProvider timeProvider,
                                AppVersionProvider appVersionProvider) {
        float cappingCountDay = getCappingCountDay(appInfoProvider);

        backgroundThread = new HandlerThread("BusinessCasesThread");
        backgroundThread.start();

        prefs = prefsProvider.providePrefs("PWBusinessCasesState");
        if (prefs == null) {
            return;
        }

        businessCases = new HashMap<>();
        businessCases.put(WELCOME_CASE, new BusinessCase(WELCOME_CASE, NO_CAPPING, prefs, appVersionProvider::getFirstLaunchAndDropValue, timeProvider));
        businessCases.put(APP_UPDATE_CASE, new BusinessCase(APP_UPDATE_CASE, NO_CAPPING, prefs, appVersionProvider::getFirstLaunchAfterUpdateAndDropValue, timeProvider));
        businessCases.put(PUSH_RECOVER_CASE, new BusinessCase(PUSH_RECOVER_CASE, cappingCountDay, prefs, this::isDisableNotification, timeProvider));
    }

    private float getCappingCountDay(AppInfoProvider appInfoProvider) {
        ApplicationInfo applicationInfo = appInfoProvider.getApplicationInfo();
        Bundle metaData = applicationInfo.metaData;
        if (metaData == null) {
            return DEFAULT_CAPPING;
        }

        Object capping = metaData.get(KEY_COM_PUSHWOOSH_BUSINESS_CASES_FREQUENCY_CAPPING);
        if (capping == null) {
            return DEFAULT_CAPPING;
        }
        float cappingCountDay;
        if (capping instanceof Integer) {
            cappingCountDay = ((Integer) capping).floatValue();
        } else if (capping instanceof Float) {
            cappingCountDay = (float) capping;
        } else {
            cappingCountDay = DEFAULT_CAPPING;
            PWLog.error(TAG, WRONG_FORMAT_CAPPING_MESSAGE);
        }
        if (cappingCountDay < 0) {
            PWLog.error(TAG, WRONG_FORMAT_CAPPING_MESSAGE);
            cappingCountDay = DEFAULT_CAPPING;
        }
        PWLog.noise(TAG, "set Up capping:" + cappingCountDay);
        return cappingCountDay;
    }

    private boolean isPushTokenEmpty() {
        return PushwooshPlatform.getInstance().notificationManager().getPushToken() == null;
    }

    private boolean isDisableNotification() {
        Context context = AndroidPlatformModule.getApplicationContext();
        return context != null
                && !NotificationManagerCompat.from(context).areNotificationsEnabled()
                && PushwooshPlatform.getInstance().notificationManager().getPushToken() != null;
    }

    public void triggerCase(String uid, BusinessCase.BusinessCaseCallback callback) {
        BusinessCase businessCase = businessCases.get(uid);
        if (businessCase != null) {
            new Handler(backgroundThread.getLooper()).post(() -> businessCase.trigger(callback));
        }
    }

    public static void processInAppsData(List<Resource> data) {
        try {
            Map<String, BusinessCaseData> businessCasesData = new HashMap<>();
            for (Resource resource : data) {
                if (resource.getBusinessCase() != null && !resource.getBusinessCase().isEmpty()) {
                    businessCasesData.put(resource.getBusinessCase(), BusinessCaseData.fromResource(resource));
                }
            }
            PushwooshPlatform.getInstance().getBusinessCasesManager().processBusinessCasesData(businessCasesData, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void processBusinessCasesData(JSONObject data) {
        if (data == null)
            return;
        try {
            Map<String, BusinessCaseData> businessCasesData = new HashMap<>();

            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject caseData = data.optJSONObject(key);
                businessCasesData.put(key, BusinessCaseData.fromJSON(caseData));
            }

            PushwooshPlatform.getInstance().getBusinessCasesManager().processBusinessCasesData(businessCasesData, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processBusinessCasesData(Map<String, BusinessCaseData> dataMap, boolean downloadReady) {
        for (BusinessCase businessCase : businessCases.values()) {
            BusinessCaseData data = dataMap.get(businessCase.getUid());
            if (data == null)
                continue;
            if (downloadReady) {
                businessCase.setInAppId(data.getInAppCode());
            } else {
                InAppStorage storage = InAppModule.getInAppStorage();
                if (storage != null) {
                    Resource resource = storage.getResource(data.getInAppCode());
                    if (resource != null && resource.getUpdated() == data.getUpdated()) {
                        businessCase.setInAppId(data.getInAppCode());
                    }
                }
            }
        }
    }

    public void resetBusinessCasesFrequencyCapping() {
        prefs
                .edit()
                .clear()
                .apply();
    }


}
