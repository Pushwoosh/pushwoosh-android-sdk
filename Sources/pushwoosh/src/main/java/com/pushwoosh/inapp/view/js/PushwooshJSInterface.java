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

package com.pushwoosh.inapp.view.js;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;

import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.pushwoosh.GDPRManager;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Pushwoosh inApps javascript interface
 * Instance of this class is accessible from inApp javascript sources by using "pushwooshImpl" variable
 */
public class PushwooshJSInterface {
    private static final String TAG = "[InApp]PushwooshJSInterface";

    private final JsCallback jsCallback;
    private final WeakReference<WebView> webView;
    private final View mainContainer;
    private final String customData;
    private final String messageHash;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String richMediaCode;
    private String inAppCode;

    public static final String PUSHWOOSH_JS = ""
                + "(function () {"
                    + "if (window.pushwoosh) return;"
                    + ""
                    + "window._pwCallbackHelper = {"
                    + "    __callbacks: {},"
                    + "    __cbCounter: 0,"
                    + ""
                    + "    invokeCallback: function(cbID) {"
                    + "        var args = Array.prototype.slice.call(arguments);"
                    + "        args.shift();"
                    + ""
                    + "        var cb = this.__callbacks[cbID];"
                    + "        this.__callbacks[cbID] = undefined;"
                    + ""
                    + "        return cb.apply(null, args);"
                    + "    },"
                    + ""
                    + "    registerCallback: function(func) {"
                    + "        var cbID = \"__cb\" + (+new Date) + this.__cbCounter;"
                    + ""
                    + "        this.__cbCounter++;"
                    + "        this.__callbacks[cbID] = func;"
                    + ""
                    + "        return cbID;"
                    + "    }"
                    + "};"
                    + ""
                    + "window.pushwoosh = {"
                    + "    _hwid: \"%s\","
                    + "    _version: \"%s\","
                    + "    _application: \"%s\","
                    + "    _user_id: \"%s\","
                    + "    _richmedia_code: \"%s\","
                    + "    _device_type: \"%s\","
                    + "    _message_hash: \"%s\","
                    + "    _inapp_code: \"%s\","
                    + ""
                    + "    postEvent: function(event, attributes, successCallback, errorCallback) {"
                    + "        if (!attributes) {"
                    + "            attributes = {};"
                    + "        }"
                    + ""
                    + "        if (!successCallback) {"
                    + "            successCallback = function() {};"
                    + "        }"
                    + ""
                    + "        if (!errorCallback) {"
                    + "            errorCallback = function(error) {};"
                    + "        }"
                    + ""
                    + "        var successCbId = _pwCallbackHelper.registerCallback(successCallback);"
                    + "        var errorCbId = _pwCallbackHelper.registerCallback(errorCallback);"
                    + "        pushwooshImpl.postEvent(event, JSON.stringify(attributes), successCbId, errorCbId);"
                    + "    },"
                    + ""
                    + "    richMediaAction: function(inAppCode, richMediaCode, actionType, actionAttributes, successCallback, errorCallback) {"
                    + "        if (!successCallback) {"
                    + "            successCallback = function() {};"
                    + "        }"
                    + ""
                    + "        if (!errorCallback) {"
                    + "            errorCallback = function(error) {};"
                    + "        }"
                    + ""
                    + "        var successCbId = _pwCallbackHelper.registerCallback(successCallback);"
                    + "        var errorCbId = _pwCallbackHelper.registerCallback(errorCallback);"
                    + "        pushwooshImpl.richMediaAction(inAppCode, richMediaCode, actionType, JSON.stringify(actionAttributes), successCbId, errorCbId);"
                    + "    },"
                    + ""
                    + "    sendTags: function(tags) {"
                    + "        pushwooshImpl.sendTags(JSON.stringify(tags));"
                    + "    },"
                    + ""
                    + "    getTags: function(successCallback, errorCallback) {"
                    + "        if (!errorCallback) {"
                    + "            errorCallback = function(error) {};"
                    + "        }"
                    + ""
                    + "        var successCbId = _pwCallbackHelper.registerCallback(function(tagsString) {"
                    + "            console.log(\"tags: \" + tagsString);"
                    + "            successCallback(JSON.parse(tagsString));"
                    + "        });"
                    + ""
                    + "        var errorCbId = _pwCallbackHelper.registerCallback(errorCallback);"
                    + ""
                    + "        pushwooshImpl.getTags(successCbId, errorCbId);"
                    + "    },"
                    + ""
                    + "    isCommunicationEnabled: function() {"
                    + "        return pushwooshImpl.isCommunicationEnabled();"
                    + "    },"
                    + ""
                    + "    setCommunicationEnabled: function(enabled) {"
                    + "        pushwooshImpl.setCommunicationEnabled(enabled);"
                    + "    },"
                    + ""
                    + "    removeAllDeviceData: function() {"
                    + "        pushwooshImpl.removeAllDeviceData();"
                    + "    },"
                    + ""
                    + "    log: function(str) {"
                    + "        pushwooshImpl.log(str);"
                    + "    },"
                    + ""
                    + "    closeInApp: function() {"
                    + "        pushwooshImpl.closeInApp();"
                    + "    },"
                    + ""
                    + "    getHwid: function() {"
                    + "        return this._hwid;"
                    + "    },"
                    + ""
                    + "    getVersion: function() {"
                    + "        return this._version;"
                    + "    },"
                    + ""
                    + "    getApplication: function() {"
                    + "        return this._application;"
                    + "    },"
                    + ""
                    + "    getUserId: function() {"
                    + "        return this._user_id;"
                    + "    },"
                    + ""
                    + "    getRichmediaCode: function() {"
                    + "        return this._richmedia_code;"
                    + "    },"
                    + ""
                    + "    getDeviceType: function() {"
                    + "        return this._device_type;"
                    + "    },"
                    + ""
                    + "    getMessageHash: function() {"
                    + "        return this._message_hash;"
                    + "    },"
                    + ""
                    + "    getInAppCode: function() {"
                    + "        return this._inapp_code;"
                    + "    },"
                    + ""
                    + "    getCustomData: function() {"
                    + "         var customData = pushwooshImpl.getCustomData();"
                    + "         if (customData) {"
                    + "             return JSON.parse(customData);"
                    + "         } else {"
                    + "             return null;"
                    + "         }"
                    + "    },"
                    + ""
                    + "    registerForPushNotifications: function() {"
                    + "        pushwooshImpl.registerForPushNotifications();"
                    + "    },"
                    + ""
                    + "    openAppSettings: function() {"
                    + "        pushwooshImpl.openAppSettings();"
                    + "    },"
                    + "    getChannels: function(callback) {"
                    + "        var clb = _pwCallbackHelper.registerCallback(function(channels) {"
                    + "             callback(JSON.parse(channels));"
                    + "        });"
                    + ""
                    + "        pushwooshImpl.getChannels(clb);"
                    + "    },"
                    + "    unregisterForPushNotifications: function(callback) {"
                    + "        var clb = _pwCallbackHelper.registerCallback(callback);"
                    + "        pushwooshImpl.unregisterForPushNotifications(clb);"
                    + "    },"
                    + "    isRegisteredForPushNotifications: function(callback) {"
                    + "        var clb = _pwCallbackHelper.registerCallback(function(state) {"
                    + "           if (state == 'true') {callback(true);} else if (state == 'false') {callback(false);}"
                    + "        });"
                    + ""
                    + "        pushwooshImpl.isRegisteredForPushNotifications(clb);"
                    + "    }"
                    + "};"
                    + "}());";


