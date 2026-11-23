package com.pushwoosh.pushmart.sdk

import android.os.Handler
import android.os.Looper
import com.pushwoosh.function.Callback
import com.pushwoosh.function.Result
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.exception.InboxMessagesException

class RealInboxGateway : InboxGateway {
    private val handler = Handler(Looper.getMainLooper())
    private val countSinks = mutableListOf<(total: Int, unread: Int) -> Unit>()

    override fun loadMessages(callback: (GatewayResult<List<InboxMessageSummary>>) -> Unit) {
        PushwooshInbox.loadMessages { result ->
            val summaries = result.data.orEmpty().map { InboxMessageSummary(it.code, it.isRead) }
            if (result.isSuccess) {
                callback(GatewayResult.success(summaries))
            } else {
                callback(
                    GatewayResult.failure(
                        result.exception?.message ?: "Inbox load failed", summaries))
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
        var total: Int? = null
        var unread: Int? = null
        fun emit() {
            listener(total ?: return, unread ?: return)
        }
        val totalCallback =
            Callback<Int, InboxMessagesException> { result ->
                total = result.count()
                emit()
            }
        val unreadCallback =
            Callback<Int, InboxMessagesException> { result ->
                unread = result.count()
                emit()
            }
        val sink: (Int, Int) -> Unit = { cachedTotal, cachedUnread ->
            total = cachedTotal
            unread = cachedUnread
            emit()
        }
        countSinks.add(sink)
        PushwooshInbox.registerMessagesCountObserver(totalCallback)
        PushwooshInbox.registerUnreadMessagesCountObserver(unreadCallback)
        loadCachedCounts { cachedTotal, cachedUnread ->
            if (sink in countSinks) sink(cachedTotal, cachedUnread)
        }
        return object : Registration {
            override fun unregister() {
                countSinks.remove(sink)
                PushwooshInbox.unregisterMessagesCountObserver(totalCallback)
                PushwooshInbox.unregisterUnreadMessagesCountObserver(unreadCallback)
            }
        }
    }

    override fun resync() {
        // The SDK's AppIdChanged handler wipes the old rows first.
        handler.post { reload() }
        // The SDK's 60 s update window survives an app code change.
        handler.postDelayed({ reload() }, RESYNC_RETRY_MS)
    }

    /** The SDK count observers stay silent when the window blocks a load or wipes the database. */
    private fun reload() {
        PushwooshInbox.loadMessages {
            loadCachedCounts { total, unread -> countSinks.toList().forEach { it(total, unread) } }
        }
    }

    /** The same rules as the SDK counters: every live message, and those not read yet. */
    private fun loadCachedCounts(callback: (total: Int, unread: Int) -> Unit) {
        PushwooshInbox.loadCachedMessages(
            { result ->
                val messages = result.data ?: return@loadCachedMessages
                callback(messages.size, messages.count { !it.isRead })
            },
            null,
            ALL_MESSAGES)
    }

    private fun Result<Int, InboxMessagesException>.count(): Int = data ?: 0

    private companion object {
        const val RESYNC_RETRY_MS = 61_000L
        const val ALL_MESSAGES = -1
    }
}
