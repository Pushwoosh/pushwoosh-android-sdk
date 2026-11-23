package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.sdk.PushwooshGateway

class ShopEvents(private val gateway: PushwooshGateway) {
    fun clearBag(cart: CartStore) {
        cart.clear()
        gateway.postEvent("CartCleared", emptyMap())
    }

    fun basketOpened(cart: CartStore) {
        gateway.postEvent("BasketOpen", mapOf("products" to cart.lines.map { it.product.id }))
    }
}
