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

package com.pushwoosh.inbox.storage.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.inbox.internal.data.InboxMessageInternal;
import com.pushwoosh.inbox.internal.data.InboxMessageStatus;
import com.pushwoosh.inbox.storage.InboxStorage;
import com.pushwoosh.inbox.storage.data.MergeResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DbInboxStorage implements InboxStorage {

	private final InboxDbHelper inboxDbHelper;

	public DbInboxStorage(InboxDbHelper inboxDbHelper) {
		this.inboxDbHelper = inboxDbHelper;
	}

	@Override
	public void deleteList(@NonNull Collection<String> codes) {
		if (codes.isEmpty()) {
			return;
		}

		inboxDbHelper.removeItems(codes);
	}

	@NonNull
	@Override
	public MergeResult mergeState(@NonNull Collection<InboxMessageInternal> inboxMessageInternals, boolean fullList) {
		if (inboxMessageInternals.isEmpty() && !fullList) {
			return MergeResult.createNull();
		}
		return inboxDbHelper.createOrUpdate(inboxMessageInternals, fullList);
	}

	@NonNull
	@Override
	public Collection<InboxMessageInternal> getAllActualMessages() {
		final Collection<InboxMessageInternal> allActualWithStatus = inboxDbHelper.getAllActualWithStatus(InboxMessageStatus.getActualCodes());
		return allActualWithStatus == null ? Collections.emptyList() : allActualWithStatus;
	}

	@NonNull
	@Override
	public Collection<InboxMessageInternal> getActualMessages(long sortOrder, int limit) {
		final Collection<InboxMessageInternal> allActualWithStatus = inboxDbHelper.getActualMessagesWithStatus(InboxMessageStatus.getActualCodes(), sortOrder, limit);
		return allActualWithStatus == null ? Collections.emptyList() : allActualWithStatus;
	}

	@Override
	public Collection<String> updateStatus(@NonNull String code, @NonNull InboxMessageStatus inboxMessageStatus) {
		return inboxDbHelper.changeStatus(Collections.singleton(code), inboxMessageStatus);
	}

	@Override
	public int getUnreadInboxCount() {
		final Integer actualCountWithStatus = inboxDbHelper.getActualCountWithStatus(InboxMessageStatus.READ.getLowerStatus());
		return actualCountWithStatus == null ? 0 : actualCountWithStatus;
	}

	@Override
	public int getCountWithNoActionPerformed() {
		final Integer actualCountWithStatus = inboxDbHelper.getActualCountWithStatus(InboxMessageStatus.OPEN.getLowerStatus());
		return actualCountWithStatus == null ? 0 : actualCountWithStatus;
	}

	@Override
	public int getTotalCount() {
		final Integer actualCountWithStatus = inboxDbHelper.getActualCountWithStatus(InboxMessageStatus.DELETED_BY_USER.getLowerStatus());
		return actualCountWithStatus == null ? 0 : actualCountWithStatus;
	}

	@NonNull
	@Override
	public Collection<InboxMessageInternal> getActualInboxMessages(Collection<String> codes) {
		final List<InboxMessageInternal> byCode = inboxDbHelper.getById(codes);
		return byCode == null ? Collections.emptyList() : byCode;
	}

	@Nullable
	@Override
	public InboxMessageInternal getActualInboxMessage(String code) {
		return inboxDbHelper.getById(code);
	}

	@NonNull
	@Override
	public Collection<InboxMessageInternal> getAllPushMessages() {
		final Collection<InboxMessageInternal> allActualWithStatus = inboxDbHelper.getAllPushMessages();
		return allActualWithStatus == null ? Collections.emptyList() : allActualWithStatus;
	}
}
