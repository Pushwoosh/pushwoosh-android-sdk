package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseReporterTest {
    private val prefs = InMemoryKeyValueStore()
    private val gateway = FakePushwooshGateway()
    private val cart = CartStore(prefs, gateway)
    private val numbers = ArrayDeque(listOf(24474, 24474, 25000))
    private val orders =
        OrderStore(prefs, now = { PLACED_AT }, nextNumber = { numbers.removeFirst() })
    private val reporter = PurchaseReporter(prefs, gateway, orders)

    @Test
    fun checkoutSendsOnePurchasePerLineAtUnitPrice() {
        fill("tops_02", "outwear_01", "outwear_01")
        reporter.checkout(cart)
        assertEquals(listOf("outwear_01", "tops_02"), gateway.purchases.map { it.first })
        assertEquals(0, BigDecimal("129.99").compareTo(gateway.purchases[0].second))
        assertEquals(0, BigDecimal("27.99").compareTo(gateway.purchases[1].second))
        assertTrue(gateway.purchases.all { it.third == "USD" })
    }

    @Test
    fun checkoutSetsLifetimePurchaseTagsAndPostsCheckoutSuccess() {
        fill("tops_02", "outwear_01", "outwear_01")
        val first = reporter.checkout(cart)
        assertEquals(
            listOf(
                TagOp.PutList("In-app Product", listOf("outwear_01", "tops_02")),
                TagOp.PutInt("In-app Purchase", 288),
                TagOp.IncrementInt("orders_placed", 1)),
            gateway.tags)
        assertEquals(
            listOf(
                "CheckoutSuccess" to
                    mapOf<String, Any>("products" to listOf("outwear_01", "tops_02"))),
            gateway.events)
        fill("skirt_01")
        val second = PurchaseReporter(prefs, gateway, orders).checkout(cart)
        assertEquals(
            listOf(
                TagOp.PutList("In-app Product", listOf("outwear_01", "skirt_01", "tops_02")),
                TagOp.PutInt("In-app Purchase", 328),
                TagOp.IncrementInt("orders_placed", 1)),
            gateway.tags)
        assertEquals(
            listOf("CheckoutSuccess" to mapOf<String, Any>("products" to listOf("skirt_01"))),
            gateway.events)
        assertEquals("PM-24474", first?.id)
        assertEquals("PM-25000", second?.id)
        assertEquals(listOf("PM-25000", "PM-24474"), orders.orders.map { it.id })
        assertEquals(orders.orders, OrderStore(prefs).orders)
    }

    @Test
    fun checkoutCallsGoPurchasesTagsEventThenBadgeAndOrderPersists() {
        fill("skirt_01")
        val order = reporter.checkout(cart)
        assertEquals(
            listOf("purchase:skirt_01", "setTags", "postEvent:CheckoutSuccess", "badge:0"),
            gateway.calls)
        assertEquals(Order("PM-24474", listOf("skirt_01"), "$39.99", PLACED_AT), order)
        assertEquals(0, cart.count)
        assertEquals(listOf(order), OrderStore(prefs).orders)
        assertEquals(
            listOf("skirt_01"),
            orders.place(listOf("nope", "skirt_01"), "$39.99").products.map { it.id })
    }

    @Test
    fun emptyCartChecksOutNothing() {
        assertNull(reporter.checkout(cart))
        assertTrue(gateway.calls.isEmpty())
        assertTrue(orders.orders.isEmpty())
        assertEquals("", prefs.getString(PurchaseReporter.KEY_TOTAL_CENTS))
    }

    private fun fill(vararg ids: String) {
        ids.forEach(cart::add)
        gateway.calls.clear()
        gateway.events.clear()
        gateway.tags.clear()
        gateway.purchases.clear()
    }

    private companion object {
        const val PLACED_AT = 1_762_000_000_000L
    }
}
