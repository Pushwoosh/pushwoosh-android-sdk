package com.pushwoosh.pushmart.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryKeyValueStoreTest {
    private val store = InMemoryKeyValueStore()

    @Test
    fun returnsDefaultsForMissingKeys() {
        assertEquals("", store.getString("missing"))
        assertEquals("x", store.getString("missing", "x"))
        assertFalse(store.getBoolean("missing"))
        assertTrue(store.getBoolean("missing", true))
        assertEquals(emptyList<String>(), store.getStringList("missing"))
    }

    @Test
    fun roundTripsValuesAndRemove() {
        store.putString("s", "v")
        store.putBoolean("b", true)
        store.putStringList("l", listOf("a", "b"))
        assertEquals("v", store.getString("s"))
        assertTrue(store.getBoolean("b"))
        assertEquals(listOf("a", "b"), store.getStringList("l"))
        store.remove("s")
        assertEquals("", store.getString("s"))
    }
}
