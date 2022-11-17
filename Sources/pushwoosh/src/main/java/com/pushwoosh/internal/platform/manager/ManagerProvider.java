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

package com.pushwoosh.internal.platform.manager;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.view.WindowManager;

public interface ManagerProvider {
	@Nullable
	TelephonyManager getTelephonyManager();

	@Nullable
	AssetManager getAssets();

	@Nullable
	ConnectivityManager getConnectivityManager();

	@Nullable
	WindowManager getWindowManager();

	@Nullable
	NotificationManager getNotificationManager();

	@Nullable
	PackageManager getPackageManager();

	@Nullable
	PowerManager getPowerManager();

	@Nullable
	KeyguardManager getKeyguardManager();

	@Nullable
	ActivityManager getActivityManager();

	@Nullable
	AudioManager getAudioManager();

	@Nullable
	AlarmManager getAlarmManager();
}
