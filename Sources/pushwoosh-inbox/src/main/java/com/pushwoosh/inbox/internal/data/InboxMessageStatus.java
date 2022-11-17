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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

public enum InboxMessageStatus {
	CREATED(0),
	DELIVERED(1),
	READ(2),
	OPEN(3),
	DELETED_BY_USER(4),
	DELETED_FROM_SERVICE(5);

	private int code;

	InboxMessageStatus(int code) {
		this.code = code;
	}

	@Nullable
	public static InboxMessageStatus getByCode(int code) {
		for (InboxMessageStatus status : InboxMessageStatus.values()) {
			if (status.code == code) {
				return status;
			}
		}

		PWLog.error("Incorrect InboxMessageStatusCode: " + code);
		return null;
	}

	public static List<InboxMessageStatus> getActualCodes() {
		return Arrays.asList(CREATED, DELIVERED, READ, OPEN);
	}

	public int getCode() {
		return code;
	}

	public boolean isLowerStatus(InboxMessageStatus inboxMessageStatus) {
		switch (inboxMessageStatus) {
			case CREATED:
				return false;
			case DELIVERED:
				if (this.equals(CREATED)) {
					return true;
				}
				break;
			case READ:
				if (this.equals(CREATED) || this.equals(DELIVERED)) {
					return true;
				}
				break;
			case OPEN:
				if (this.equals(CREATED) || this.equals(DELIVERED) || this.equals(READ)) {
					return true;
				}
				break;
			case DELETED_BY_USER:
				if (this.equals(CREATED) || this.equals(DELIVERED) || this.equals(READ) || this.equals(OPEN)) {
					return true;
				}
				break;
			case DELETED_FROM_SERVICE:
				return true;
		}

		return false;
	}

	public Collection<InboxMessageStatus> getLowerStatus() {
		Collection<InboxMessageStatus> result = new ArrayList<>();
		for (InboxMessageStatus status : InboxMessageStatus.values()) {
			if (status.isLowerStatus(this)) {
				result.add(status);
			}
		}
		return result;
	}
}
