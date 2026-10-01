package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceAlertStoreTest {
    private val prefs = InMemoryKeyValueStore()
    private val gateway = FakePushwooshGateway()
    private val alerts = PriceAlertStore(prefs, gateway)
    private val chrono = Catalog.byId("chrono")!!

    @Test
    fun turningOnAppendsTagAndPostsEvent() {
        alerts.set(true, chrono)
        assertTrue(alerts.isOn("chrono"))
        assertEquals(listOf("chrono"), prefs.getStringList("pushmart.priceAlerts"))
        assertEquals(listOf(TagOp.AppendList("price_alerts", listOf("chrono"))), gateway.tags)
        assertEquals(
            listOf("price_alert_on" to mapOf<String, Any>("productId" to "chrono", "price" to 249)),
            gateway.events)
    }

    @Test
    fun turningOffRemovesTagWithoutEvent() {
        alerts.set(true, chrono)
        alerts.set(false, chrono)
        assertFalse(alerts.isOn("chrono"))
        assertEquals(TagOp.RemoveFromList("price_alerts", listOf("chrono")), gateway.tags.last())
        assertEquals(1, gateway.events.size)
    }

    @Test
    fun settingSameStateTwiceIsIdempotent() {
        alerts.set(true, chrono)
        alerts.set(true, chrono)
        assertEquals(listOf("chrono"), prefs.getStringList("pushmart.priceAlerts"))
        assertEquals(1, gateway.tags.size)
        assertEquals(1, gateway.events.size)
    }
}
