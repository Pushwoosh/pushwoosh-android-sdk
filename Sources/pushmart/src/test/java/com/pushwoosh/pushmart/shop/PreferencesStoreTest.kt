package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
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
    fun toggleOnSendsTrueTagAndPersists() {
        val on = preferences.toggle("Shoes")
        assertTrue(on)
        assertTrue(preferences.isSelected("Shoes"))
        assertEquals(setOf("Shoes"), preferences.selected)
        assertEquals(listOf(TagOp.PutBoolean("fav_shoes", true)), gateway.tags)
        assertEquals(listOf("Shoes"), prefs.getStringList(PreferencesStore.KEY))
    }

    @Test
    fun toggleOffSendsFalseTagAndRemoves() {
        preferences.toggle("Shoes")
        val on = preferences.toggle("Shoes")
        assertFalse(on)
        assertFalse(preferences.isSelected("Shoes"))
        assertEquals(TagOp.PutBoolean("fav_shoes", false), gateway.tags.last())
        assertTrue(prefs.getStringList(PreferencesStore.KEY).isEmpty())
    }

    @Test
    fun restoredFromPrefsOnANewInstance() {
        preferences.toggle("Shoes")
        val reloaded = PreferencesStore(prefs, FakePushwooshGateway())
        assertTrue(reloaded.isSelected("Shoes"))
        assertEquals(setOf("Shoes"), reloaded.selected)
    }
}
