package com.pushwoosh.pushmart.sdk

class FakeInboxGateway : InboxGateway {
    val calls = mutableListOf<String>()
    val messages = mutableListOf<InboxMessageSummary>()
    private val listeners = mutableListOf<(Int, Int) -> Unit>()

    /** When non-null, [loadMessages] delivers this instead of a snapshot of [messages]. */
    var loadResult: GatewayResult<List<InboxMessageSummary>>? = null

    override fun loadMessages(callback: (GatewayResult<List<InboxMessageSummary>>) -> Unit) {
        calls.add("loadMessages")
        callback(loadResult ?: GatewayResult.success(messages.toList()))
    }

    override fun readMessages(codes: Collection<String>) {
        calls.add("readMessages:" + codes.joinToString(","))
        for (i in messages.indices) {
            if (messages[i].code in codes) messages[i] = messages[i].copy(isRead = true)
        }
    }

    override fun deleteMessages(codes: Collection<String>) {
        calls.add("deleteMessages:" + codes.joinToString(","))
        messages.removeAll { it.code in codes }
    }

    override fun observeCounts(listener: (total: Int, unread: Int) -> Unit): Registration {
        calls.add("observeCounts")
        listeners.add(listener)
        return object : Registration {
            override fun unregister() {
                calls.add("unregister")
                listeners.remove(listener)
            }
        }
    }

    override fun fetchCounts(callback: (total: Int, unread: Int) -> Unit) {
        calls.add("fetchCounts")
        callback(messages.size, messages.count { !it.isRead })
    }

    /** Pushes the current counts to every registered listener, like the SDK observers do. */
    fun emitCounts() {
        val total = messages.size
        val unread = messages.count { !it.isRead }
        for (listener in listeners.toList()) listener(total, unread)
    }
}
