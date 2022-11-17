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

package com.pushwoosh.inbox.event;

import java.util.Collection;
import java.util.Collections;

import androidx.annotation.NonNull;

import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.internal.event.Event;

public class InboxMessagesUpdatedEvent implements Event {
	private Collection<InboxMessage> messagesAdded;
	private Collection<InboxMessage> messagesUpdated;
	private Collection<String> messagesDeleted;

	InboxMessagesUpdatedEvent(Collection<InboxMessage> messagesAdded, Collection<InboxMessage> inboxMessagesUpdated, Collection<String> inboxMessagesDeleted) {
		this.messagesAdded = messagesAdded == null ? Collections.emptyList() : messagesAdded;
		this.messagesUpdated = inboxMessagesUpdated == null ? Collections.emptyList() : inboxMessagesUpdated;
		this.messagesDeleted = inboxMessagesDeleted == null ? Collections.emptyList() : inboxMessagesDeleted;
	}

	@NonNull
	public Collection<InboxMessage> getMessagesAdded() {
		return messagesAdded;
	}

	@NonNull
	public Collection<InboxMessage> getMessagesUpdated() {
		return messagesUpdated;
	}

	@NonNull
	public Collection<String> getMessagesDeleted() {
		return messagesDeleted;
	}
}
