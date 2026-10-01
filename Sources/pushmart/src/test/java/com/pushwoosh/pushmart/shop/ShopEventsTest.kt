package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopEventsTest {
    private val gateway = FakePushwooshGateway()
    private val events = ShopEvents(gateway)
    private val aeroknit = Catalog.byId("aeroknit")!!

    @Test
    fun productOpenedCarriesProductId() {
        events.productOpened(aeroknit)
        assertEquals(
            listOf("product-opened" to mapOf<String, Any>("productId" to "aeroknit")),
            gateway.events)
    }

    @Test
    fun addToCartSendsPriceAsNumberAndOptionAsString() {
        events.addedToCart(aeroknit, "US 9")
        val (name, attributes) = gateway.events.single()
        assertEquals("add-to-cart", name)
        assertEquals("aeroknit", attributes["productId"])
        assertEquals(129, attributes["price"])
        assertTrue(attributes["price"] is Int)
        assertEquals("US 9", attributes["option"])
        assertEquals(setOf("productId", "price", "option"), attributes.keys)
    }
}
