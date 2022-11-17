package com.pushwoosh.notification;

import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.notification.event.RegistrationErrorEvent;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;

public final class RegistrationCallbackHolder {
	private final Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback;
	private Subscription<RegistrationSuccessEvent> registrationSuccessSubscription;
	private Subscription<RegistrationErrorEvent> registrationErrorSubscription;
	private static RegistrationCallbackHolder currentCallbackHolder;

	private RegistrationCallbackHolder(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
		this.callback = callback;
	}

	private void subscribe(boolean isRegisterForPushNotificationsCallback) {
		registrationSuccessSubscription = EventBus.subscribe(RegistrationSuccessEvent.class, (event) -> {
			unsubscribe(isRegisterForPushNotificationsCallback);
			callback.process(Result.fromData(event.getData()));
		});

		registrationErrorSubscription = EventBus.subscribe(RegistrationErrorEvent.class, (event) -> {
			unsubscribe(isRegisterForPushNotificationsCallback);
			callback.process(Result.fromException(new RegisterForPushNotificationsException(event.getData())));
		});
	}

	private void unsubscribe(boolean isRegisterForPushNotificationsCallback) {
		if (registrationSuccessSubscription != null) {
			registrationSuccessSubscription.unsubscribe();
		}

		if (registrationErrorSubscription != null) {
			registrationErrorSubscription.unsubscribe();
		}
		if (isRegisterForPushNotificationsCallback) {
			currentCallbackHolder = null;
		}
	}

	public static void setCallback(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback,
								   boolean isRegisterForPushNotificationsCallback) {
		if (callback == null) {
			return;
		}

		if (!isRegisterForPushNotificationsCallback) {
			RegistrationCallbackHolder holder = new RegistrationCallbackHolder(callback);
			holder.subscribe(false);
		} else {
			if (currentCallbackHolder == null) {
				currentCallbackHolder = new RegistrationCallbackHolder(callback);
				currentCallbackHolder.subscribe(true);
			}
		}
	}
}
