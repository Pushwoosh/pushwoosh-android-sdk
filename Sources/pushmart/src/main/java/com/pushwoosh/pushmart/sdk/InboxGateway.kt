package com.pushwoosh.pushmart.sdk

data class InboxMessageSummary(val code: String, val isRead: Boolean)

interface Registration {
    fun unregister()
}

interface InboxGateway {
    fun loadMessages(callback: (GatewayResult<List<InboxMessageSummary>>) -> Unit)

    fun readMessages(codes: Collection<String>)

    fun deleteMessages(codes: Collection<String>)

    fun observeCounts(listener: (total: Int, unread: Int) -> Unit): Registration

    fun fetchCounts(callback: (total: Int, unread: Int) -> Unit)
}
