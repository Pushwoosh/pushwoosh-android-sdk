/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inbox.ui.model.repository

import com.pushwoosh.function.Callback
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.event.InboxMessagesUpdatedEvent
import com.pushwoosh.inbox.exception.InboxMessagesException
import com.pushwoosh.internal.event.EventBus
import com.pushwoosh.internal.event.Subscription
import com.pushwoosh.internal.utils.PWLog

object InboxRepository {

    private val callbacks = mutableListOf<(InboxEvent) -> Unit>()
    private var currentInboxEvent: InboxEvent? = null
    private val currentInboxMessages: MutableList<InboxMessage> = ArrayList()

    fun addCallback(callback: (InboxEvent) -> Unit) {
        callbacks.add(callback)
        if (currentInboxEvent != null) {
            callback.invoke(currentInboxEvent!!)
        }
    }

    fun removeCallback(callback: (InboxEvent) -> Unit) {
        callbacks.remove(callback)
    }

    fun subscribeToEvent(): Subscription<InboxMessagesUpdatedEvent> =
            EventBus.subscribe(InboxMessagesUpdatedEvent::class.java, { event ->
                val added: MutableCollection<InboxMessage> = ArrayList(event.messagesAdded.filter { !currentInboxMessages.contains(it) })
                val updated = event.messagesUpdated.filter {
                    if (currentInboxMessages.contains(it)) {
                        currentInboxMessages[currentInboxMessages.indexOf(it)] = it
                        true
                    } else {
                        added.add(it)
                        false
                    }
                }

                currentInboxMessages.addAll(added)

                if (currentInboxMessages.isEmpty()) {
                    return@subscribe
                }

                val deleted = currentInboxMessages.remove {
                    event.messagesDeleted.contains(it.code)
                }

                updateEvent(if (currentInboxMessages.isEmpty()) InboxEvent.InboxEmpty() else InboxEvent.InboxMessagesUpdated(added, updated, deleted))
            })

    @Suppress("UNUSED_PARAMETER")
    fun loadInbox(forceRequest: Boolean, inboxMessage: InboxMessage?, limit:Int) {
        updateEvent(InboxEvent.Loading())
        PushwooshInbox.loadMessages(getLoadMessagesCallback(isLoadingCachedMessages = false), inboxMessage, limit)
    }

    fun loadCachedInbox(inboxMessage: InboxMessage?, limit:Int) : Collection<InboxMessage> {
        return PushwooshInbox.loadCachedMessages(inboxMessage, limit)
    }

    fun loadCachedInboxAsync(inboxMessage: InboxMessage?, limit:Int) {
        PushwooshInbox.loadCachedMessages(getLoadMessagesCallback(isLoadingCachedMessages = true), inboxMessage, limit)
    }

    private fun getLoadMessagesCallback(isLoadingCachedMessages : Boolean) : Callback<Collection<InboxMessage>, InboxMessagesException> {
        return Callback{ result ->
            PWLog.noise("loadInbox", "result isSuccess: " + result.isSuccess)
            updateEvent(InboxEvent.FinishLoading())
            val data = result.data

            currentInboxMessages.clear()
            data?.let{
                currentInboxMessages.addAll(ArrayList(data))
            }

            if (data != null && data.isNotEmpty()) {
                if (isLoadingCachedMessages) {
                    updateEvent(InboxEvent.SuccessLoadingCache(currentInboxMessages))
                } else {
                    updateEvent(InboxEvent.SuccessLoading(currentInboxMessages))
                }
            }
            val error = result.exception

            if (error != null) {
                updateEvent(InboxEvent.FailedLoading(error))
            }

            if (error == null && (data == null || data.isEmpty())) {
                updateEvent(InboxEvent.InboxEmpty())
            }
        }
    }

    private fun updateEvent(inboxEvent: InboxEvent) {
        PWLog.noise("updateEvent", "InboxEvent: " + inboxEvent.javaClass.name)
        currentInboxEvent = inboxEvent
        callbacks.forEach { it(inboxEvent) }
    }

    fun removeItem(inboxMessage: InboxMessage) {
        PushwooshInbox.deleteMessage(inboxMessage.code)
    }
}

sealed class InboxEvent {
    class OnCreate : InboxEvent()
    class Loading : InboxEvent()
    class FinishLoading : InboxEvent()
    class FailedLoading(val error: Throwable) : InboxEvent()
    class SuccessLoadingCache(val inboxMessages: Collection<InboxMessage>) : InboxEvent()
    class SuccessLoading(val inboxMessages: Collection<InboxMessage>) : InboxEvent()
    class InboxEmpty : InboxEvent()
    class InboxMessagesUpdated(val addedInboxMessages: Collection<InboxMessage>,
                               val updatedInboxMessages: Collection<InboxMessage>,
                               val deleted: Collection<InboxMessage>) : InboxEvent()
    class RestoreState: InboxEvent()
}

fun <T> MutableCollection<T>.remove(filter: (T) -> Boolean): Collection<T> {
    val deleted = ArrayList<T>()
    val each = iterator()
    while (each.hasNext()) {
        val next = each.next()
        if (filter(next)) {
            each.remove()
            deleted.add(next)
        }
    }
    return deleted
}