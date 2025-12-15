package com.pushwoosh.internal.utils;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.PushwooshWorkManagerHelper;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.richmedia.RichMediaController;

import java.util.List;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

public class LockScreenReceiver extends BroadcastReceiver {
	private static final String TAG = "LockScreenReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			handleIntent(context, intent);
		} catch (Exception e) {
			PWLog.error(TAG, "Failed to handle intent", e);
		}
	}

	private void handleIntent(Context context, Intent intent) {
		if (intent == null) {
			return;
		}

		if (TextUtils.equals(intent.getAction(), Intent.ACTION_SCREEN_ON)) {
			new ShowCachedResourceWrappersTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else if (TextUtils.equals(intent.getAction(), Intent.ACTION_USER_PRESENT)) {
			showCachedRemoteUrls(context);
		}
	}

	private void showCachedRemoteUrls(Context context) {
		new GetCachedRemoteUrlsTask(result -> {
			List<Uri> remoteUrls = result.getData();
			if (remoteUrls == null || remoteUrls.isEmpty()) {
				return;
			}
			for (Uri uri : remoteUrls) {
				showRemoteUrl(uri, context);
			}
			new ClearRemoteUrlsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
 	}

	private void showRemoteUrl(Uri url, Context context) {
		Intent intent = new Intent(Intent.ACTION_VIEW, url);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			context.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			PWLog.error("Can't open remote url: " + url, e);
		}
	}

	private static class ShowCachedResourceWrappersTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... voids) {
			List<ResourceWrapper> resourceWrapperList = RepositoryModule.getLockScreenMediaStorage().getCachedResourcesList();
			if (resourceWrapperList == null || resourceWrapperList.isEmpty()) {
				return null;
			}

			RichMediaController richMediaController = PushwooshPlatform.getInstance().getRichMediaController();
			if (richMediaController == null) {
				PWLog.error(TAG, "RichMediaController is null");
				return null;
			}

			for (ResourceWrapper resourceWrapper : resourceWrapperList) {
				richMediaController.showResourceWrapper(resourceWrapper);
			}
			RepositoryModule.getLockScreenMediaStorage().clearResources();
			return null;
		}
	}

	private static class GetCachedRemoteUrlsTask extends AsyncTask<Void, Void, List<Uri>> {
		private final Callback<List<Uri>, PushwooshException> callback;

		public GetCachedRemoteUrlsTask(Callback<List<Uri>, PushwooshException> callback) {
			this.callback = callback;
		}

		@Override
		protected List<Uri> doInBackground(Void... voids) {
			return RepositoryModule.getLockScreenMediaStorage().getCachedRemoteUrls();
		}

		@Override
		protected void onPostExecute(List<Uri> uris) {
			super.onPostExecute(uris);
			callback.process(Result.from(uris, null));
		}
	}

	private static class ClearRemoteUrlsTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... voids) {
			RepositoryModule.getLockScreenMediaStorage().clearRemoteUrls();
			return null;
		}
	}
}
