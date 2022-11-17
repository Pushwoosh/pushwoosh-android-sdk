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

package com.pushwoosh.location.scheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PendingIntentUtils;
import com.pushwoosh.location.network.GeoLocationServiceApi16;


class GeoLocationServiceSchedulerApi16 implements Scheduler {

	@Override
	public void scheduleNearestGeoZones(@Nullable final Context context, long updateInterval) {
		if (context == null) {
			return;
		}

		PendingIntent pendingIntent = getAlarmIntent(context);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (alarmManager != null) {
			try {
				alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + updateInterval, pendingIntent);
			} catch (SecurityException e) {
				PWLog.error("Too many alarms. Please clear all local alarm to continue use AlarmManager. Geo zones alarm skipped");
			}
		}
	}

	@Nullable
	private PendingIntent getAlarmIntent(@Nullable Context context) {
		if (context == null) {
			return null;
		}
		Intent intent = new Intent(context, GeoLocationServiceApi16.class);
		intent.putExtra(GeoLocationServiceApi16.KEY_FORCE_UPDATE, false);
		return PendingIntent.getService(context, 0, intent, PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void cancelAlarm(@Nullable Context context) {
		if (context == null) {
			return;
		}

		PendingIntent pendingIntent = getAlarmIntent(context);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (alarmManager != null) {
			alarmManager.cancel(pendingIntent);
		}
	}

	@Override
	public void requestUpdateNearestGeoZones(@Nullable Context context) {
		if (context != null) {
			context.startService(GeoLocationServiceApi16.createGetNearestIntent(context,true));
		}
	}

	@Override
	public void stop(@Nullable Context context) {
		if (context == null) {
			return;
		}

		context.stopService(new Intent(context, GeoLocationServiceSchedulerApi16.class));
		cancelAlarm(context);
	}

	@Override
	public void deviceRebooted(@Nullable Context context) {
		requestUpdateNearestGeoZones(context);
	}

	@Override
	public void requestLocationDisabled(@Nullable Context context) {
		if (context != null) {
			context.startService(GeoLocationServiceApi16.createLocationDisableIntent(context));
		}
	}
}
