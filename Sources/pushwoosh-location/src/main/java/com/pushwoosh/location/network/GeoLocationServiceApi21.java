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

package com.pushwoosh.location.network;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PersistableBundle;
import androidx.annotation.RequiresApi;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.internal.LocationModule;
import com.pushwoosh.location.internal.utils.LocationConfig;

import java.lang.ref.WeakReference;

/**
 * Service for updating by schedule nearest zones. If forceUpdate is true than it request nearest zones
 * from service otherwise it get nearest zones from storage
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class GeoLocationServiceApi21 extends JobService {
	private static final String SUB_TAG = "[GeoLocationServiceApi]";
	public static final String KEY_FORCE_UPDATE = SUB_TAG + "KEY_FORCE_UPDATE";
	public static final String KEY_TYPE = SUB_TAG + ".key_TYPE";

	private static final int GET_NEAREST_TYPE = 0;
	private static final int DISABLE_LOCATION_TYPE = 1;

	private AsyncTask<Void, Void, Void> jobTask;

	public static PersistableBundle createGetNearestExtras(boolean forceUpdate) {
		PersistableBundle persistableBundle = new PersistableBundle();
		persistableBundle.putInt(KEY_FORCE_UPDATE, forceUpdate ? 1 : 0);
		persistableBundle.putInt(KEY_TYPE, GET_NEAREST_TYPE);
		return persistableBundle;
	}

	public static PersistableBundle createDisableLocationExtras() {
		PersistableBundle persistableBundle = new PersistableBundle();
		persistableBundle.putInt(KEY_TYPE, DISABLE_LOCATION_TYPE);
		return persistableBundle;
	}

	private GetNearestZoneJobApplier jobApplayer;

	@Override
	public void onCreate() {
		super.onCreate();
		AndroidPlatformModule.init(getApplicationContext());
		jobApplayer = new GetNearestZoneJobApplier(LocationModule.nearestZonesManager(),
				LocationModule.updateNearestRepository(),
				LocationModule.locationTracker());
	}

	@Override
	public boolean onStartJob(final JobParameters jobParameters) {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + "onStartJob");
		boolean forceUpdate = jobParameters.getExtras().getInt(KEY_FORCE_UPDATE, 0) == 1;
		final int type = jobParameters.getExtras().getInt(KEY_TYPE);
		jobTask = new JobExecutor(type, jobApplayer, () -> jobFinished(jobParameters, false), forceUpdate);
		jobTask.execute();
		return true;
	}

	@Override
	public boolean onStopJob(final JobParameters jobParameters) {
		jobApplayer.cancel();

		if (jobTask != null) {
			jobTask.cancel(true);
		}
		return true;
	}

	@Override
	public void onDestroy() {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + " destroy nearest geoZones service");
		super.onDestroy();
	}


	private static class JobExecutor extends AsyncTask<Void, Void, Void> {
		private final int type;
		private final WeakReference<GetNearestZoneJobApplier> jobApplayer;
		private final WeakReference<JobFinished> jobFinished;
		private final boolean forceUpdate;

		JobExecutor(int type, GetNearestZoneJobApplier jobApplayer, JobFinished jobFinished, boolean forceUpdate) {
			this.type = type;
			this.jobApplayer = new WeakReference<>(jobApplayer);
			this.jobFinished = new WeakReference<>(jobFinished);
			this.forceUpdate = forceUpdate;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			GetNearestZoneJobApplier applier = jobApplayer.get();
			if (applier == null) {
				return null;
			}
			switch (type) {
				case GET_NEAREST_TYPE:
					applier.loadNearestGeoZones(forceUpdate);
					break;
				case DISABLE_LOCATION_TYPE:
					applier.locationDisable();
					break;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			JobFinished jobFinished = this.jobFinished.get();
			if (jobFinished != null) {
				jobFinished.taskFinish();
			}
		}
	}

	private interface JobFinished {
		void taskFinish();
	}
}
