package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseReporterTest {
    private val gateway = FakePushwooshGateway()
    private val cart = CartStore(InMemoryKeyValueStore(), FakePushwooshGateway())
    private val reporter = PurchaseReporter(gateway)

    @Test
    fun reportsOnePurchasePerUnitAtUnitPrice() {
        cart.add("orbit")
        cart.add("aeroknit")
        cart.add("aeroknit")
        reporter.report(cart)
        assertEquals(listOf("aeroknit", "aeroknit", "orbit"), gateway.purchases.map { it.first })
        assertEquals(0, BigDecimal("129").compareTo(gateway.purchases[0].second))
        assertEquals(0, BigDecimal("129").compareTo(gateway.purchases[1].second))
        assertEquals(0, BigDecimal("49").compareTo(gateway.purchases[2].second))
        assertTrue(gateway.purchases.all { it.third == "USD" })
    }

    @Test
    fun incrementsOrdersAppendsProductsAndPostsCheckout() {
        cart.add("orbit")
        cart.add("aeroknit")
        cart.add("aeroknit")
        reporter.report(cart)
        assertEquals(
            listOf(
                TagOp.IncrementInt("orders_placed", 1),
                TagOp.AppendList("purchased_products", listOf("aeroknit", "orbit"))),
            gateway.tags)
        assertEquals(
            listOf("checkout" to mapOf<String, Any>("items" to 3, "total" to "$307.00")),
            gateway.events)
    }

    @Test
    fun callsGoPurchasesThenTagsThenEvent() {
        cart.add("solo")
        reporter.report(cart)
        assertEquals(listOf("purchase:solo", "setTags", "postEvent:checkout"), gateway.calls)
    }

    @Test
    fun emptyCartReportsNothing() {
        reporter.report(cart)
        assertTrue(gateway.calls.isEmpty())
    }
}
