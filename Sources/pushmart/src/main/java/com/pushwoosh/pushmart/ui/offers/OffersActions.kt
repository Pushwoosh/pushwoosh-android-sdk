package com.pushwoosh.pushmart.ui.offers

import com.pushwoosh.pushmart.sdk.InboxGateway
import com.pushwoosh.pushmart.sdk.InboxMessageSummary

class OffersActions(private val inbox: InboxGateway) {
    fun refresh(onResult: (syncFailed: Boolean) -> Unit) {
        inbox.loadMessages { result -> onResult(!result.isSuccess) }
    }

    fun markAllRead() {
        act(select = { messages -> messages.filter { !it.isRead } }, apply = inbox::readMessages)
    }

    fun clearRead() {
        act(select = { messages -> messages.filter { it.isRead } }, apply = inbox::deleteMessages)
    }

    fun deleteAll() {
        act(select = { messages -> messages }, apply = inbox::deleteMessages)
    }

    private fun act(
        select: (List<InboxMessageSummary>) -> List<InboxMessageSummary>,
        apply: (Collection<String>) -> Unit,
    ) {
        inbox.loadMessages { result ->
            val codes = select(result.data.orEmpty()).map { it.code }
            if (codes.isNotEmpty()) apply(codes)
        }
    }
}
