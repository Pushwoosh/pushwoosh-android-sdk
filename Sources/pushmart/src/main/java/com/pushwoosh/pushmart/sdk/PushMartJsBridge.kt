package com.pushwoosh.pushmart.sdk

import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * Native object exposed to rich media HTML as `window.PushMart`, as iOS `PushMartJSBridge`. Calls
 * arrive on the WebView bridge thread, and JS may pass null.
 */
class PushMartJsBridge {
    @JavascriptInterface fun execute(): String = """{"app":"PushMart"}"""

    @JavascriptInterface
    fun executeWithParam(param: String?): String = JSONObject().put("echo", param ?: "").toString()
}
