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

package com.pushwoosh.repository;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.network.RequestStorage;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.prefs.PrefsFactory;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme;
import com.pushwoosh.internal.platform.prefs.migration.PrefsMigration;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.UUIDFactory;
import com.pushwoosh.repository.util.StatusBarNotificationHelper;

import java.util.ArrayList;
import java.util.Collection;

public class RepositoryModule {
	private static NotificationPrefs notificationPreferences;
	private static RegistrationPrefs registrationPreferences;
	private static LocalNotificationStorage localNotificationStorage;
	private static RequestStorage requestStorage;
	private static LockScreenMediaStorage lockScreenMediaStorage;
	private static PushBundleStorage pushBundleStorage;
	private static InboxNotificationStorage inboxNotificationStorage;
	private static SilentRichMediaStorage silentRichMediaStorage;
	private static StatusBarNotificationStorage statusBarNotificationStorage;
	private static SummaryNotificationStorage summaryNotificationStorage;

	public static void init(Config config, UUIDFactory uuidFactory, DeviceRegistrar deviceRegistrar) {

		migratePrefsIfNeeded(config);

		if (notificationPreferences == null) {
			notificationPreferences = new NotificationPrefs(config);
		}

		if (registrationPreferences == null) {
			registrationPreferences = new RegistrationPrefs(config, deviceRegistrar);
		}

		if (localNotificationStorage == null) {
			createLocalNotificationStorage();
		}

		if (requestStorage == null) {
			Context context = AndroidPlatformModule.getApplicationContext();
			requestStorage = new RequestStorage(context, uuidFactory);
		}

		if (lockScreenMediaStorage == null) {
			Context context = AndroidPlatformModule.getApplicationContext();
			lockScreenMediaStorage = new LockScreenMediaStorageImpl(context);
		}

		if (pushBundleStorage == null) {
			Context context = AndroidPlatformModule.getApplicationContext();
			pushBundleStorage = new PushBundleStorageImpl(context);
		}

		if (inboxNotificationStorage == null) {
			Context context = AndroidPlatformModule.getApplicationContext();
			inboxNotificationStorage = new InboxNotificationStorageImpl(context);
		}

		if (silentRichMediaStorage == null) {
			Context context = AndroidPlatformModule.getApplicationContext();
			silentRichMediaStorage = new SilentRichMediaStorageImpl(context);
		}

		if (statusBarNotificationStorage == null) {
			Context context = AndroidPlatformModule.getApplicationContext();
			statusBarNotificationStorage = new StatusBarNotificationStorageImpl(context);
			new UpdateStatusBarNotificationStorageTask(statusBarNotificationStorage).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
		}

		if (summaryNotificationStorage == null) {
			Context context = AndroidPlatformModule.getApplicationContext();
			summaryNotificationStorage = new SummaryNotificationStorageImpl(context);
			new UpdateSummaryNotificationStorageTask(summaryNotificationStorage).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
		}
	}

	private static void createLocalNotificationStorage() {
		Context context = AndroidPlatformModule.getApplicationContext();
		if (context == null) {
			PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
			return;
		}

		DbLocalNotificationHelper dbLocalNotificationHelper = new DbLocalNotificationHelper(context);
		localNotificationStorage = new LocalNotificationStorage(dbLocalNotificationHelper);
	}

	private static void migratePrefsIfNeeded(Config config) {
		PWLog.noise("Migrate prefs if needed");
		final PrefsMigration prefsMigration = AndroidPlatformModule.getPrefsMigration();

		if (prefsMigration == null) {
			return;
		}

		final PrefsProvider prevPrefsProvider = PrefsFactory.getPrevPrefsProvider();

		if (prevPrefsProvider == null) {
			return;
		}

		PWLog.noise("Start migration with prevPrefsProvider: " + prevPrefsProvider.getClass().getName());

		Collection<MigrationScheme> migrationSchemes = new ArrayList<>();
		migrationSchemes.add(RegistrationPrefs.provideMigrationScheme(prevPrefsProvider));
		migrationSchemes.add(NotificationPrefs.provideMigrationScheme(prevPrefsProvider));

		for (Plugin plugin : config.getPlugins()) {
			final Collection<? extends MigrationScheme> prefsMigrationSchemes = plugin.getPrefsMigrationSchemes(prevPrefsProvider);
			if (prefsMigrationSchemes != null) {
				migrationSchemes.addAll(prefsMigrationSchemes);
			}
		}

		prefsMigration.migrate(migrationSchemes);
	}

	public static NotificationPrefs getNotificationPreferences() {
		return notificationPreferences;
	}

	public static void setNotificationPreferences(NotificationPrefs prefs) {
		notificationPreferences = prefs;
	}

	public static RegistrationPrefs getRegistrationPreferences() {
		return registrationPreferences;
	}

	public static void setRegistrationPreferences(RegistrationPrefs prefs) {
		registrationPreferences = prefs;
	}

	public static LocalNotificationStorage getLocalNotificationStorage() {
		return localNotificationStorage;
	}

	public static void setLocalNotificationStorage(LocalNotificationStorage storage) {
		localNotificationStorage = storage;
	}

	public static RequestStorage getRequestStorage() {
		return requestStorage;
	}

	public static void setRequestStorage(RequestStorage storage) {
		requestStorage = storage;
	}

	public static LockScreenMediaStorage getLockScreenMediaStorage() {
		return lockScreenMediaStorage;
	}

	public static PushBundleStorage getPushBundleStorage() {
		return pushBundleStorage;
	}

	public static InboxNotificationStorage getInboxNotificationStorage() {
		return inboxNotificationStorage;
	}

	public static SilentRichMediaStorage getSilentRichMediaStorage() {
		return silentRichMediaStorage;
	}

	public static StatusBarNotificationStorage getStatusBarNotificationStorage() { return statusBarNotificationStorage; }

	public static SummaryNotificationStorage getSummaryNotificationStorage() { return summaryNotificationStorage; }

	private static class UpdateStatusBarNotificationStorageTask extends AsyncTask<Void, Void, Void>{
		StatusBarNotificationStorage statusBarNotificationStorage;

		public UpdateStatusBarNotificationStorageTask(StatusBarNotificationStorage statusBarNotificationStorage) {
			this.statusBarNotificationStorage = statusBarNotificationStorage;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			if (Build.VERSION.SDK_INT >= 23) {
				statusBarNotificationStorage.update(StatusBarNotificationHelper.getActiveNotificationsIds());
			}
			return null;
		}
	}

	private static class UpdateSummaryNotificationStorageTask extends AsyncTask<Void, Void, Void>{
		SummaryNotificationStorage summaryNotificationStorage;

		public UpdateSummaryNotificationStorageTask(SummaryNotificationStorage summaryNotificationStorage) {
			this.summaryNotificationStorage = summaryNotificationStorage;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			if (Build.VERSION.SDK_INT >= 23) {
				summaryNotificationStorage.update(StatusBarNotificationHelper.getSummaryNotificationsIds());
			}
			return null;
		}
	}
}
