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

package com.pushwoosh.notification;

import android.os.Bundle;

import com.pushwoosh.internal.chain.Chain;
import com.pushwoosh.notification.handlers.Required;
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandler;
import com.pushwoosh.notification.handlers.message.user.MessageHandler;

import java.util.Iterator;

 class PushMessageHandler {

	private final Chain<MessageSystemHandler> messagePreHandleChain;
	private final Chain<MessageHandler> messageHandleChain;

	PushMessageHandler(final Chain<MessageSystemHandler> messageSystemHandleChain, final Chain<MessageHandler> messageHandleProcessor) {
		this.messagePreHandleChain = messageSystemHandleChain;
		this.messageHandleChain = messageHandleProcessor;
	}

	boolean preHandleMessage(Bundle pushBundle) {
		Iterator<MessageSystemHandler> iterator = messagePreHandleChain.getIterator();
		boolean result = false;
		while (iterator.hasNext()) {
			MessageSystemHandler next = iterator.next();
			result = next.preHandleMessage(pushBundle) || result;
		}

		return result;
	}

	void handlePushMessage(PushMessage message, boolean isHandled) {
		Iterator<MessageHandler> iterator = messageHandleChain.getIterator();
		while (iterator.hasNext()) {
			MessageHandler handler = iterator.next();
			// If message didn't handled or handler is required than handle push message
			if(!isHandled || handler instanceof Required) {
				handler.handlePushMessage(message);
			}
		}
	}
}
