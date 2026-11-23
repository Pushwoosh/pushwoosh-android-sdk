package com.pushwoosh.pushmart.sdk

import java.math.BigDecimal

sealed class TagOp {
    data class PutString(val key: String, val value: String) : TagOp()

    data class PutBoolean(val key: String, val value: Boolean) : TagOp()

    data class PutInt(val key: String, val value: Int) : TagOp()

    data class PutList(val key: String, val values: List<String>) : TagOp()

    data class IncrementInt(val key: String, val by: Int) : TagOp()

    data class AppendList(val key: String, val values: List<String>) : TagOp()

    data class RemoveFromList(val key: String, val values: List<String>) : TagOp()
}

class GatewayResult<T> private constructor(val data: T?, val error: String?) {
    val isSuccess: Boolean
        get() = error == null

    companion object {
        fun <T> success(data: T?): GatewayResult<T> = GatewayResult(data, null)

        /**
         * [data] is whatever the call still produced, such as the cached inbox after a failed sync.
         */
        fun <T> failure(error: String, data: T? = null): GatewayResult<T> =
            GatewayResult(data, error)
    }
}

interface PushwooshGateway {
    fun setAppId(code: String)

    fun getApplicationCode(): String

    fun resetUserId()

    fun registerForPushNotifications(callback: (GatewayResult<String?>) -> Unit)

    fun unregisterForPushNotifications(callback: ((GatewayResult<Unit>) -> Unit)? = null)

    fun setEmail(email: String, callback: (GatewayResult<Unit>) -> Unit)

    /** Fire and forget: the SDK has no callback for it. */
    fun registerSmsNumber(number: String)

    /** Fire and forget: the SDK has no callback for it. */
    fun registerWhatsappNumber(number: String)

    fun setTags(ops: List<TagOp>)

    /** Same as [setTags], surfacing the SDK result. */
    fun setTags(ops: List<TagOp>, callback: (GatewayResult<Unit>) -> Unit)

    fun setEmailTags(email: String, ops: List<TagOp>, callback: (GatewayResult<Unit>) -> Unit)

    /**
     * The device's tags as the SDK reads them. On a network error the SDK answers with its cached
     * tags as a success.
     */
    fun getTags(callback: (GatewayResult<Map<String, Any?>>) -> Unit)

    fun postEvent(name: String, attributes: Map<String, Any>)

    /**
     * Same as [postEvent], surfacing the SDK callback. Success means the request went through, not
     * that any content came back.
     */
    fun postEvent(
        name: String,
        attributes: Map<String, Any>,
        callback: (GatewayResult<Unit>) -> Unit
    )

    fun sendInappPurchase(sku: String, price: BigDecimal, currency: String)

    fun setBadgeNumber(count: Int)

    fun scheduleLocalNotification(title: String, body: String, delaySeconds: Int)

    /** Whether a push arriving while PushMart is in the foreground shows as a notification. */
    fun setShowForegroundAlerts(show: Boolean)
}