    public PushwooshJSInterface(JsCallback jsCallback, WebView webView, @Nullable View mainContainer, @Nullable String customData, @Nullable String messageHash) {
        this.jsCallback = jsCallback;
        this.webView = new WeakReference<>(webView);
        this.mainContainer = mainContainer;
        this.customData = customData;
        this.messageHash = messageHash;
    }

    public void onPageStarted(WebView webView, Resource resource) {
        String url = String.format("javascript:" + PUSHWOOSH_JS,
                Pushwoosh.getInstance().getHwid(),
                GeneralUtils.SDK_VERSION,
                Pushwoosh.getInstance().getApplicationCode(),
                Pushwoosh.getInstance().getUserId(),
                !resource.isInApp() ? resource.getCode().substring(2) : "",
                DeviceSpecificProvider.getInstance().deviceType(),
                this.messageHash != null ? this.messageHash : "",
                resource.isInApp() ? resource.getCode() : ""
        );
        richMediaCode = !resource.isInApp() ? resource.getCode().substring(2) : "";
        inAppCode = resource.isInApp() ? resource.getCode() : "";

        webView.loadUrl(url);
    }

    // webView.loadUrl onPageStarted may fail on first start
    public void onPageFinished(WebView webView, Resource resource) {
        String url = String.format("javascript:" + PUSHWOOSH_JS,
                Pushwoosh.getInstance().getHwid(),
                GeneralUtils.SDK_VERSION,
                Pushwoosh.getInstance().getApplicationCode(),
                Pushwoosh.getInstance().getUserId(),
                !resource.isInApp() ? resource.getCode().substring(2) : "",
                DeviceSpecificProvider.getInstance().deviceType(),
                this.messageHash != null ? this.messageHash : "",
                resource.isInApp() ? resource.getCode() : ""
        );
        webView.loadUrl(url);
    }

    @JavascriptInterface
    public void setCommunicationEnabled(boolean enabled) {
        GDPRManager.getInstance().setCommunicationEnabled(enabled, null);
    }

    @JavascriptInterface
    public void removeAllDeviceData() {
        GDPRManager.getInstance().removeAllDeviceData(null);
    }

    @JavascriptInterface
    public boolean isCommunicationEnabled() {
        return GDPRManager.getInstance().isCommunicationEnabled();
    }

