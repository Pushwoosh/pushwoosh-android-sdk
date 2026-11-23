package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.sdk.PushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp
import java.math.BigDecimal
import kotlin.math.roundToInt

class PurchaseReporter(
    private val prefs: KeyValueStore,
    private val gateway: PushwooshGateway,
    private val orders: OrderStore,
) {
    /** Reports the bag to Pushwoosh, saves it as an order and empties it. Null for an empty bag. */
    fun checkout(cart: CartStore): Order? {
        val lines = cart.lines
        if (lines.isEmpty()) return null
        val ids = lines.map { it.product.id }
        for (line in lines) {
            val unitPrice = BigDecimal.valueOf(line.product.priceCents.toLong(), 2)
            gateway.sendInappPurchase(line.product.id, unitPrice, "USD")
        }
        val lifetimeIds = (prefs.getStringList(KEY_PRODUCT_IDS) + ids).distinct().sorted()
        val lifetimeCents =
            (prefs.getString(KEY_TOTAL_CENTS).toLongOrNull() ?: 0L) + cart.totalCents
        prefs.putStringList(KEY_PRODUCT_IDS, lifetimeIds)
        prefs.putString(KEY_TOTAL_CENTS, lifetimeCents.toString())
        gateway.setTags(
            listOf(
                TagOp.PutList("In-app Product", lifetimeIds),
                TagOp.PutInt("In-app Purchase", (lifetimeCents / 100.0).roundToInt()),
                TagOp.IncrementInt("orders_placed", 1)))
        gateway.postEvent("CheckoutSuccess", mapOf("products" to ids))
        val order = orders.place(ids, cart.totalText)
        cart.clear()
        return order
    }

    companion object {
        const val KEY_PRODUCT_IDS = "nuvo.purchasedProductIds"
        const val KEY_TOTAL_CENTS = "nuvo.purchaseTotalCents"
    }
}
