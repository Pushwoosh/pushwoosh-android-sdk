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
    private val sundress = Catalog.byId("dress_04")!!

    @Test
    fun turningOnAppendsPriceAlertsTag() {
        alerts.set(true, sundress)
        assertTrue(alerts.isOn("dress_04"))
        assertEquals(listOf("dress_04"), prefs.getStringList("pushmart.priceAlerts"))
        assertEquals(listOf(TagOp.AppendList("price_alerts", listOf("dress_04"))), gateway.tags)
        assertTrue(gateway.events.isEmpty())
    }

    @Test
    fun turningOffRemovesPriceAlertsTag() {
        alerts.set(true, sundress)
        alerts.set(false, sundress)
        assertFalse(alerts.isOn("dress_04"))
        assertEquals(TagOp.RemoveFromList("price_alerts", listOf("dress_04")), gateway.tags.last())
        assertTrue(gateway.events.isEmpty())
    }

    @Test
    fun settingSameStateTwiceIsIdempotent() {
        alerts.set(true, sundress)
        alerts.set(true, sundress)
        assertEquals(listOf("dress_04"), prefs.getStringList("pushmart.priceAlerts"))
        assertEquals(1, gateway.tags.size)
    }
}
