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

import com.pushwoosh.exception.UnregisterForPushNotificationException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.notification.event.DeregistrationErrorEvent;
import com.pushwoosh.notification.event.DeregistrationSuccessEvent;

final class UnregistrationCallbackHolder {
	private final Callback<String, UnregisterForPushNotificationException> callback;
	private Subscription<DeregistrationSuccessEvent> unregistrationSuccessSubscription;
	private Subscription<DeregistrationErrorEvent> unregistrationErrorSubscription;
	private static UnregistrationCallbackHolder currentCallbackHolder;

	private UnregistrationCallbackHolder(Callback<String, UnregisterForPushNotificationException> callback) {
		this.callback = callback;
	}

	private void subscribe() {
		unregistrationSuccessSubscription = EventBus.subscribe(DeregistrationSuccessEvent.class, (event) -> {
			unsubscribe();
			callback.process(Result.fromData(event.getData()));
		});

		unregistrationErrorSubscription = EventBus.subscribe(DeregistrationErrorEvent.class, (event) -> {
			unsubscribe();
			callback.process(Result.fromException(new UnregisterForPushNotificationException(event.getData())));
		});
	}

	private void unsubscribe() {
		if (unregistrationSuccessSubscription != null) {
			unregistrationSuccessSubscription.unsubscribe();
		}

		if (unregistrationErrorSubscription != null) {
			unregistrationErrorSubscription.unsubscribe();
		}
		currentCallbackHolder = null;
	}

	public static void setCallback(Callback<String, UnregisterForPushNotificationException> callback) {
		if (callback != null && currentCallbackHolder == null) {
			currentCallbackHolder = new UnregistrationCallbackHolder(callback);
			currentCallbackHolder.subscribe();
		}
	}
}
