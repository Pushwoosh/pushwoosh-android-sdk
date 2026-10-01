package com.pushwoosh.pushmart.sdk

import java.math.BigDecimal

sealed class TagOp {
    data class PutString(val key: String, val value: String) : TagOp()

    data class PutBoolean(val key: String, val value: Boolean) : TagOp()

    data class IncrementInt(val key: String, val by: Int) : TagOp()

    data class AppendList(val key: String, val values: List<String>) : TagOp()

    data class RemoveFromList(val key: String, val values: List<String>) : TagOp()
}

class GatewayResult<T> private constructor(val data: T?, val error: String?) {
    val isSuccess: Boolean
        get() = error == null

    companion object {
        fun <T> success(data: T?): GatewayResult<T> = GatewayResult(data, null)

        fun <T> failure(error: String): GatewayResult<T> = GatewayResult(null, error)
    }
}

interface PushwooshGateway {
    fun setAppId(code: String)

    fun getApplicationCode(): String

    fun registerForPushNotifications(callback: (GatewayResult<String?>) -> Unit)

    fun unregisterForPushNotifications()

    fun setEmail(email: String, callback: (GatewayResult<Unit>) -> Unit)

    fun setUserId(userId: String, callback: (GatewayResult<Unit>) -> Unit)

    fun getUserId(): String?

    fun getPushToken(): String?

    fun getHwid(): String

    fun setTags(ops: List<TagOp>)

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
}
