package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartStoreTest {
    private val prefs = InMemoryKeyValueStore()
    private val gateway = FakePushwooshGateway()
    private val cart = CartStore(prefs, gateway)

    @Test
    fun addPostsProductAddOnlyForNewProductAndSyncsBadge() {
        cart.add("outwear_01")
        cart.add("outwear_01")
        cart.add("tops_02")
        assertEquals(mapOf("outwear_01" to 2, "tops_02" to 1), cart.items)
        assertEquals(3, cart.count)
        assertEquals(listOf(1, 2, 3), gateway.badges)
        assertEquals(
            listOf(
                "ProductAdd" to mapOf<String, Any>("id" to "outwear_01"),
                "ProductAdd" to mapOf<String, Any>("id" to "tops_02")),
            gateway.events)
        assertEquals(
            listOf("postEvent:ProductAdd", "badge:1", "badge:2", "postEvent:ProductAdd", "badge:3"),
            gateway.calls)
    }

    @Test
    fun setQuantityZeroOrNegativeRemovesLine() {
        cart.add("outwear_01")
        cart.add("tops_02")
        cart.setQuantity("outwear_01", 0)
        assertEquals(mapOf("tops_02" to 1), cart.items)
        cart.setQuantity("tops_02", -3)
        assertTrue(cart.items.isEmpty())
        assertEquals(0, gateway.badges.last())
        cart.add("outwear_01")
        assertEquals(
            "ProductAdd" to mapOf<String, Any>("id" to "outwear_01"), gateway.events.last())
        assertEquals(3, gateway.events.size)
    }

    @Test
    fun linesSortedByNameWithTotals() {
        cart.add("tops_02")
        cart.add("outwear_01")
        cart.setQuantity("outwear_01", 2)
        assertEquals(listOf("outwear_01", "tops_02"), cart.lines.map { it.product.id })
        assertEquals(listOf(2, 1), cart.lines.map { it.quantity })
        assertEquals(28797, cart.totalCents)
        assertEquals("$287.97", cart.totalText)
    }

    @Test
    fun clearEmptiesCartAndResetsBadge() {
        cart.add("outwear_01")
        cart.clear()
        assertTrue(cart.items.isEmpty())
        assertEquals(0, cart.count)
        assertEquals(0, gateway.badges.last())
        assertEquals("$0.00", cart.totalText)
    }

    @Test
    fun persistsAcrossInstances() {
        cart.add("outwear_01")
        cart.add("outwear_01")
        cart.add("skirt_01")
        val reloadedGateway = FakePushwooshGateway()
        val reloaded = CartStore(prefs, reloadedGateway)
        assertEquals(mapOf("outwear_01" to 2, "skirt_01" to 1), reloaded.items)
        reloaded.add("outwear_01")
        assertTrue(reloadedGateway.events.isEmpty())
    }

    @Test
    fun ignoresCorruptPersistedEntries() {
        prefs.putStringList(
            "pushmart.cart", listOf("abc", "tops_02:x", "nope:2", "aeroknit:2", "dress_04:3", ":1"))
        val loaded = CartStore(prefs, gateway)
        assertEquals(mapOf("dress_04" to 3), loaded.items)
    }

    @Test
    fun ignoresUnknownProductIds() {
        cart.add("nope")
        assertTrue(cart.items.isEmpty())
        assertTrue(gateway.badges.isEmpty())
        assertTrue(gateway.events.isEmpty())
    }

    @Test
    fun notifiesListenersOnChange() {
        var notified = 0
        val listener: () -> Unit = { notified++ }
        cart.addListener(listener)
        cart.add("outwear_01")
        cart.removeListener(listener)
        cart.add("outwear_01")
        assertEquals(1, notified)
    }
}
