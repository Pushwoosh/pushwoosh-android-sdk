package com.pushwoosh.pushmart.ui.offers

import com.pushwoosh.pushmart.account.AccountOutcome
import com.pushwoosh.pushmart.sdk.FakeInboxGateway
import com.pushwoosh.pushmart.sdk.GatewayResult
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
        var outcome: AccountOutcome? = null
        actions.markAllRead { outcome = it }
        assertEquals(listOf("loadMessages", "readMessages:a,c"), inbox.calls)
        assertEquals(AccountOutcome.Success("Marked as read", "2 offers marked as read."), outcome)
        assertTrue(inbox.messages.all { it.isRead })
    }

    @Test
    fun markAllReadWithNothingUnreadReportsNothingToDo() {
        inbox.messages.add(InboxMessageSummary("b", isRead = true))
        var outcome: AccountOutcome? = null
        actions.markAllRead { outcome = it }
        assertEquals(listOf("loadMessages"), inbox.calls)
        assertEquals(AccountOutcome.Success("Nothing to do", "No unread offers."), outcome)
    }

    @Test
    fun clearReadDeletesOnlyReadMessages() {
        seed()
        var outcome: AccountOutcome? = null
        actions.clearRead { outcome = it }
        assertEquals(listOf("loadMessages", "deleteMessages:b"), inbox.calls)
        assertEquals(AccountOutcome.Success("Cleared", "1 read offers removed."), outcome)
        assertEquals(listOf("a", "c"), inbox.messages.map { it.code })
    }

    @Test
    fun clearReadWithNothingReadReportsNothingToDo() {
        inbox.messages.add(InboxMessageSummary("a", isRead = false))
        var outcome: AccountOutcome? = null
        actions.clearRead { outcome = it }
        assertEquals(listOf("loadMessages"), inbox.calls)
        assertEquals(AccountOutcome.Success("Nothing to do", "No read offers."), outcome)
    }

    @Test
    fun deleteAllDeletesEveryMessage() {
        seed()
        var outcome: AccountOutcome? = null
        actions.deleteAll { outcome = it }
        assertEquals(listOf("loadMessages", "deleteMessages:a,b,c"), inbox.calls)
        assertEquals(AccountOutcome.Success("Inbox cleared", "3 offers removed."), outcome)
        assertTrue(inbox.messages.isEmpty())
    }

    @Test
    fun deleteAllOnEmptyInboxReportsNothingToDo() {
        var outcome: AccountOutcome? = null
        actions.deleteAll { outcome = it }
        assertEquals(listOf("loadMessages"), inbox.calls)
        assertEquals(
            AccountOutcome.Success("Nothing to do", "Your inbox is already empty."), outcome)
    }

    @Test
    fun loadFailureReportsFailureWithoutMutations() {
        inbox.loadResult = GatewayResult.failure("offline")
        var outcome: AccountOutcome? = null
        actions.markAllRead { outcome = it }
        assertEquals(listOf("loadMessages"), inbox.calls)
        assertEquals(AccountOutcome.Failure("Inbox unavailable", "offline"), outcome)
    }
}
