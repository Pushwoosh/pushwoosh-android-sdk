package com.pushwoosh.pushmart.ui.offers

import org.junit.Assert.assertEquals
import org.junit.Test

class InboxCounterTest {
    @Test
    fun emptyInboxSaysNoMessagesYet() {
        assertEquals("No messages yet", InboxCounter.format(0, 0))
    }

    @Test
    fun allReadWhenUnreadIsZero() {
        assertEquals("3 messages · all read", InboxCounter.format(3, 0))
    }

    @Test
    fun showsUnreadCount() {
        assertEquals("3 messages · 1 unread", InboxCounter.format(3, 1))
    }

    @Test
    fun singleMessageKeepsPluralForm() {
        assertEquals("1 messages · 1 unread", InboxCounter.format(1, 1))
    }

    @Test
    fun unreadIsClampedToTotal() {
        assertEquals("2 messages · 2 unread", InboxCounter.format(2, 5))
    }

    @Test
    fun negativeOrStaleUnreadIsTreatedAsZero() {
        assertEquals("2 messages · all read", InboxCounter.format(2, -1))
        assertEquals("No messages yet", InboxCounter.format(0, 3))
    }
}
