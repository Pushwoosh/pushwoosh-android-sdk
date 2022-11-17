//
//  PushEventsTransmitter.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.pushwoosh.internal.event;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EventBus {

    private static final Map<Class<? extends Event>, List<EventListener<?>>> SUBSCRIBERS_MAP = new ConcurrentHashMap<>();

    public static <T extends Event> boolean sendEvent(@NonNull T event) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        return sendEventInternal(event, mainHandler);
    }

    private static <T extends Event> boolean sendEventInternal(@NonNull T event, Handler handler) {
        Class<? extends Event> eventClass = event.getClass();

        if (!SUBSCRIBERS_MAP.containsKey(eventClass)) {
            return false;
        }

        List<EventListener<?>> subscribers = SUBSCRIBERS_MAP.get(eventClass);

        if (subscribers == null) {
            return false;
        }

		handler.post(() -> notifyEventListeners(event, subscribers));

        return true;
    }

	@MainThread
	private static <T extends Event> void notifyEventListeners(final @NonNull T event, final List<EventListener<?>> eventListeners) {
		List<EventListener<?>> copy;
		synchronized (eventListeners) {
			copy = new ArrayList<>(eventListeners);
		}

		for (EventListener<?> eventAction : copy) {
			//noinspection unchecked
			EventListener<T> typedEventAction = (EventListener<T>) eventAction;
			typedEventAction.onReceive(event);
        }
    }

    public static <T extends Event> Subscription<T> subscribe(Class<T> event, EventListener<T> listener) {
        if (listener == null) {
            return null;
        }
        List<EventListener<?>> subscribers = SUBSCRIBERS_MAP.get(event);
        if (subscribers == null) {
            subscribers = new LinkedList<>();
            SUBSCRIBERS_MAP.put(event, subscribers);
        }

		synchronized (subscribers) {
			subscribers.add(listener);
		}

        return new Subscription<>(event, listener);
    }

    public static <T extends Event> void unsubscribe(Class<T> eventClass, EventListener<T> eventAction) {
        if (!SUBSCRIBERS_MAP.containsKey(eventClass)) {
            return;
        }

		List<EventListener<?>> eventActions = SUBSCRIBERS_MAP.get(eventClass);
		if (eventActions == null) {
			return;
		}
		synchronized (eventActions) {
			eventActions.remove(eventAction);
		}
	}


    private EventBus() {/*do nothing*/}

}
