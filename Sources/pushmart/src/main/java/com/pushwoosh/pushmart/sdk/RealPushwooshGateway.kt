package com.pushwoosh.pushmart.sdk

import com.pushwoosh.Pushwoosh
import com.pushwoosh.badge.PushwooshBadge
import com.pushwoosh.inapp.InAppManager
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.tags.TagsBundle
import java.math.BigDecimal

class RealPushwooshGateway : PushwooshGateway {
    private val pushwoosh: Pushwoosh
        get() = Pushwoosh.getInstance()

    override fun setAppId(code: String) {
        pushwoosh.setAppId(code)
    }

    override fun getApplicationCode(): String = pushwoosh.applicationCode

    override fun registerForPushNotifications(callback: (GatewayResult<String?>) -> Unit) {
        pushwoosh.registerForPushNotifications { result ->
            if (result.isSuccess) {
                callback(GatewayResult.success(result.data?.token))
            } else {
                callback(GatewayResult.failure(result.exception?.message ?: "Registration failed"))
            }
        }
    }

    override fun unregisterForPushNotifications() {
        pushwoosh.unregisterForPushNotifications()
    }

    override fun setEmail(email: String, callback: (GatewayResult<Unit>) -> Unit) {
        pushwoosh.setEmail(email) { result ->
            callback(unitResult(result.isSuccess, result.exception?.message, "setEmail failed"))
        }
    }

    override fun setUserId(userId: String, callback: (GatewayResult<Unit>) -> Unit) {
        pushwoosh.setUserId(userId) { result ->
            callback(unitResult(result.isSuccess, result.exception?.message, "setUserId failed"))
        }
    }

    override fun getUserId(): String? = pushwoosh.userId

    override fun getPushToken(): String? = pushwoosh.pushToken

    override fun getHwid(): String = pushwoosh.hwid

    override fun setTags(ops: List<TagOp>) {
        val builder = TagsBundle.Builder()
        for (op in ops) {
            when (op) {
                is TagOp.PutString -> builder.putString(op.key, op.value)
                is TagOp.PutBoolean -> builder.putBoolean(op.key, op.value)
                is TagOp.IncrementInt -> builder.incrementInt(op.key, op.by)
                is TagOp.AppendList -> builder.appendList(op.key, op.values)
                is TagOp.RemoveFromList -> builder.removeFromList(op.key, op.values)
            }
        }
        pushwoosh.setTags(builder.build()) { result ->
            if (!result.isSuccess) PWLog.warn(TAG, "setTags failed: " + result.exception?.message)
        }
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
