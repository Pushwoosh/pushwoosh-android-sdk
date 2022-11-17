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

package com.pushwoosh.inbox.network.data;

import com.pushwoosh.inbox.internal.data.InboxMessageStatus;

import org.json.JSONException;
import org.json.JSONObject;

import static com.pushwoosh.inbox.network.NetworkDataHelper.HASH;
import static com.pushwoosh.inbox.network.NetworkDataHelper.INBOX_STATUS;

public class UpdateInboxMessageStatusRequest extends BaseInboxRequest<Void> {
	private final long inboxOrder;
	private final InboxMessageStatus status;
	private final String hash;

	public UpdateInboxMessageStatusRequest(long inboxOrder, InboxMessageStatus status, String hash) {
		this.inboxOrder = inboxOrder;
		this.status = status;
		this.hash = hash;
	}

	@Override
	protected void buildParams(JSONObject params) throws JSONException {
		super.buildParams(params);
		params.put("inbox_code", String.valueOf(inboxOrder));
		params.put(INBOX_STATUS, status.getCode());
		params.put(HASH, hash);
	}

	@Override
	public String getMethod() {
		return "inboxStatus";
	}
}
