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

package com.pushwoosh.notification.handlers.notification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.chain.Chain;

public class NotificationOpenHandlerChain implements Chain<PushNotificationOpenHandler> {

	private final Collection<PushNotificationOpenHandler> pushNotificationOpenHandlers;

	private NotificationOpenHandlerChain(@NonNull final Collection<PushNotificationOpenHandler> pushNotificationOpenHandlers) {
		this.pushNotificationOpenHandlers = pushNotificationOpenHandlers;
	}

	@Override
	public Iterator<PushNotificationOpenHandler> getIterator() {
		return pushNotificationOpenHandlers.iterator();
	}

	@Override
	public void addItem(final PushNotificationOpenHandler item) {
		pushNotificationOpenHandlers.add(item);
	}

	@Override
	public void removeItem(final PushNotificationOpenHandler item) {
		pushNotificationOpenHandlers.remove(item);
	}

	public static final class Builder {

		private final Collection<PushNotificationOpenHandler> collection = new ArrayList<>();

		public Builder() {/*do nothing*/}

		Builder addMessagePreHandler(PushNotificationOpenHandler pushNotificationOpenHandler) {
			collection.add(pushNotificationOpenHandler);
			return this;
		}

		public NotificationOpenHandlerChain build() {
			return new NotificationOpenHandlerChain(collection);
		}
	}
}
