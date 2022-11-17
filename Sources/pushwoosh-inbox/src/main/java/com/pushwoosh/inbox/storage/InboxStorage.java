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

package com.pushwoosh.inbox.storage;

import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.pushwoosh.inbox.internal.data.InboxMessageInternal;
import com.pushwoosh.inbox.internal.data.InboxMessageStatus;
import com.pushwoosh.inbox.storage.data.MergeResult;

public interface InboxStorage {
	/**
	 * Delete list of {@link InboxMessageInternal}
	 *
	 * @param ids - list of {@link InboxMessageInternal#id} which must be deleted
	 */
	@WorkerThread
	void deleteList(@NonNull Collection<String> ids);

	/**
	 * Merge state with provided list
	 *
	 * @param inboxMessageInternals - list for merge
	 * @param fullList - if true delete all others from db
	 * @return map where key is {@link InboxMessageInternal#id} value is {@link InboxMessageInternal#inboxMessageStatus} which should be updated on service
	 */
	@WorkerThread
	@NonNull
	MergeResult mergeState(@NonNull Collection<InboxMessageInternal> inboxMessageInternals, boolean fullList);

	@WorkerThread
	@NonNull
	Collection<InboxMessageInternal> getActualInboxMessages(Collection<String> codes);

	@WorkerThread
	@Nullable
	InboxMessageInternal getActualInboxMessage(String code);
	/**
	 * Obtain all actual (not expired and not deleted) the list of {@link InboxMessageInternal}
	 *
	 * @return the list of actual {@link InboxMessageInternal}
	 */
	@WorkerThread
	@NonNull
	Collection<InboxMessageInternal> getAllActualMessages();

	@WorkerThread
	@NonNull
	Collection<InboxMessageInternal> getActualMessages(long order, int limit);

	@WorkerThread
	@NonNull
	Collection<InboxMessageInternal> getAllPushMessages();

	@WorkerThread
	Collection<String> updateStatus(@NonNull String id, @NonNull InboxMessageStatus inboxMessageStatus);

	@WorkerThread
	int getUnreadInboxCount();

	@WorkerThread
	int getCountWithNoActionPerformed();

	@WorkerThread
	int getTotalCount();
}
