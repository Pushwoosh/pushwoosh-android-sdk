package com.pushwoosh.pushmart.ui.offers

import com.pushwoosh.pushmart.account.AccountOutcome
import com.pushwoosh.pushmart.sdk.InboxGateway
import com.pushwoosh.pushmart.sdk.InboxMessageSummary

class OffersActions(private val inbox: InboxGateway) {
    fun markAllRead(onResult: (AccountOutcome) -> Unit) {
        act(
            onResult,
            select = { messages -> messages.filter { !it.isRead } },
            apply = inbox::readMessages,
            nothing = "No unread offers.",
            done = { n -> AccountOutcome.Success("Marked as read", "$n offers marked as read.") })
    }

    fun clearRead(onResult: (AccountOutcome) -> Unit) {
        act(
            onResult,
            select = { messages -> messages.filter { it.isRead } },
            apply = inbox::deleteMessages,
            nothing = "No read offers.",
            done = { n -> AccountOutcome.Success("Cleared", "$n read offers removed.") })
    }

    fun deleteAll(onResult: (AccountOutcome) -> Unit) {
        act(
            onResult,
            select = { messages -> messages },
            apply = inbox::deleteMessages,
            nothing = "Your inbox is already empty.",
            done = { n -> AccountOutcome.Success("Inbox cleared", "$n offers removed.") })
    }

    private fun act(
        onResult: (AccountOutcome) -> Unit,
        select: (List<InboxMessageSummary>) -> List<InboxMessageSummary>,
        apply: (Collection<String>) -> Unit,
        nothing: String,
        done: (Int) -> AccountOutcome,
    ) {
        inbox.loadMessages { result ->
            if (!result.isSuccess) {
                onResult(AccountOutcome.Failure("Inbox unavailable", result.error.orEmpty()))
                return@loadMessages
            }
            val codes = select(result.data.orEmpty()).map { it.code }
            if (codes.isEmpty()) {
                onResult(AccountOutcome.Success("Nothing to do", nothing))
                return@loadMessages
            }
            apply(codes)
            onResult(done(codes.size))
        }
    }
}
