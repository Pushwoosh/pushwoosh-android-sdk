package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.sdk.PushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp
import java.math.BigDecimal

class PurchaseReporter(private val gateway: PushwooshGateway) {
    fun report(cart: CartStore) {
        val lines = cart.lines
        if (lines.isEmpty()) return
        // The SDK hardcodes quantity=1 per call, so each unit is its own purchase to keep revenue
        // right.
        for (line in lines) {
            val unitPrice = BigDecimal.valueOf(line.product.priceCents.toLong(), 2)
            repeat(line.quantity) { gateway.sendInappPurchase(line.product.id, unitPrice, "USD") }
        }
        gateway.setTags(
            listOf(
                TagOp.IncrementInt("orders_placed", 1),
                TagOp.AppendList("purchased_products", lines.map { it.product.id })))
        gateway.postEvent("checkout", mapOf("items" to cart.count, "total" to cart.totalText))
    }
}
