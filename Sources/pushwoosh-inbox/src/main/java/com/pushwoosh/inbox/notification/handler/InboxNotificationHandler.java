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

package com.pushwoosh.inbox.notification.handler;

import android.os.Bundle;
import androidx.annotation.WorkerThread;

import com.pushwoosh.inbox.internal.PushwooshInboxModule;
import com.pushwoosh.inbox.internal.data.InboxMessageInternal;
import com.pushwoosh.inbox.notification.InboxPayloadDataProvider;
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandler;

public class InboxNotificationHandler implements MessageSystemHandler {

	@Override
	@WorkerThread
	public boolean preHandleMessage(Bundle pushBundle) {
		if (InboxPayloadDataProvider.getInboxId(pushBundle) == null) {
			return false;
		}

		PushwooshInboxModule.getInboxRepository().addMessage(new InboxMessageInternal.Builder()
				.setPushBundle(pushBundle)
				.build());

		return false;
	}
}
