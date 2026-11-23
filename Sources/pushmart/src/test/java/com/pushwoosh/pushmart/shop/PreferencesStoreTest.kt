package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import com.pushwoosh.pushmart.sdk.GatewayResult
import com.pushwoosh.pushmart.sdk.TagOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesStoreTest {
    private val prefs = InMemoryKeyValueStore()
    private val gateway = FakePushwooshGateway()
    private val preferences = PreferencesStore(prefs, gateway)

    @Test
    fun categoryToggleSendsStringTagAndPersists() {
        assertTrue(preferences.toggle("Tops"))
        assertTrue(preferences.isSelected("Tops"))
        assertEquals(listOf("Tops"), prefs.getStringList(PreferencesStore.KEY))
        assertEquals(setOf("Tops"), PreferencesStore(prefs, FakePushwooshGateway()).selected)

        assertFalse(preferences.toggle("Tops"))
        assertFalse(preferences.isSelected("Tops"))
        assertTrue(prefs.getStringList(PreferencesStore.KEY).isEmpty())
        assertEquals(
            listOf(TagOp.PutString("fav_tops", "true"), TagOp.PutString("fav_tops", "false")),
            gateway.tags)
    }

    @Test
    fun sizeTopicsAndEmailNewsFollowTheSdkResult() {
        var failures = 0
        preferences.selectSize("M") { failures++ }
        gateway.setTagsResult = GatewayResult.failure("offline")
        preferences.selectSize("L") { failures++ }
        assertEquals("M", preferences.size)
        assertEquals("M", PreferencesStore(prefs, FakePushwooshGateway()).size)
        assertEquals(1, failures)
        assertEquals(
            listOf(TagOp.PutString("size", "M"), TagOp.PutString("size", "L")), gateway.tags)

        gateway.setTagsResult = GatewayResult.success(Unit)
        gateway.tags.clear()
        gateway.calls.clear()
        val saved = mutableListOf<Boolean>()
        preferences.saveNewsTopics(listOf("new_in", "drops")) { saved.add(it) }
        assertEquals(setOf("new_in", "drops"), preferences.newsTopics)
        assertEquals(listOf(TagOp.PutList("news_topics", listOf("new_in", "drops"))), gateway.tags)
        assertEquals(listOf("setTags", "postEvent:Subscription Segments"), gateway.calls)
        assertEquals(listOf("Subscription Segments"), gateway.events.map { it.first })

        gateway.setTagsResult = GatewayResult.failure("offline")
        preferences.saveNewsTopics(listOf("sales")) { saved.add(it) }
        assertEquals(listOf(true, false), saved)
        assertEquals(setOf("new_in", "drops"), preferences.newsTopics)

        preferences.setEmailNews(true, "shopper@example.com") { failures++ }
        assertFalse(preferences.emailNews)
        assertEquals(2, failures)
        assertEquals(TagOp.PutBoolean("email_news", true), gateway.tags.last())

        gateway.setTagsResult = GatewayResult.success(Unit)
        gateway.calls.clear()
        preferences.setEmailNews(true, "") { failures++ }
        assertTrue(gateway.calls.isEmpty())
        preferences.setEmailNews(true, "shopper@example.com") { failures++ }
        assertEquals(listOf("setEmailTags:shopper@example.com"), gateway.calls)
        assertTrue(preferences.emailNews)
        assertEquals(2, failures)
    }

    @Test
    fun restoreMergesSdkTagsIntoLocalState() {
        preferences.toggle("Jeans")
        preferences.toggle("Dresses")
        preferences.selectSize("S") {}
        val sent = gateway.tags.size
        gateway.tagsResult =
            GatewayResult.success(
                mapOf(
                    "fav_outerwear" to true,
                    "fav_tops" to "1",
                    "fav_jeans" to "false",
                    "size" to "XXL"))
        var restored = 0
        preferences.restore { restored++ }
        assertEquals(1, restored)
        assertEquals(setOf("Outerwear", "Tops", "Dresses"), preferences.selected)
        assertEquals("S", preferences.size)

        gateway.tagsResult = GatewayResult.success(mapOf("size" to "M"))
        preferences.restore { restored++ }
        assertEquals("M", preferences.size)
        assertEquals(setOf("Outerwear", "Tops", "Dresses"), preferences.selected)

        gateway.tagsResult = GatewayResult.failure("offline")
        preferences.restore { restored++ }
        assertEquals(2, restored)
        assertEquals(sent, gateway.tags.size)

        val reloaded = PreferencesStore(prefs, FakePushwooshGateway())
        assertEquals(setOf("Outerwear", "Tops", "Dresses"), reloaded.selected)
        assertEquals("M", reloaded.size)
    }
}
