package com.pushwoosh.inline.internal.html

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.pushwoosh.inapp.view.js.JsCallback
import com.pushwoosh.inapp.view.js.PushManagerJSInterface
import com.pushwoosh.inapp.view.js.PushwooshJSInterface
import com.pushwoosh.internal.utils.PWLog
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Runs a bridge call on the main thread while the banner is shown; the page calls its bridges on a
 * WebView thread.
 */
internal typealias InlineMainRunner = (task: () -> Unit) -> Unit

/** `pushwooshInlineHeight`: the document height posted by [InlineScripts.HEIGHT]. */
internal class InlineHeightJs(private val onMain: InlineMainRunner, private val onHeight: (Double) -> Unit) {
    @JavascriptInterface
    fun postMessage(height: Double) = onMain { onHeight(height) }
}

/**
 * Why a banner's page may not post an event or run an action now; `null` = it may.
 * [isLive] `false` = the slot no longer holds the banner (expired, replaced), and checking it collapses an
 * expired one; [canPostEvent] `false` = a user change is not confirmed yet, so the event would go to the new user.
 */
internal class InlineBridgeGate(private val isLive: () -> Boolean, private val canPostEvent: () -> Boolean) {
    fun actionRefusal(): String? = if (isLive()) null else "the banner expired or was replaced"

    fun postEventRefusal(): String? = actionRefusal() ?: if (canPostEvent()) null else "the user change is not confirmed yet"
}

/**
 * `pushwooshImpl` of an inline banner: the Rich Media bridge without the in-app close report;
 * closing goes to the banner's [coordinator]; `postEvent` and the closing actions are refused as [gate] says.
 */
internal class InlinePushwooshJs(
    webView: WebView,
    private val onMain: InlineMainRunner,
    private val coordinator: InlineHtmlTapCoordinator,
    private val gate: InlineBridgeGate,
) : PushwooshJSInterface(JsCallback { onMain(coordinator::onCloseInApp) }, webView, null, null, null) {

    private val webView = WeakReference(webView)

    @JavascriptInterface
    override fun closeInApp() = onMain(coordinator::onCloseInApp)

    @JavascriptInterface
    override fun postEvent(event: String?, attributesStr: String?, successCb: String?, errorCb: String?) = onMain {
        val refusal = gate.postEventRefusal()
        if (refusal == null) {
            super.postEvent(event, attributesStr, successCb, errorCb)
        } else {
            PWLog.warn(TAG, "Inline banner postEvent '$event' refused: $refusal")
            webView.get()?.evaluateJavascript(
                "_pwCallbackHelper.invokeCallback(${JSONObject.quote(errorCb.orEmpty())}, ${JSONObject.quote(NOT_ALLOWED)});",
                null,
            )
        }
    }

    @JavascriptInterface
    override fun registerForPushNotifications() = onMain { closing("registerForPushNotifications") { super.registerForPushNotifications() } }

    @JavascriptInterface
    override fun openAppSettings() = onMain { closing("openAppSettings") { super.openAppSettings() } }

    /** Runs [action], then closes the banner; a tap that triggered it is reported as a click first. Nothing runs when [gate] refuses. */
    private fun closing(name: String, action: () -> Unit) {
        gate.actionRefusal()?.let {
            PWLog.warn(TAG, "Inline banner $name refused: $it")
            return
        }
        coordinator.onClosingAction()
        try {
            action()
        } catch (e: RuntimeException) {
            PWLog.warn(TAG, "Inline banner action failed: ${e.message}")
        }
        coordinator.onCloseInApp()
    }

    private companion object {
        const val TAG = "InlinePushwooshJs"
        const val NOT_ALLOWED = "postEvent is not allowed now"
    }
}

/** `pushManager` (legacy) of an inline banner: `closeInApp` closes through [coordinator], `postEvent` is a no-op while [gate] refuses it. */
internal class InlinePushManagerJs(
    webView: WebView,
    private val onMain: InlineMainRunner,
    coordinator: InlineHtmlTapCoordinator,
    private val gate: InlineBridgeGate,
) : PushManagerJSInterface(webView, JsCallback { onMain(coordinator::onCloseInApp) }) {

    @JavascriptInterface
    override fun postEvent(blob: String?) = onMain {
        val refusal = gate.postEventRefusal()
        if (refusal == null) {
            super.postEvent(blob)
        } else {
            PWLog.warn(TAG, "Inline banner postEvent refused: $refusal")
        }
    }

    private companion object {
        const val TAG = "InlinePushManagerJs"
    }
}
