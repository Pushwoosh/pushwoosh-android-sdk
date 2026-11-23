package com.pushwoosh.pushmart.sdk

class FakeInboxGateway : InboxGateway {
    val calls = mutableListOf<String>()
    val messages = mutableListOf<InboxMessageSummary>()
    private val listeners = mutableListOf<(Int, Int) -> Unit>()

    /** When non-null, every sync fails with it and, like the SDK, still delivers local storage. */
    var syncError: String? = null

    override fun loadMessages(callback: (GatewayResult<List<InboxMessageSummary>>) -> Unit) {
        calls.add("loadMessages")
        val error = syncError
        callback(
            if (error == null) GatewayResult.success(messages.toList())
            else GatewayResult.failure(error, messages.toList()))
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
        listener(messages.size, messages.count { !it.isRead })
        return object : Registration {
            override fun unregister() {
                calls.add("unregister")
                listeners.remove(listener)
            }
        }
    }

    override fun resync() {
        calls.add("resync")
    }

    /** Pushes the current counts to every registered listener, like the SDK observers do. */
    fun emitCounts() {
        val total = messages.size
        val unread = messages.count { !it.isRead }
        for (listener in listeners.toList()) listener(total, unread)
    }
}
