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

package com.pushwoosh.inbox.repository.data;

import java.util.Collection;
import java.util.Collections;

import androidx.annotation.NonNull;

public class LoadResult {
	public static final LoadResult EMPTY = new LoadResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

	private Collection<String> inboxMessagesAdded;
	private Collection<String> inboxMessagesUpdated;
	private Collection<String> inboxMessagesDeleted;

	public LoadResult(Collection<String> inboxMessagesAdded, Collection<String> inboxMessagesUpdated, Collection<String> inboxMessagesDeleted) {
		this.inboxMessagesAdded = inboxMessagesAdded == null ? Collections.emptyList() : inboxMessagesAdded;
		this.inboxMessagesUpdated = inboxMessagesUpdated == null ? Collections.emptyList(): inboxMessagesUpdated;
		this.inboxMessagesDeleted = inboxMessagesDeleted == null ? Collections.emptyList() : inboxMessagesDeleted;
	}

	@NonNull
	public Collection<String> getInboxMessagesAdded() {
		return inboxMessagesAdded;
	}

	@NonNull
	public Collection<String> getInboxMessagesUpdated() {
		return inboxMessagesUpdated;
	}

	@NonNull
	public Collection<String> getInboxMessagesDeleted() {
		return inboxMessagesDeleted;
	}

	public boolean isEmpty() {
		return inboxMessagesAdded.isEmpty() && inboxMessagesUpdated.isEmpty() && inboxMessagesDeleted.isEmpty();
	}
}
