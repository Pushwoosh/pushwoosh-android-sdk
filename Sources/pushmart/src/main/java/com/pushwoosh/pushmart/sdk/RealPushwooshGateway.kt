package com.pushwoosh.pushmart.sdk

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.pushwoosh.Pushwoosh
import com.pushwoosh.badge.PushwooshBadge
import com.pushwoosh.inapp.InAppManager
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.notification.LocalNotification
import com.pushwoosh.tags.TagsBundle
import java.math.BigDecimal

class RealPushwooshGateway : PushwooshGateway {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val pushwoosh: Pushwoosh
        get() = Pushwoosh.getInstance()

    override fun setAppId(code: String) {
        pushwoosh.setAppId(code)
    }

    override fun getApplicationCode(): String = pushwoosh.applicationCode

    override fun resetUserId() {
        val hwid = pushwoosh.hwid
        if (pushwoosh.userId != hwid) pushwoosh.setUserId(hwid)
    }

    override fun registerForPushNotifications(callback: (GatewayResult<String?>) -> Unit) {
        pushwoosh.registerForPushNotifications { result ->
            if (result.isSuccess) {
                callback(GatewayResult.success(result.data?.token))
            } else {
                callback(GatewayResult.failure(result.exception?.message ?: "Registration failed"))
            }
        }
    }

    override fun unregisterForPushNotifications(callback: ((GatewayResult<Unit>) -> Unit)?) {
        // The SDK keeps one pending unregister callback: a no-op one would swallow a code switch's.
        if (callback == null) {
            pushwoosh.unregisterForPushNotifications()
            return
        }
        pushwoosh.unregisterForPushNotifications { r ->
            callback(unitResult(r.isSuccess, r.exception?.message, "Unregister failed"))
        }
    }

    override fun setEmail(email: String, callback: (GatewayResult<Unit>) -> Unit) {
        pushwoosh.setEmail(email) { result ->
            callback(unitResult(result.isSuccess, result.exception?.message, "setEmail failed"))
        }
    }

    override fun registerSmsNumber(number: String) {
        pushwoosh.registerSMSNumber(number)
    }

    override fun registerWhatsappNumber(number: String) {
        pushwoosh.registerWhatsappNumber(number)
    }

    override fun setTags(ops: List<TagOp>) {
        setTags(ops) { result ->
            if (!result.isSuccess) PWLog.warn(TAG, "setTags failed: " + result.error)
        }
    }

    override fun setTags(ops: List<TagOp>, callback: (GatewayResult<Unit>) -> Unit) {
        pushwoosh.setTags(bundle(ops)) { result ->
            onMain {
                callback(unitResult(result.isSuccess, result.exception?.message, "setTags failed"))
            }
        }
    }

    override fun setEmailTags(
        email: String,
        ops: List<TagOp>,
        callback: (GatewayResult<Unit>) -> Unit
    ) {
        pushwoosh.setEmailTags(bundle(ops), email) { result ->
            onMain {
                callback(
                    unitResult(result.isSuccess, result.exception?.message, "setEmailTags failed"))
            }
        }
    }

    override fun getTags(callback: (GatewayResult<Map<String, Any?>>) -> Unit) {
        pushwoosh.getTags { result ->
            val tags = result.data
            onMain {
                if (result.isSuccess && tags != null) {
                    callback(GatewayResult.success(tags.map.toMap()))
                } else {
                    callback(GatewayResult.failure(result.exception?.message ?: "getTags failed"))
                }
            }
        }
    }

    private fun bundle(ops: List<TagOp>): TagsBundle {
        val builder = TagsBundle.Builder()
        for (op in ops) {
            when (op) {
                is TagOp.PutString -> builder.putString(op.key, op.value)
                is TagOp.PutBoolean -> builder.putBoolean(op.key, op.value)
                is TagOp.PutInt -> builder.putInt(op.key, op.value)
                is TagOp.PutList -> builder.putList(op.key, op.values)
                is TagOp.IncrementInt -> builder.incrementInt(op.key, op.by)
                is TagOp.AppendList -> builder.appendList(op.key, op.values)
                is TagOp.RemoveFromList -> builder.removeFromList(op.key, op.values)
            }
        }
        return builder.build()
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    override fun postEvent(name: String, attributes: Map<String, Any>) {
        postEvent(name, attributes) { result ->
            if (!result.isSuccess) PWLog.warn(TAG, "postEvent $name failed: " + result.error)
        }
    }

    override fun postEvent(
        name: String,
        attributes: Map<String, Any>,
        callback: (GatewayResult<Unit>) -> Unit
    ) {
        val builder = TagsBundle.Builder()
        for ((key, value) in attributes) {
            when (value) {
                is Int -> builder.putInt(key, value)
                is Boolean -> builder.putBoolean(key, value)
                is List<*> -> builder.putList(key, value.map { it.toString() })
                else -> builder.putString(key, value.toString())
            }
        }
        InAppManager.getInstance().postEvent(name, builder.build()) { result ->
            callback(unitResult(result.isSuccess, result.exception?.message, "postEvent failed"))
        }
    }

    override fun sendInappPurchase(sku: String, price: BigDecimal, currency: String) {
        pushwoosh.sendInappPurchase(sku, price, currency)
    }

    override fun setBadgeNumber(count: Int) {
        PushwooshBadge.setBadgeNumber(count)
    }

    override fun scheduleLocalNotification(title: String, body: String, delaySeconds: Int) {
        // The builder has no title setter; PushBundleDataProvider reads the title from "header".
        val extras = Bundle().apply { putString("header", title) }
        pushwoosh.scheduleLocalNotification(
            LocalNotification.Builder()
                .setMessage(body)
                .setDelay(delaySeconds.coerceAtLeast(1))
                .setExtras(extras)
                .build())
    }

    override fun setShowForegroundAlerts(show: Boolean) {
        // Inverted on Android: true suppresses foreground notifications (Pushwoosh.java).
        pushwoosh.setShowPushnotificationAlert(!show)
    }

    private fun unitResult(
        success: Boolean,
        error: String?,
        fallback: String
    ): GatewayResult<Unit> =
        if (success) GatewayResult.success(Unit) else GatewayResult.failure(error ?: fallback)

    companion object {
        private const val TAG = "RealPushwooshGateway"
    }
}
