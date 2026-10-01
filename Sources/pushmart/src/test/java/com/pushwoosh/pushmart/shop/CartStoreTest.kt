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
    fun addIncrementsQuantityAndSyncsBadge() {
        cart.add("aeroknit")
        cart.add("aeroknit")
        cart.add("orbit")
        assertEquals(mapOf("aeroknit" to 2, "orbit" to 1), cart.items)
        assertEquals(3, cart.count)
        assertEquals(listOf(1, 2, 3), gateway.badges)
    }

    @Test
    fun setQuantityZeroOrNegativeRemovesLine() {
        cart.add("aeroknit")
        cart.add("orbit")
        cart.setQuantity("aeroknit", 0)
        assertEquals(mapOf("orbit" to 1), cart.items)
        cart.setQuantity("orbit", -3)
        assertTrue(cart.items.isEmpty())
        assertEquals(0, gateway.badges.last())
    }

    @Test
    fun linesSortedByNameWithTotals() {
        cart.add("orbit")
        cart.add("aeroknit")
        cart.setQuantity("aeroknit", 2)
        assertEquals(listOf("aeroknit", "orbit"), cart.lines.map { it.product.id })
        assertEquals(listOf(2, 1), cart.lines.map { it.quantity })
        assertEquals(30700, cart.totalCents)
        assertEquals("$307.00", cart.totalText)
    }

    @Test
    fun clearEmptiesCartAndResetsBadge() {
        cart.add("aeroknit")
        cart.clear()
        assertTrue(cart.items.isEmpty())
        assertEquals(0, cart.count)
        assertEquals(0, gateway.badges.last())
        assertEquals("$0.00", cart.totalText)
    }

    @Test
    fun persistsAcrossInstances() {
        cart.add("aeroknit")
        cart.add("aeroknit")
        cart.add("solo")
        val reloaded = CartStore(prefs, FakePushwooshGateway())
        assertEquals(mapOf("aeroknit" to 2, "solo" to 1), reloaded.items)
    }

    @Test
    fun ignoresCorruptPersistedEntries() {
        prefs.putStringList("pushmart.cart", listOf("abc", "orbit:x", "nope:2", "chrono:3", ":1"))
        val loaded = CartStore(prefs, gateway)
        assertEquals(mapOf("chrono" to 3), loaded.items)
    }

    @Test
    fun ignoresUnknownProductIds() {
        cart.add("nope")
        assertTrue(cart.items.isEmpty())
        assertTrue(gateway.badges.isEmpty())
    }

    @Test
    fun notifiesListenersOnChange() {
        var notified = 0
        val listener: () -> Unit = { notified++ }
        cart.addListener(listener)
        cart.add("aeroknit")
        cart.removeListener(listener)
        cart.add("aeroknit")
        assertEquals(1, notified)
    }
}
