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

package com.pushwoosh.inbox.internal.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.inbox.internal.utils.DateProvider;
import com.pushwoosh.inbox.notification.InboxPayloadDataProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

public class InboxMessageImpl implements InboxMessage {
	private static final long serialVersionUID = 5094347184829087767L;
	private final InboxMessageInternal inboxMessageInternal;

	public InboxMessageImpl(InboxMessageInternal inboxMessageInternal) {
		this.inboxMessageInternal = inboxMessageInternal;
	}

	@NonNull
	@Override
	public String getCode() {
		return inboxMessageInternal.getId();
	}

	@Nullable
	@Override
	public String getTitle() {
		return inboxMessageInternal.getTitle();
	}

	@Nullable
	@Override
	public String getImageUrl() {
		return inboxMessageInternal.getImage();
	}

	@NonNull
	@Override
	public String getMessage() {
		return inboxMessageInternal.getMessage();
	}

	@NonNull
	@Override
	public Date getSendDate() {
		return DateProvider.map(inboxMessageInternal.getSendDate());
	}

	@NonNull
	@Override
	public String getISO8601SendDate() {
		Date date = DateProvider.map(inboxMessageInternal.getSendDate());
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		return format.format(date);
	}

	@NonNull
	@Override
	public InboxMessageType getType() {
		return InboxPayloadDataProvider.getInboxType(inboxMessageInternal.getActionParams());
	}

	@Nullable
	@Override
	public String getBannerUrl() {
		return inboxMessageInternal.getBannerUrl();
	}

	@Override
	public boolean isRead() {
		return inboxMessageInternal.isRead();
	}

	@Override
	public boolean isActionPerformed() {
		return inboxMessageInternal.isActionCompleted();
	}

	@Override
	public String getActionParams() {
		return inboxMessageInternal.getActionParams();
	}

	public InboxMessageInternal getInboxMessageInternal() {
		return inboxMessageInternal;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		InboxMessageImpl that = (InboxMessageImpl) o;

		return inboxMessageInternal != null ? inboxMessageInternal.equals(that.inboxMessageInternal) : that.inboxMessageInternal == null;
	}

	@Override
	public int hashCode() {
		return inboxMessageInternal != null ? inboxMessageInternal.hashCode() : 0;
	}

	@Override
	public int compareTo(@NonNull InboxMessage o) {
		if (o instanceof InboxMessageImpl) {
			return inboxMessageInternal.compareTo(((InboxMessageImpl) o).inboxMessageInternal);
		}

		return -1;
	}
}
