package com.pushwoosh.pushmart.account

import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountStoreTest {
    private val prefs = InMemoryKeyValueStore()
    private val store = AccountStore(prefs)

    @Test
    fun defaultsAreGuestNotOnboarded() {
        assertFalse(store.onboarded)
        assertNull(store.personalCode)
        assertTrue(store.isGuest)
        assertFalse(store.pushNotificationEnabled)
    }

    @Test
    fun persistsUnderIosKeys() {
        store.onboarded = true
        store.personalCode = "ABCDE-12345"
        store.smsNumber = "+15550100"
        store.whatsappNumber = "+15550101"
        store.userEmail = "ada@example.com"
        store.pushNotificationEnabled = true
        assertTrue(prefs.getBoolean("nuvo.onboarded"))
        assertEquals("ABCDE-12345", prefs.getString("nuvo.appCode"))
        assertEquals("+15550100", prefs.getString("pushmart.smsNumber"))
        assertEquals("+15550101", prefs.getString("pushmart.whatsappNumber"))
        assertEquals("ada@example.com", prefs.getString("nuvo.userEmail"))
        assertTrue(prefs.getBoolean("pushNotificationEnabled"))
        store.personalCode = null
        assertEquals("absent", prefs.getString("nuvo.appCode", "absent"))
        assertTrue(store.isGuest)
    }

    @Test
    fun storedCodeReadsAsGuestUnlessPersonal() {
        prefs.putString("nuvo.appCode", "5DF3B-09018")
        assertNull(store.personalCode)
        assertTrue(store.isGuest)
        prefs.putString("nuvo.appCode", "nope")
        assertNull(store.personalCode)
        assertTrue(store.isGuest)
        prefs.putString("nuvo.appCode", "ABCDE-12345")
        assertEquals("ABCDE-12345", store.personalCode)
        assertFalse(store.isGuest)
    }
}
