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

package com.pushwoosh.inbox.storage.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

import com.pushwoosh.inbox.internal.data.InboxMessageStatus;

public class MergeResult {
	public static MergeResult createEmpty() {
		return new MergeResult(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new ArrayList<>());
	}

	public static MergeResult createNull() {
		return new MergeResult();
	}

	private final Collection<String> newItems;
	private final Collection<String> updatedItems;
	private final Collection<String> deletedItems;
	private final Map<String, InboxMessageStatus> incorrectNetworkStatus;

	private MergeResult() {
		this(null, null, null, null);
	}

	private MergeResult(Collection<String> newItems, Collection<String> updatedItems, Map<String, InboxMessageStatus> incorrectNetworkStatus, Collection<String> deletedItems) {
		this.newItems = newItems == null ? Collections.emptyList() : newItems;
		this.incorrectNetworkStatus = incorrectNetworkStatus == null ? Collections.emptyMap() : incorrectNetworkStatus;
		this.updatedItems = updatedItems == null ? Collections.emptyList() : updatedItems;
		this.deletedItems = deletedItems == null ? Collections.emptyList() : deletedItems;
	}

	@NonNull
	public Collection<String> getDeletedItems() {
		return deletedItems;
	}

	@NonNull
	public Collection<String> getNewItems() {
		return newItems;
	}

	@NonNull
	public Map<String, InboxMessageStatus> getIncorrectNetworkStatus() {
		return incorrectNetworkStatus;
	}

	@NonNull
	public Collection<String> getUpdatedItems() {
		return updatedItems;
	}
}
