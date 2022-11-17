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

import java.lang.ref.WeakReference;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.pushwoosh.GDPRManager;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * PushManager inApps javascript interface
 * Instance of this class is accessible from inApp javascript sources by using "pushManager" variable
 */
public class PushManagerJSInterface {
    private static final String TAG = "[InApp]PushManagerJSInterface";

    private static final String EVENT_NAME = "event";
    private static final String ATTRIBUTES = "attributes";
    private static final String SUCCESS_CALLBACK = "success";
    private static final String ERROR_CALLBACK = "error";

    private final WeakReference<WebView> webView;
    private final JsCallback jsCallback;

    public PushManagerJSInterface(WebView webView, JsCallback jsCallback) {
        this.webView = new WeakReference<>(webView);
        this.jsCallback = jsCallback;
    }

    /**
     * Send /postEvent request
     * <p>
     * js example:
     * <pre>
     * {@code
     * var successCallback = function () {
     * 		console.log("Post event success");
     * }
     * var errorCallback = function (message) {
     * 		console.log("Post event failed: ", message);
     * 		alert("Post event failed: " + message);
     * }
     * pushManager.postEvent(JSON.stringify({
     * 			"event" : "testEvent",
     * 			"attributes" : {
     * 				"TestAttributeString" : "testString",
     * 				"TestAttributeInt" : 42,
     * 				"TestAttributeList" : [ 123, 456, "someString" ],
     * 				"TestAttributeBool" : true,
     * 				"TestAttributeNull" : null,
     * 				"TestAttributeDaysAgo" : 7,
     * 				"TestAttributeDate" : today
     * 			};,
     * 			"success" : "successCallback", 	// optional
     * 			"error" : "errorCallback" 		// optional
     * 	}));
     * }
     * </pre>
     *
     * @param event      Event name for postEvent request
     * @param attributes Dictionary with attributes to send
     * @param success    Success callback function name (optional)
     * @param error      Error callback function name (optional)
     */
    @JavascriptInterface
    public void postEvent(String blob) {
        try {
            JSONObject blobJSObject = new JSONObject(blob);
            String event = blobJSObject.getString(EVENT_NAME);
            JSONObject attributes = blobJSObject.getJSONObject(ATTRIBUTES);
            String successCallback = null;
            String errorCallback = null;
            try {
                successCallback = blobJSObject.getString(SUCCESS_CALLBACK);
            } catch (JSONException e) {
                // success property is optional
            }

            try {
                errorCallback = blobJSObject.getString(ERROR_CALLBACK);
            } catch (JSONException e) {
                // error property are optional
            }

            final String successCallbackFinal = successCallback;
            final String errorCallbackFinal = errorCallback;
            PushwooshPlatform.getInstance().pushwooshInApp().postEvent(event, Tags.fromJson(attributes), result -> {
                if (successCallbackFinal != null) {
                    WebView webView = this.webView.get();
                    if (webView != null) {
                        if (result.isSuccess()) {
                            String url = String.format("javascript:%s();", successCallbackFinal);
                            webView.loadUrl(url);
                        } else {
                            String url = String.format("javascript:%s('%s');", errorCallbackFinal, result.getException().getMessage());
                            webView.loadUrl(url);
                        }
                    }
                }
            });
        } catch (JSONException e) {
            PWLog.error("Invalid arguments", e);
        }
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

    /**
     * Closes current In-App
     * <p>
     * js example:
     * <pre>
     * {@code
     *    pushManager.closeInApp();
     * }
     * </pre>
     */
    @JavascriptInterface
    public void closeInApp() {
        jsCallback.close();
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
        if (PushwooshPlatform.getInstance() != null)
            PushwooshPlatform.getInstance().notificationManager().registerForPushNotifications(null,true);
    }

    /**
     * send /sendTags request
     * <p>
     * js example:
     * <pre>
     * {@code
     *    pushManager.sendTags(JSON.stringify({
     *    	"IntTag" : 42,
     *    	"BoolTag" : true,
     *    	"StringTag" : "Test String",
     *    	"ListTag" : ["string1", "string2"]
     *    }));
     * }
     * </pre>
     */
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
    public void log(String str) {
        PWLog.debug(TAG, str);
    }
}
