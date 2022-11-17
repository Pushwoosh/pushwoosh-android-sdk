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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.pushwoosh.badge.thirdparty.shortcutbadger.ShortcutBadger;
import com.pushwoosh.internal.platform.AndroidPlatformModule;

/**
 * PushwooshBadge is a static class responsible for application icon badge number managing. <br>
 * By default pushwoosh-badge library automatically adds following permissions:<br>
 * com.sec.android.provider.badge.permission.READ<br>
 * com.sec.android.provider.badge.permission.WRITE<br>
 <!--for htc-->
 * com.htc.launcher.permission.READ_SETTINGS<br>
 * com.htc.launcher.permission.UPDATE_SHORTCUT<br>
 <!--for sony-->
 * com.sonyericsson.home.permission.BROADCAST_BADGE<br>
 * com.sonymobile.home.permission.PROVIDER_INSERT_BADGE<br>
 <!--for apex-->
 * com.anddoes.launcher.permission.UPDATE_COUNT<br>
 <!--for solid-->
 * com.majeur.launcher.permission.UPDATE_BADGE<br>
 <!--for huawei-->
 * com.huawei.android.launcher.permission.CHANGE_BADGE<br>
 * com.huawei.android.launcher.permission.READ_SETTINGS<br>
 * com.huawei.android.launcher.permission.WRITE_SETTINGS<br>
 <!--for ZUK-->
 * android.permission.READ_APP_BADGE<br>
 <!--for OPPO-->
 * com.oppo.launcher.permission.READ_SETTINGS<br>
 * com.oppo.launcher.permission.WRITE_SETTINGS<br>
 <!--for EvMe-->
 * me.everything.badger.permission.BADGE_COUNT_READ<br>
 * me.everything.badger.permission.BADGE_COUNT_WRITE<br>
 *
 */
public class PushwooshBadge {

	/**
	 * Set application icon badge number and synchronize this value with pushwoosh backend.
	 * 0 value can be used to clear badges
	 *
	 * @param newBadge icon badge number
	 */
	public static void setBadgeNumber(int newBadge) {
		// must be executed on main thread
		Handler mainHandler = new Handler(Looper.getMainLooper());
		mainHandler.post(() -> {
			Context context = AndroidPlatformModule.getApplicationContext();
			if (context == null) {
				return;
			}
			ShortcutBadger.applyCount(context, newBadge);
			BadgeModule.getBadgePrefs().badgeCount().set(newBadge);
		});
	}

	/**
	 * @return current application icon badge number
	 */
	public static int getBadgeNumber() {
		return BadgeModule.getBadgePrefs().badgeCount().get();
	}

	/**
	 * Increment current icon badge number
	 *
	 * @param deltaBadge application icon badge number addition
	 */
	public static void addBadgeNumber(int deltaBadge) {
		int oldBadges = getBadgeNumber();
		int newBadge = oldBadges + deltaBadge;

		if (newBadge < 0) {
			newBadge = 0;
		}

		setBadgeNumber(newBadge);
	}
}
