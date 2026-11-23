package com.pushwoosh.pushmart.sdk

data class InboxMessageSummary(val code: String, val isRead: Boolean)

interface Registration {
    fun unregister()
}

/** Counts and messages always reflect local storage, also when the sync behind them failed. */
interface InboxGateway {
    /** A failed sync is a failure that still carries the cached messages. */
    fun loadMessages(callback: (GatewayResult<List<InboxMessageSummary>>) -> Unit)

    fun readMessages(codes: Collection<String>)

    fun deleteMessages(codes: Collection<String>)

    /**
     * First call: the cached counts, read without a sync. After that it fires only when both counts
     * are known.
     */
    fun observeCounts(listener: (total: Int, unread: Int) -> Unit): Registration

    /** Reloads the inbox after an app code switch, now and once more after the SDK sync window. */
    fun resync()
}
