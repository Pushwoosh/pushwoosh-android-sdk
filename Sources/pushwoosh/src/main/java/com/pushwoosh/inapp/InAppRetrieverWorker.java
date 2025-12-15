package com.pushwoosh.inapp;

import android.content.Context;
import androidx.annotation.NonNull;

import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.internal.utils.PWLog;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class InAppRetrieverWorker extends Worker {
	public static final String TAG = "InAppRetrieverWorker";
	public InAppRetrieverWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	@NonNull
	@Override
	public Result doWork() {
		try {
			doLoadInApps();
		} catch (Exception e) {
			PWLog.error(TAG, "Failed to load in-apps", e);
		}
		return Result.success();
	}

	private static void doLoadInApps() {
		InAppRepository inAppRepository = InAppModule.getInAppRepository();
		if(inAppRepository == null){
			return;
		}
		inAppRepository.loadInApps();
	}
}
