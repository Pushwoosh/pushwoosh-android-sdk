package com.pushwoosh.pushmart.account

import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountStoreTest {
    private val prefs = InMemoryKeyValueStore()
    private val store = AccountStore(prefs)

    @Test
    fun defaultsAreGuestNotOnboarded() {
        assertFalse(store.onboarded)
        assertEquals("", store.appCode)
        assertTrue(store.isGuest)
        assertFalse(store.pushNotificationEnabled)
        assertEquals("G", store.initials)
        assertEquals("", store.firstName)
    }

    @Test
    fun persistsUnderIosKeys() {
        store.onboarded = true
        store.appCode = "5DF3B-09018"
        store.userName = "Ada Lovelace"
        store.userEmail = "ada@example.com"
        store.pushNotificationEnabled = true
        assertTrue(prefs.getBoolean("nuvo.onboarded"))
        assertEquals("5DF3B-09018", prefs.getString("nuvo.appCode"))
        assertEquals("Ada Lovelace", prefs.getString("nuvo.userName"))
        assertEquals("ada@example.com", prefs.getString("nuvo.userEmail"))
        assertTrue(prefs.getBoolean("pushNotificationEnabled"))
    }

    @Test
    fun derivesFirstNameAndInitials() {
        store.userName = "Ada Lovelace"
        assertEquals("Ada", store.firstName)
        assertEquals("AL", store.initials)
        store.userName = "  grace "
        assertEquals("grace", store.firstName)
        assertEquals("G", store.initials)
        assertFalse(store.isGuest)
    }

    @Test
    fun clearIdentityMakesGuest() {
        store.userName = "Ada"
        store.userEmail = "ada@example.com"
        store.clearIdentity()
        assertEquals("", store.userName)
        assertEquals("", store.userEmail)
        assertTrue(store.isGuest)
    }
}
