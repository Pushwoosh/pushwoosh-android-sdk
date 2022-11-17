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

package com.pushwoosh.inbox.internal;

import android.content.SharedPreferences;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.inbox.repository.InboxRepository;
import com.pushwoosh.inbox.storage.InboxStorage;
import com.pushwoosh.inbox.storage.db.DbInboxStorage;
import com.pushwoosh.inbox.storage.db.InboxDbHelper;
import com.pushwoosh.internal.command.CommandApplayer;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;

public class PushwooshInboxModule {
	private static final String TAG = "pwInbox";
	private static final String APP_ID = "appId";

	private static volatile InboxRepository sInboxRepository;
	private static final Object sRepositoryMutex = new Object();

	private static volatile InboxStorage sInboxStorage;
	private static final Object sStorageMutex = new Object();
	private static InboxDbHelper sInboxDbHelper;
	private static RequestManager sRequestManager;
	private static CommandApplayer sCommandApplayer;

	public static InboxRepository getInboxRepository() {
		if (sInboxRepository == null) {
			synchronized (sRepositoryMutex) {
				if (sInboxRepository == null) {
					if (sRequestManager == null) {
						throw new IllegalArgumentException("Incorrect state.");
					}
					if(sCommandApplayer == null){
						throw new IllegalArgumentException("Incorrect state.");
					}

					sInboxRepository = new InboxRepository(sRequestManager, getInboxStorage(), sCommandApplayer);
					sRequestManager = null;
				}
			}
		}

		return sInboxRepository;
	}

	private static InboxStorage getInboxStorage() {
		if (sInboxStorage == null) {
			synchronized (sStorageMutex) {
				if (sInboxStorage == null) {
					if (sInboxDbHelper == null) {
						throw new IllegalArgumentException("Incorrect state.");
					}
					sInboxStorage = new DbInboxStorage(sInboxDbHelper);
					sInboxDbHelper = null;
				}
			}
		}

		return sInboxStorage;
	}

	public static void init(InboxDbHelper inboxDbHelper, RequestManager requestManager, PrefsProvider prefsProvider) {
		sInboxDbHelper = inboxDbHelper;
		sRequestManager = requestManager;
		sCommandApplayer = new CommandApplayer();

		SharedPreferences sharedPreferences = prefsProvider.providePrefs(TAG);

		String currentAppId = Pushwoosh.getInstance().getApplicationCode();
		String prevAppId = sharedPreferences == null ? currentAppId : sharedPreferences.getString(APP_ID, currentAppId);

		if (sharedPreferences != null) {
			sharedPreferences.edit()
					.putString(APP_ID, currentAppId)
					.apply();
		}

		if (!prevAppId.equals(currentAppId)) {
			sInboxDbHelper.dropDb();
		}
	}
}
