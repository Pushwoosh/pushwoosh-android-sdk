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

package com.pushwoosh.inbox.data;

import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

/**
 * The Inbox message type.<br>
 * * Plain - without any action<br>
 * * RichMedia - contains a Rich media page,<br>
 * * URL - contains remote URL,<br>
 * * Deeplink - contains Deeplink<br>
 */
public enum InboxMessageType {
	PLAIN(0),
	RICH_MEDIA(1),
	URL(2),
	DEEP_LINK(3);

	private int code;

	InboxMessageType(int code) {
		this.code = code;
	}

	@Nullable
	public static InboxMessageType getByCode(int code) {
		for (InboxMessageType type : InboxMessageType.values()) {
			if (type.code == code) {
				return type;
			}
		}

		PWLog.error("Incorrect type of InboxMessageType: " + code);
		return null;
	}

	public int getCode() {
		return code;
	}
}
