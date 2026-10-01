package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.sdk.PushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp

class PriceAlertStore(private val prefs: KeyValueStore, private val gateway: PushwooshGateway) {
    fun isOn(productId: String): Boolean = prefs.getStringList(KEY).contains(productId)

    fun set(on: Boolean, product: Product) {
        if (isOn(product.id) == on) return
        val current = prefs.getStringList(KEY)
        val updated = if (on) current + product.id else current.filter { it != product.id }
        prefs.putStringList(KEY, updated)
        val op =
            if (on) TagOp.AppendList(TAG, listOf(product.id))
            else TagOp.RemoveFromList(TAG, listOf(product.id))
        gateway.setTags(listOf(op))
        if (on) {
            gateway.postEvent(
                "price_alert_on", mapOf("productId" to product.id, "price" to product.priceDollars))
        }
    }

    companion object {
        const val KEY = "pushmart.priceAlerts"
        const val TAG = "price_alerts"
    }
}
