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

package com.pushwoosh.inbox;

import com.pushwoosh.inbox.internal.PushwooshInboxModule;
import com.pushwoosh.inbox.notification.handler.InboxNotificationHandler;
import com.pushwoosh.inbox.notification.handler.InboxNotificationOpenHandler;
import com.pushwoosh.inbox.storage.db.InboxDbHelper;
import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.event.AppIdChangedEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme;
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandleChainProvider;
import com.pushwoosh.notification.handlers.notification.NotificationOpenHandlerChainProvider;

import java.util.Collection;
import java.util.Collections;

public class PushwooshInboxPlugin implements Plugin {
	@Override
	public void init() {
		PushwooshInboxModule.init(new InboxDbHelper(AndroidPlatformModule.getApplicationContext()),
								  NetworkModule.getRequestManager(),
								  AndroidPlatformModule.getPrefsProvider());

		MessageSystemHandleChainProvider.getMessageSystemChain().addItem(new InboxNotificationHandler());
		NotificationOpenHandlerChainProvider.getNotificationOpenHandlerChain().addItem(new InboxNotificationOpenHandler());
		EventBus.subscribe(AppIdChangedEvent.class, event -> PushwooshInboxModule.getInboxRepository().clearAllInboxMessages());
	}

	@Override
	public Collection<? extends MigrationScheme> getPrefsMigrationSchemes(PrefsProvider prefsProvider) {
		return Collections.emptyList();
	}
}
