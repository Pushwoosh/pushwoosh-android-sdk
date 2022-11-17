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

package com.pushwoosh.baidu;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.pushservice.PushMessageReceiver;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;

import java.util.List;

public class BaiduPushMessageReceiver extends PushMessageReceiver {

    private static final String TAG = BaiduPushMessageReceiver.class.getSimpleName();


    private static String stringFromErrorCode(int errorCode) {
        switch (errorCode) {
            case 0: return "Success";
            case 10001: return "Network Problem";
            case 10002: return "Service not available";
            case 10003: return "Service not available temporary";
            case 10101: return "Integration error, please check the declarations and permissions";
            case 30600: return "Internal Server Error";
            case 30601: return "Method Not Allowed";
            case 30602: return "Request Params Not Valid";
            case 30603: return "Authentication Failed";
            case 30604: return "Quota Use Up Payment Required";
            case 30605: return "Data Required Not Found";
            case 30606: return "Request Time Expires Timeout";
            case 30607: return "Channel Token Timeout";
            case 30608: return "Bind Relation Not Found";
            case 30609: return "Bind Number Too Many";
            case 30610: return "Duplicate Operation";
            case 30611: return "Group Not Found";
            case 30612: return "Application Forbidden, Need Whitelist Authorization";
            case 30613: return "App Need Inied First In Push-console";
            case 30614: return "Number Of Tag For User Too Many";
            case 30615: return "Number Of Tag For App Too Many";
            case 30699: return "Requests Are Too Frequent To Be Temporarily Rejected";
            case 110001: return "User blacked this app";
            default: return "Unknown error. Error code: " + errorCode;
        }
    }

    @Override
    public void onBind(Context context, int errorCode, String appid,
                       String userId, String channelId, String requestId) {
        String responseString = "onBind errorCode=" + errorCode + " appid="
                + appid + " userId=" + userId + " channelId=" + channelId
                + " requestId=" + requestId;
        Log.d(TAG, responseString);

        if (errorCode == 0) {
            PWLog.noise(TAG, "success onBind");
            NotificationRegistrarHelper.onRegisteredForRemoteNotifications(channelId);
        } else {
            String errorMessage = "Baidu registartion failed. " + stringFromErrorCode(errorCode);
            PWLog.noise(TAG, errorMessage);
            NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(errorMessage);
        }
    }


    @Override
    public void onMessage(Context context, String message, String customContentString) {
        String messageString = "onMessage=\"" + message
                + "\" customContentString=" + customContentString;
        PWLog.noise(TAG, messageString);

        handleMessage(message, customContentString);
    }

    private void handleMessage(String description, String customContentString) {
        try {
            Bundle bundle;
            if (TextUtils.isEmpty(customContentString)) {
                bundle = getBundle(description);
            } else {
                bundle = getBundle(customContentString);
            }
            NotificationRegistrarHelper.handleMessage(bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bundle getBundle(String message) {
        return JsonUtils.jsonStringToBundle(message, true);
    }

    @Override
    public void onNotificationArrived(Context context, String title,
                                      String description, String customContentString) {
        PWLog.noise(TAG, "onNotificationArrived  title=\"" + title + "\" description=\""
                + description + "\" customContent=" + customContentString);

        handleMessage(description, customContentString);
    }

    @Override
    public void onNotificationClicked(Context context, String title,
                                      String description, String customContentString) {
        PWLog.noise(TAG, "onNotificationClicked title=\"" + title + "\" description=\""
                + description + "\" customContent=" + customContentString);
    }

    @Override
    public void onSetTags(Context context, int errorCode,
                          List<String> successTags, List<String> failTags, String requestId) {
        PWLog.noise(TAG, "onSetTags errorCode=" + errorCode + " successTags=" + successTags
                + " failTags=" + failTags + " requestId=" + requestId);
    }


    @Override
    public void onDelTags(Context context, int errorCode,
                          List<String> successTags, List<String> failTags, String requestId) {
        PWLog.noise(TAG, "onDelTags errorCode=" + errorCode + " successTags=" + successTags
                + " failTags=" + failTags + " requestId=" + requestId);
    }



    @Override
    public void onListTags(Context context, int errorCode, List<String> tags,
                           String requestId) {
        PWLog.noise(TAG, "onListTags errorCode=" + errorCode + " tags=" + tags);
    }

    @Override
    public void onUnbind(Context context, int errorCode, String requestId) {
        String responseString = "onUnbind errorCode=" + errorCode
                + " requestId = " + requestId;
        Log.d(TAG, responseString);

        if (errorCode == 0) {
            PWLog.noise(TAG, "success onUnbind");
            NotificationRegistrarHelper.onUnregisteredFromRemoteNotifications(Pushwoosh.getInstance().getPushToken());
        } else {
            String errorMessage = "Baidu unregistration failed. " + stringFromErrorCode(errorCode);
            PWLog.noise(TAG, errorMessage);
            NotificationRegistrarHelper.onFailedToUnregisterFromRemoteNotifications(errorMessage);
        }
    }
}
