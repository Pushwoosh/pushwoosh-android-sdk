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
 * EXSS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.notification.handlers.message.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.chain.Chain;

class MessageHandleChain implements Chain<MessageHandler> {

	private final Collection<MessageHandler> messageHandlers;

	private MessageHandleChain(@NonNull final Collection<MessageHandler> messageHandlers) {
		this.messageHandlers = messageHandlers;
	}

	@Override
	public Iterator<MessageHandler> getIterator() {
		return messageHandlers.iterator();
	}

	@Override
	public void addItem(final MessageHandler item) {
		messageHandlers.add(item);
	}

	@Override
	public void removeItem(final MessageHandler item) {
		messageHandlers.remove(item);
	}

	public static final class Builder {

		private final Collection<MessageHandler> collection = new ArrayList<>();

		public Builder() {/*do nothing*/}

		Builder addMessageHandler(MessageHandler messageHandler) {
			collection.add(messageHandler);
			return this;
		}

		public MessageHandleChain build() {
			return new MessageHandleChain(collection);
		}
	}
}
