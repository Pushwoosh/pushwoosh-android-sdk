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

package com.pushwoosh.inapp;

import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.exception.MergeUserException;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.exception.ReloadInAppsException;
import com.pushwoosh.exception.RichMediaActionException;
import com.pushwoosh.exception.SetUserIdException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.event.ServerCommunicationStartedEvent;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.richmedia.RichMediaController;
import com.pushwoosh.tags.TagsBundle;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class PushwooshInAppImpl {
	private static final String TAG = "[InApp]PushwooshInApp";

	private final RegistrationPrefs registrationPrefs;

	private final Map<String, Object> javascriptInterfaces = new HashMap<>();
	private final Map<String, String> registeredJavascriptInterfaces = new HashMap<>();
	private final InAppRepository inAppRepository;
	private final ServerCommunicationManager serverCommunicationManager;
	private PushwooshInAppService pushwooshInAppService;
	private EventListener<ServerCommunicationStartedEvent> checkForUpdatesWhenServerCommunicationStartsEvent;

	public PushwooshInAppImpl(PushwooshInAppService pushwooshInAppService,
							  ServerCommunicationManager serverCommunicationManager) {
		registrationPrefs = RepositoryModule.getRegistrationPreferences();
		inAppRepository = InAppModule.getInAppRepository();
		this.pushwooshInAppService = pushwooshInAppService;
		this.serverCommunicationManager = serverCommunicationManager;

	}

	public void checkForUpdates() {
		if (serverCommunicationManager != null && !serverCommunicationManager.isServerCommunicationAllowed()) {
			subscribeCheckForUpdatesWhenServerCommunicationStartsEvent();
			return;
		}
		pushwooshInAppService.startService();
	}

	private void subscribeCheckForUpdatesWhenServerCommunicationStartsEvent() {
		if (checkForUpdatesWhenServerCommunicationStartsEvent != null) {
			return;
		}
		checkForUpdatesWhenServerCommunicationStartsEvent = new EventListener<ServerCommunicationStartedEvent>() {
			@Override
			public void onReceive(ServerCommunicationStartedEvent event) {
				EventBus.unsubscribe(ServerCommunicationStartedEvent.class, this);
				checkForUpdates();
			}
		};
		EventBus.subscribe(ServerCommunicationStartedEvent.class, checkForUpdatesWhenServerCommunicationStartsEvent);
	}

	public void postEvent(@NonNull String event, @Nullable TagsBundle attributes, @Nullable final Callback<Void, PostEventException> callback, boolean isInternal) {
		SdkStateProvider.getInstance().executeOrQueue(() -> {
			inAppRepository.postEvent(event, attributes, result -> {
				if (result.isSuccess()) {
					Resource resource = result.getData();

					if (!isInternal) {
						PWLog.info("Posted event " + event);
						if (attributes != null) {
							PWLog.info("Event attributes: "+ attributes.toJson());
						}
					}

					if (callback != null) {
						callback.process(Result.fromData(null));
					}
					if (resource == null) {
						return;
					}
					if (registrationPrefs.communicationEnable().get()) {
							showResource(resource);
					} else {
						PWLog.error(TAG, "can't show inApp because all communication disable");
					}
				} else {
					if (callback != null) {
						if (!isInternal) {
							PWLog.info("Failed to post event " + event);
						}
						callback.process(Result.fromException(result.getException()));
					}

					PWLog.warn(TAG, result.getException() == null ? "" : result.getException().getMessage(), result.getException());
				}
			});
		});
	}

	public void setUserId(@NonNull String userId) {
		String oldUserId = registrationPrefs.userId().get();

		if (TextUtils.equals(userId, oldUserId)) {
			return;
		}

		registrationPrefs.userId().set(userId);
		Callback<Boolean, SetUserIdException> callback = result -> {
			if (result.getException() != null) {
				registrationPrefs.userId().set(oldUserId);
			}
		};
		inAppRepository.setUserId(userId, callback);
	}

	public void mergeUserId(@NonNull String oldUserId, @NonNull String newUserId, boolean doMerge, @Nullable final Callback<Void, MergeUserException> callback) {
		inAppRepository.mergeUserId(oldUserId, newUserId, doMerge, callback);
	}

	public void addJavascriptInterface(@NonNull Object object, @NonNull String name) {
		javascriptInterfaces.put(name, object);
	}

	public void removeJavascriptInterface(@NonNull String name) {
		javascriptInterfaces.remove(name);
	}

	public void registerJavascriptInterface(@NonNull String className, @NonNull String name) {
		registeredJavascriptInterfaces.put(name, className);
	}

	public void reloadInApps(Callback<Boolean, ReloadInAppsException> callback) {
		if (inAppRepository == null) {
			return;
		}

		ReloadInAppsTask reloadInAppsTask = new ReloadInAppsTask(callback);
		reloadInAppsTask.execute();
	}

	public Map<String, Object> getJavascriptInterfaces() {
		Map<String, Object> result = new HashMap<>();
		result.putAll(javascriptInterfaces);

		for (Map.Entry<String, String> entry : registeredJavascriptInterfaces.entrySet()) {
			String name = entry.getKey();
			String className = entry.getValue();

			try {
				Class<?> jsInterfaceClassName = Class.forName(className);

				Object jsInterface = jsInterfaceClassName.newInstance();
				if (jsInterface != null) {
					result.put(name, jsInterface);
				}
			} catch (Exception e) {
				PWLog.warn(TAG, "Failed to instantiate javascript interface for " + name, e);
			}
		}

		return result;
	}

	public void showGDPRConsentInApp() {
		new ShowGDPRConsentInApp(this, () -> {
			Resource resource = inAppRepository.getGDPRConsentInAppResource();
			showResource(resource);
		}).execute();
	}

	private void showResource(Resource resource) {
		if (resource == null) {
			PWLog.error(TAG, "resource is null, can not finds resource");
			return;
		}
		ResourceWrapper resourceWrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.build();
		RichMediaController richMediaController = PushwooshPlatform.getInstance().getRichMediaController();
		if (richMediaController != null)
			richMediaController.showResourceWrapper(resourceWrapper);

	}

	public void sendRichMediaAction(String richmediaCode, String inappCode, String messageHash, String actionAttributes, int actionType, Callback<Void, RichMediaActionException> callback) {
		inAppRepository.richMediaAction(richmediaCode, inappCode, messageHash, actionAttributes, actionType, callback);
	}

	public void showGDPRDeletionInApp() {
		new ShowGDPRDeletionInAppTask(this, () -> {
			Resource resource = inAppRepository.getGDPRDeletionInApp();
			showResource(resource);
		}).execute();
	}

	private static class ShowGDPRConsentInApp extends ShowResourceTask {
		public ShowGDPRConsentInApp(PushwooshInAppImpl pushwooshInAppWeakRef, OnShowResourceFailureCallback callback) {
			super(pushwooshInAppWeakRef, callback);
		}

		@Override
		protected Resource doInBackground(Void... voids) {
			if (pushwooshInAppWeakRef.get() != null) {
				return pushwooshInAppWeakRef.get().inAppRepository.getGDPRConsentInAppResource();
			}
			return null;
		}
	}

	private static class ShowGDPRDeletionInAppTask extends ShowResourceTask {
		public ShowGDPRDeletionInAppTask(PushwooshInAppImpl pushwooshInAppWeakRef, OnShowResourceFailureCallback callback) {
			super(pushwooshInAppWeakRef, callback);
		}

		@Override
		protected Resource doInBackground(Void... voids) {
			if (pushwooshInAppWeakRef.get() != null) {
				return pushwooshInAppWeakRef.get().inAppRepository.getGDPRDeletionInApp();
			}
			return null;
		}
	}

	private static abstract class ShowResourceTask extends AsyncTask<Void, Void, Resource> {
		protected final WeakReference<PushwooshInAppImpl> pushwooshInAppWeakRef;
		private final OnShowResourceFailureCallback callback;

		public ShowResourceTask(PushwooshInAppImpl pushwooshInAppWeakRef, OnShowResourceFailureCallback callback) {
			this.pushwooshInAppWeakRef = new WeakReference<>(pushwooshInAppWeakRef);
			this.callback = callback;
		}

		@Override
		protected void onPostExecute(Resource resource) {
			super.onPostExecute(resource);
			if (resource != null && pushwooshInAppWeakRef.get() != null) {
				pushwooshInAppWeakRef.get().showResource(resource);
			} else {
				callback.onFail();
			}
		}

	}

	private interface OnShowResourceFailureCallback {
		void onFail();

	}

	private static class ReloadInAppsTask extends AsyncTask<Void, Void, Result<Void, NetworkException>> {
		private final Callback<Boolean, ReloadInAppsException> callback;

		public ReloadInAppsTask(Callback<Boolean, ReloadInAppsException> callback) {
			this.callback = callback;
		}

		private Result<Void, NetworkException> reloadInApps() throws NullPointerException {
			InAppRepository inAppRepository = InAppModule.getInAppRepository();
			if (inAppRepository == null) {
				throw new NullPointerException();
			}
			return inAppRepository.loadInApps();
		}

		@Override
		protected Result<Void, NetworkException> doInBackground(Void... voids) {
			return reloadInApps();
		}

		@Override
		protected void onPostExecute(Result<Void, NetworkException> result) {
			if (callback != null) {
				if (result.isSuccess()) {
						callback.process(Result.fromData(true));
				} else {
					String error = result.getException() != null ? result.getException().getMessage()
							: "Unknown error occurred while reloading inapps";
					callback.process(Result.fromException(new ReloadInAppsException(error)));
				}
			}
		}
	}

}
