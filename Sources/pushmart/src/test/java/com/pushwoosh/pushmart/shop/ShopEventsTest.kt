package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import org.junit.Assert.assertEquals
import org.junit.Test

class ShopEventsTest {
    private val gateway = FakePushwooshGateway()
    private val events = ShopEvents(gateway)
    private val cart = CartStore(InMemoryKeyValueStore(), FakePushwooshGateway())

    @Test
    fun basketOpenListsBagIdsInBagOrder() {
        cart.add("tops_02")
        cart.add("outwear_01")
        cart.add("outwear_01")
        events.basketOpened(cart)
        assertEquals(
            listOf(
                "BasketOpen" to mapOf<String, Any>("products" to listOf("outwear_01", "tops_02"))),
            gateway.events)
    }

    @Test
    fun clearBagPostsCartClearedAndLeavesBasketOpenEmpty() {
        val bag = CartStore(InMemoryKeyValueStore(), gateway)
        bag.add("tops_02")
        gateway.calls.clear()
        gateway.events.clear()
        events.clearBag(bag)
        events.basketOpened(bag)
        assertEquals(
            listOf("badge:0", "postEvent:CartCleared", "postEvent:BasketOpen"), gateway.calls)
        assertEquals(
            listOf(
                "CartCleared" to emptyMap<String, Any>(),
                "BasketOpen" to mapOf<String, Any>("products" to emptyList<String>())),
            gateway.events)
    }
}
