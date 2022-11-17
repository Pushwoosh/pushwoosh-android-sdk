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
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

public class ContextManagerProvider implements ManagerProvider {

	private final WeakReference<Context> context;

	public ContextManagerProvider(@Nullable final Context context) {
		this.context = new WeakReference<>(context);
	}

	@Nullable
	private Context getContext() {
		return context.get();
	}

	@Override
	public TelephonyManager getTelephonyManager() {
		return getContext() == null ? null : (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
	}

	@Override
	public AssetManager getAssets() {
		return getContext() == null ? null : getContext().getAssets();
	}

	@Override
	public ConnectivityManager getConnectivityManager() {
		return getContext() == null ? null : (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	@Override
	public WindowManager getWindowManager() {
		return getContext() == null ? null : (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
	}

	@Override
	public NotificationManager getNotificationManager() {
		return getContext() == null ? null : (android.app.NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public PackageManager getPackageManager() {
		return getContext() == null ? null : getContext().getPackageManager();
	}

	@Override
	public PowerManager getPowerManager() {
		return getContext() == null ? null : (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
	}

	@Override
	public KeyguardManager getKeyguardManager() {
		return getContext() == null ? null : (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
	}

	@Override
	public ActivityManager getActivityManager() {
		return getContext() == null ? null : (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
	}

	@Override
	public AudioManager getAudioManager() {
		return getContext() == null ? null : (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
	}

	@Override
	public AlarmManager getAlarmManager() {
		return getContext() == null ? null : (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
	}
}
