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

import com.pushwoosh.inbox.internal.data.InboxMessageInternal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GetInboxMessagesResponse {
	private static final String NEW_INBOX = "new_inbox";
	private static final String DELETED_INBOX = "deleted";
	private static final String MESSAGES_INBOX = "messages";

	private Collection<String> deleted;
	private int newInboxCount;
	private List<InboxMessageInternal> messages;

	GetInboxMessagesResponse(JSONObject jsonObject) throws JSONException {
		if (!jsonObject.isNull(NEW_INBOX)) {
			newInboxCount = jsonObject.getInt(NEW_INBOX);
		}

		if (!jsonObject.isNull(DELETED_INBOX)) {
			final JSONArray deletedJson = jsonObject.getJSONArray(DELETED_INBOX);
			deleted = new ArrayList<>(deletedJson.length());
			for (int i = 0; i < deletedJson.length(); i++) {
				deleted.add(deletedJson.getString(i));
			}
		} else {
			deleted = Collections.emptyList();
		}


		if (!jsonObject.isNull(MESSAGES_INBOX)) {
			final JSONArray messagesJson = jsonObject.getJSONArray(MESSAGES_INBOX);
			messages = new ArrayList<>(messagesJson.length());
			for (int i = 0; i < messagesJson.length(); i++) {
				try {
					messages.add(new InboxMessageInternal.Builder()
										 .setJsonObject(messagesJson.getJSONObject(i))
										 .build());
				} catch (InboxMessageInternal.InboxInvalidArgumentException ignore) {

				}
			}
		} else {
			messages = Collections.emptyList();
		}
	}

	public Collection<String> getDeleted() {
		return deleted;
	}

	public int getNewInboxCount() {
		return newInboxCount;
	}

	public List<InboxMessageInternal> getMessages() {
		return messages;
	}
}
