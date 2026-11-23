package com.pushwoosh.pushmart.ui.offers

import com.pushwoosh.pushmart.sdk.FakeInboxGateway
import com.pushwoosh.pushmart.sdk.InboxMessageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OffersActionsTest {
    private val inbox = FakeInboxGateway()
    private val actions = OffersActions(inbox)

    private fun seed() {
        inbox.messages.add(InboxMessageSummary("a", isRead = false))
        inbox.messages.add(InboxMessageSummary("b", isRead = true))
        inbox.messages.add(InboxMessageSummary("c", isRead = false))
    }

    @Test
    fun markAllReadReadsOnlyUnreadMessages() {
        seed()
        actions.markAllRead()
        assertEquals(listOf("loadMessages", "readMessages:a,c"), inbox.calls)
        assertTrue(inbox.messages.all { it.isRead })
    }

    @Test
    fun markAllReadWithNothingUnreadOnlyLoads() {
        inbox.messages.add(InboxMessageSummary("b", isRead = true))
        actions.markAllRead()
        assertEquals(listOf("loadMessages"), inbox.calls)
    }

    @Test
    fun clearReadDeletesOnlyReadMessages() {
        seed()
        actions.clearRead()
        assertEquals(listOf("loadMessages", "deleteMessages:b"), inbox.calls)
        assertEquals(listOf("a", "c"), inbox.messages.map { it.code })
    }

    @Test
    fun clearReadWithNothingReadOnlyLoads() {
        inbox.messages.add(InboxMessageSummary("a", isRead = false))
        actions.clearRead()
        assertEquals(listOf("loadMessages"), inbox.calls)
    }

    @Test
    fun deleteAllDeletesEveryMessage() {
        seed()
        actions.deleteAll()
        assertEquals(listOf("loadMessages", "deleteMessages:a,b,c"), inbox.calls)
        assertTrue(inbox.messages.isEmpty())
    }

    @Test
    fun deleteAllOnEmptyInboxOnlyLoads() {
        actions.deleteAll()
        assertEquals(listOf("loadMessages"), inbox.calls)
    }

    @Test
    fun failedSyncActsOnCachedMessages() {
        seed()
        inbox.syncError = "offline"
        actions.clearRead()
        actions.markAllRead()
        actions.deleteAll()
        assertEquals(
            listOf(
                "loadMessages",
                "deleteMessages:b",
                "loadMessages",
                "readMessages:a,c",
                "loadMessages",
                "deleteMessages:a,c"),
            inbox.calls)
        assertTrue(inbox.messages.isEmpty())
    }
}
