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

package com.pushwoosh.badge;

import java.util.Collection;
import java.util.Collections;

import com.pushwoosh.badge.notification.BadgeUpdaterMessageHandler;
import com.pushwoosh.badge.prefs.BadgePrefs;
import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme;
import com.pushwoosh.notification.handlers.message.user.MessageHandleChainProvider;

public class BadgePlugin implements Plugin {
	@Override
	public void init() {
		MessageHandleChainProvider.getHandleProcessor().addItem(new BadgeUpdaterMessageHandler());
		BadgeModule.init();
	}

	@Override
	public Collection<? extends MigrationScheme> getPrefsMigrationSchemes(PrefsProvider prefsProvider) {
		return Collections.singleton(BadgePrefs.provideMigrationScheme(prefsProvider));
	}
}
