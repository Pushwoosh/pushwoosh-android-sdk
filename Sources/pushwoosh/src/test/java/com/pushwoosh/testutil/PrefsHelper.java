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

package com.pushwoosh.testutil;

import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.notification.VibrateType;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.RepositoryTestManager;

import java.util.Set;

/**
 * Created by etkachenko on 3/29/17.
 */

public class PrefsHelper {
	public static void tearDownPrefs() {
		tearDownNotificationPrefs();
		tearDownRegistrationPrefs();
		tearDowmLocalNotificationStorage();
		EventBus.clearSubscribersMap();
	}

	private static void tearDownNotificationPrefs() {
		NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
		if (notificationPrefs == null) {
			return;
		}

		notificationPrefs.multiMode().set(false);
		notificationPrefs.messageId().set(0);
		notificationPrefs.lightScreenOn().set(false);
		notificationPrefs.ledEnabled().set(false);
		notificationPrefs.ledColor().set(0);
		notificationPrefs.notificationFactoryClass().set(null);
		notificationPrefs.iconBackgroundColor().set(0);
		notificationPrefs.richMediaDelayMs().set(0);
		notificationPrefs.lastNotificationHash().set(null);
		notificationPrefs.notificationEnabled().set(false);
		notificationPrefs.soundType().set(SoundType.DEFAULT_MODE);
		notificationPrefs.vibrateType().set(VibrateType.DEFAULT_MODE);
		notificationPrefs.pushHistory().clear();
		notificationPrefs.tags().set(null);
		RepositoryModule.setNotificationPreferences(null);
	}

	private static void tearDownRegistrationPrefs() {
		RegistrationPrefs registrationPrefs = RepositoryModule.getRegistrationPreferences();
		if (registrationPrefs == null) {
			return;
		}

		RepositoryTestManager.destroyRegistrationPrefs(registrationPrefs);
		RepositoryModule.setRegistrationPreferences(null);
	}

	private static void tearDowmLocalNotificationStorage() {
		LocalNotificationStorage localNotificationStorage = RepositoryModule.getLocalNotificationStorage();
		Set<Integer> ids = localNotificationStorage.getRequestIds();
		for (Integer id : ids) {
			localNotificationStorage.removeLocalNotification(id);
		}
		localNotificationStorage.getRequestIds().clear();
	}
}
