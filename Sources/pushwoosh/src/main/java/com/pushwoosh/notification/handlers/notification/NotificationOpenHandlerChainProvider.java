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

import androidx.annotation.NonNull;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.chain.Chain;

public final class NotificationOpenHandlerChainProvider {

	private NotificationOpenHandlerChainProvider() {/*do nothing*/}

	private static Chain<PushNotificationOpenHandler> pushNotificationOpenHandlerChain;

	public static void init() {
		pushNotificationOpenHandlerChain = generateDefault();
	}

	/**
	 * Use this method for adding some not default {@link com.pushwoosh.notification.handlers.notification.PushNotificationOpenHandler}
	 * If you want to add new PushNotificationOpenHandler use {@link com.pushwoosh.internal.Plugin}
	 * @return current chain of PushNotificationOpenHandler
	 */
	@NonNull
	public static Chain<PushNotificationOpenHandler> getNotificationOpenHandlerChain() {
		return pushNotificationOpenHandlerChain;
	}

	@NonNull
	private static Chain<PushNotificationOpenHandler> generateDefault() {

		return new NotificationOpenHandlerChain.Builder()
				.addMessagePreHandler(PushwooshPlatform.getInstance().pushStatNotificationOpenHandler())
				.addMessagePreHandler(new HtmlPagePushNotificationOpenHandler())
				.addMessagePreHandler(new RemotePagePushNotificationOpenHandler())
				.addMessagePreHandler(new RichMediaPushNotificationOpenHandler())
				.addMessagePreHandler(new LaunchActivityPushNotificationOpenHandler())
				.addMessagePreHandler(new UpdateStatusBarStorageOpenHandler())
				.build();
	}
}
