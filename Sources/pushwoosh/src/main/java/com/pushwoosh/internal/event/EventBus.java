//
//  PushEventsTransmitter.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.pushwoosh.internal.event;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventBus {
    private static final String TAG = "EventBus";

    private static final ConcurrentHashMap<Class<? extends Event>, List<EventListener<?>>> SUBSCRIBERS_MAP =
            new ConcurrentHashMap<>();

    public static <T extends Event> boolean sendEvent(@NonNull T event) {
        List<EventListener<?>> subscribers = SUBSCRIBERS_MAP.get(event.getClass());
        if (subscribers == null || subscribers.isEmpty()) {
            return false;
        }

        BackgroundExecutor.main(() -> notifyEventListeners(event, subscribers));
        return true;
    }

    @MainThread
    private static <T extends Event> void notifyEventListeners(
            final @NonNull T event, final List<EventListener<?>> subscribers) {
        for (EventListener<?> eventAction : subscribers) {
            try {
                //noinspection unchecked
                EventListener<T> typedEventAction = (EventListener<T>) eventAction;
                typedEventAction.onReceive(event);
            } catch (Throwable t) {
                PWLog.error(TAG, "Listener for " + event.getClass().getSimpleName() + " threw", t);
            }
        }
    }

    @NonNull public static <T extends Event> Subscription<T> subscribe(
            @NonNull Class<T> event, @NonNull EventListener<T> listener) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        List<EventListener<?>> subscribers = SUBSCRIBERS_MAP.get(event);
        if (subscribers == null) {
            List<EventListener<?>> created = new CopyOnWriteArrayList<>();
            List<EventListener<?>> existing = SUBSCRIBERS_MAP.putIfAbsent(event, created);
            subscribers = existing != null ? existing : created;
        }
        subscribers.add(listener);
        return new Subscription<>(event, listener);
    }

    public static <T extends Event> void unsubscribe(
            @NonNull Class<T> eventClass, @NonNull EventListener<T> eventAction) {
        List<EventListener<?>> eventActions = SUBSCRIBERS_MAP.get(eventClass);
        if (eventActions == null) {
            return;
        }
        eventActions.remove(eventAction);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static Map<Class<? extends Event>, List<EventListener<?>>> getSubscribersMap() {
        return SUBSCRIBERS_MAP;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void clearSubscribersMap() {
        SUBSCRIBERS_MAP.clear();
    }

    private EventBus() {
        /*do nothing*/
    }
}