    @JavascriptInterface
    public boolean isDeviceDataRemoved() {
        return GDPRManager.getInstance().isDeviceDataRemoved();
    }

    @JavascriptInterface
    public void postEvent(String event, String attributesStr, String successCb, String errorCb) {
        try {
            JSONObject attributesJson = new JSONObject(attributesStr);
            TagsBundle attributes = Tags.fromJson(attributesJson);

            PushwooshPlatform.getInstance().pushwooshInApp().postEvent(event, attributes, result -> {
                if (result.isSuccess()) {
                    invokeCallback(successCb);
                } else {
                    invokeCallback(errorCb, result.getException().getLocalizedMessage());
                }
            });
        } catch (Exception e) {
            PWLog.error("postEvent method was failed", e);
            invokeCallback(errorCb, e.getLocalizedMessage());
        }
    }

    @JavascriptInterface
    public void richMediaAction(String inappCode, String richmediaCode, int actionType, String actionAttributes, String successCb, String errorCb) {
        try {
            PushwooshPlatform.getInstance().pushwooshInApp().sendRichMediaAction(richmediaCode, inappCode, messageHash, actionAttributes, actionType, result -> {
                if (result.isSuccess()) {
                    invokeCallback(successCb);
                } else {
                    invokeCallback(errorCb, result.getException().getLocalizedMessage());
                }
            });
        } catch (Exception e) {
            PWLog.error("failed to send /richMediaAction request:", e.getMessage());
            invokeCallback(errorCb, e.getLocalizedMessage());
        }
    }

    private void loadUrl(String url) {
        WebView webView = this.webView.get();
        if (webView != null) {
            webView.loadUrl(url);
        }
    }

    private void invokeCallback(String successCb) {
        String url = String.format("javascript:_pwCallbackHelper.invokeCallback(\"%s\");", successCb);
        mainHandler.post(() -> loadUrl(url));
    }

    private void invokeCallback(String method, String args) {
        String encodedArg = args.replace("\"", "\\\"");
        String url = String.format("javascript:_pwCallbackHelper.invokeCallback(\"%s\", \"%s\");", method, encodedArg);

        mainHandler.post(() -> loadUrl(url));
    }

    @JavascriptInterface
    public void sendTags(String tags) {
        try {
            JSONObject tagsJSObject = new JSONObject(tags);
            TagsBundle tagsBundle = new TagsBundle.Builder()
                    .putAll(tagsJSObject)
                    .build();
            Pushwoosh.getInstance().sendTags(tagsBundle);
        } catch (JSONException e) {
            PWLog.error("Invalid tags format, expected object with string properties", e);
        }
    }

    @JavascriptInterface
    public void getTags(String successCb, String errorCb) {
        Pushwoosh.getInstance().getTags(result -> {
            if (result.isSuccess()) {
                final TagsBundle data = result.getData();
                JSONObject tagsJson = data == null ? new JSONObject() : data.toJson();
                invokeCallback(successCb, tagsJson.toString());
            } else {
                invokeCallback(errorCb, result.getException().getMessage());
            }
        });
    }

    @JavascriptInterface
    public void closeInApp() {
        richMediaAction(inAppCode,richMediaCode, 4,null,null,null);
        jsCallback.close();
    }

    @JavascriptInterface
    public void log(String str) {
        PWLog.debug(TAG, str);
    }
    /**
     * send /registerDevice request
     * <p>
     * js example:
     * <pre>
     * {@code
     *    pushManager.registerForPushNotifications();
     * }
     * </pre>
     */
    @JavascriptInterface
    public void registerForPushNotifications() {
        Pushwoosh.getInstance().registerForPushNotifications();
    }

    @JavascriptInterface
    public void openAppSettings() {
        if (AndroidPlatformModule.getInstance() == null)
            return;

        //from https://stackoverflow.com/questions/32366649/any-way-to-link-to-the-android-notification-settings-for-my-app

        Context context = AndroidPlatformModule.getApplicationContext();
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");

        //for Android 5-7
        intent.putExtra("app_package", context.getPackageName());
        intent.putExtra("app_uid", context.getApplicationInfo().uid);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        // for Android O
        intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());

        context.startActivity(intent);
    }

    @JavascriptInterface
    public String getCustomData() {
        return customData;
    }

    @JavascriptInterface
    public void unregisterForPushNotifications(String callback) {
        Pushwoosh.getInstance().unregisterForPushNotifications((result) -> {
            if (result.getException() == null) {
                invokeCallback(callback);
            } else {
                invokeCallback(callback, result.getException().getMessage());
            }
        });
    }

    @JavascriptInterface
    public void isRegisteredForPushNotifications(String callback) {
        boolean registered = false;
        try {
            registered = PushwooshPlatform.getInstance().getRegistrationPrefs().isRegisteredForPush().get();
        } catch (Exception ignore) {}
        invokeCallback(callback, registered ? "true" : "false");
    }
}
