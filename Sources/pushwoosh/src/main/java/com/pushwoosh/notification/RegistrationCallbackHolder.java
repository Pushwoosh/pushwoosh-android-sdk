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

    private RegistrationCallbackHolder(
            Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        this.callback = callback;
    }

    public static void setCallback(
            Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        if (callback == null) {
            return;
        }
        new RegistrationCallbackHolder(callback).subscribe();
    }

    private void subscribe() {
        registrationSuccessSubscription = EventBus.subscribe(RegistrationSuccessEvent.class, (event) -> {
            unsubscribe();
            callback.process(Result.fromData(event.getData()));
        });

        registrationErrorSubscription = EventBus.subscribe(RegistrationErrorEvent.class, (event) -> {
            unsubscribe();
            callback.process(Result.fromException(new RegisterForPushNotificationsException(event.getData())));
        });
    }

    private void unsubscribe() {
        if (registrationSuccessSubscription != null) {
            registrationSuccessSubscription.unsubscribe();
        }
        if (registrationErrorSubscription != null) {
            registrationErrorSubscription.unsubscribe();
        }
    }
}
