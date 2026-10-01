package com.pushwoosh.pushmart.sdk

import com.pushwoosh.function.Callback
import com.pushwoosh.function.Result
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.exception.InboxMessagesException

class RealInboxGateway : InboxGateway {
    override fun loadMessages(callback: (GatewayResult<List<InboxMessageSummary>>) -> Unit) {
        PushwooshInbox.loadMessages { result ->
            if (result.isSuccess) {
                val summaries =
                    result.data.orEmpty().map { InboxMessageSummary(it.code, it.isRead) }
                callback(GatewayResult.success(summaries))
            } else {
                callback(GatewayResult.failure(result.exception?.message ?: "Inbox load failed"))
            }
        }
    }

    override fun readMessages(codes: Collection<String>) {
        PushwooshInbox.readMessages(codes)
    }

    override fun deleteMessages(codes: Collection<String>) {
        PushwooshInbox.deleteMessages(codes)
    }

    override fun observeCounts(listener: (total: Int, unread: Int) -> Unit): Registration {
        var total = 0
        var unread = 0
        val totalCallback =
            Callback<Int, InboxMessagesException> { result ->
                total = result.countOrZero()
                listener(total, unread)
            }
        val unreadCallback =
            Callback<Int, InboxMessagesException> { result ->
                unread = result.countOrZero()
                listener(total, unread)
            }
        PushwooshInbox.registerMessagesCountObserver(totalCallback)
        PushwooshInbox.registerUnreadMessagesCountObserver(unreadCallback)
        return object : Registration {
            override fun unregister() {
                PushwooshInbox.unregisterMessagesCountObserver(totalCallback)
                PushwooshInbox.unregisterUnreadMessagesCountObserver(unreadCallback)
            }
        }
    }

    override fun fetchCounts(callback: (total: Int, unread: Int) -> Unit) {
        PushwooshInbox.messagesCount { totalResult ->
            val total = totalResult.countOrZero()
            PushwooshInbox.unreadMessagesCount { unreadResult ->
                callback(total, unreadResult.countOrZero())
            }
        }
    }

    private fun Result<Int, InboxMessagesException>.countOrZero(): Int =
        if (isSuccess) data ?: 0 else 0
}
