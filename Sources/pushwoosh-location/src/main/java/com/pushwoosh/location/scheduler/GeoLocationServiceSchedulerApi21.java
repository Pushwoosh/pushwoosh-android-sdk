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

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.network.GeoLocationServiceApi21;

import static com.pushwoosh.location.internal.LocationModule.jobLocationIdProvider;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class GeoLocationServiceSchedulerApi21 implements Scheduler {

	@Override
	public void scheduleNearestGeoZones(@Nullable final Context context, final long interval) {
		scheduleJob(context, provideJobInfo(context)
				.setMinimumLatency(interval)
				.setExtras(GeoLocationServiceApi21.createGetNearestExtras(false))
				.setPersisted(true)
				.build());
	}

	@Override
	public void requestUpdateNearestGeoZones(@Nullable final Context context) {
		scheduleJob(context, provideJobInfo(context)
				.setExtras(GeoLocationServiceApi21.createGetNearestExtras(true))
				.build());
	}

	@SuppressLint("WrongConstant")
	private void scheduleJob(@Nullable Context context, JobInfo jobInfo) {
		int schedule = -1;
		if (context != null) {
			JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
			if (jobScheduler != null) {
				schedule = jobScheduler.schedule(jobInfo);
			}
		}

		if (schedule <= 0) {
			PWLog.error(LocationConfig.TAG, "Can't run job scheduler");
		}
	}

	private JobInfo.Builder provideJobInfo(final Context context) {
		return new JobInfo.Builder(jobLocationIdProvider().getNearestServiceJobId(), new ComponentName(context, GeoLocationServiceApi21.class))
				.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
	}

	@Override
	public void stop(@Nullable final Context context) {
		if (context == null) {
			return;
		}

		JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
		if (jobScheduler != null) {
			jobScheduler.cancel(jobLocationIdProvider().getNearestServiceJobId());
		}
	}

	@Override
	public void deviceRebooted(final Context context) {
		//stub can't start service starting from android O
	}

	@Override
	public void requestLocationDisabled(@Nullable Context context) {
		scheduleJob(context, provideJobInfo(context)
				.setExtras(GeoLocationServiceApi21.createDisableLocationExtras())
				.build());
	}
}
