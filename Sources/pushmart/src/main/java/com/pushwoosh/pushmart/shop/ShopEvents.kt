package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.sdk.PushwooshGateway

class ShopEvents(private val gateway: PushwooshGateway) {
    fun productOpened(product: Product) {
        gateway.postEvent("product-opened", mapOf("productId" to product.id))
    }

    fun addedToCart(product: Product, option: String) {
        gateway.postEvent(
            "add-to-cart",
            mapOf("productId" to product.id, "price" to product.priceDollars, "option" to option))
    }
}
