package com.pushwoosh.appevents;

import android.app.Application;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppManager;
import com.pushwoosh.internal.event.ConfigLoadedEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.ApplicationOpenDetector.ApplicationOpenEvent;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.repository.config.Event;
import com.pushwoosh.tags.TagsBundle;

import java.util.List;

public class PushwooshDefaultEvents {
    private static volatile Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;
    private static final Object ACTIVITY_LIFECYCLE_CALLBACKS_MUTEX = new Object();

    static final String APPLICATION_OPENED_EVENT = "PW_ApplicationOpen";
    static final String SCREEN_OPENED_EVENT = "PW_ScreenOpen";
    static final String APPLICATION_CLOSED_EVENT = "PW_ApplicationMinimized";

    private boolean isConfigLoaded = false;

    public void init() {
        EventBus.subscribe(ConfigLoadedEvent.class, configLoadedEventListener);
        registerActivityLifecycleCallbacks();
    }

    private EventListener<ApplicationOpenEvent> applicationOpenEventListener = (event) -> {
        EventBus.unsubscribe(ApplicationOpenEvent.class, PushwooshDefaultEvents.this.applicationOpenEventListener);
        registerActivityLifecycleCallbacks();
    };

    private EventListener<ConfigLoadedEvent> configLoadedEventListener = (event) -> {
        EventBus.unsubscribe(ConfigLoadedEvent.class, PushwooshDefaultEvents.this.configLoadedEventListener);
        isConfigLoaded = true;
    };

    private EventListener<ConfigLoadedEvent> readyToSendRequestsListener;

    void postEvent(String eventName, TagsBundle attributes) {
        if (isConfigLoaded) {
            postEventInternal(eventName, attributes);
        } else {
            readyToSendRequestsListener = event -> {
                EventBus.unsubscribe(ConfigLoadedEvent.class, this.readyToSendRequestsListener);
                postEventInternal(eventName, attributes);
            };
            EventBus.subscribe(ConfigLoadedEvent.class, readyToSendRequestsListener);
        }
    }

    private void postEventInternal(String eventName, TagsBundle attributes) {
        List<Event> events = PushwooshPlatform.getInstance().pushwooshRepository().getEvents();
        if (events != null) {
            for (Event e: events) {
                if (e.getName().equals(eventName)) {
                    InAppManager.getInstance().postEvent(eventName, attributes);
                    break;
                }
            }
        }
    }

    static TagsBundle buildAttributes(String eventName, String activityName) {
        TagsBundle.Builder attributes = new TagsBundle.Builder();
        attributes.putInt("device_type", DeviceSpecificProvider.getInstance().deviceType());
        attributes.putString("application_version", AndroidPlatformModule.getAppInfoProvider().getVersionName());

        if (eventName.equals(SCREEN_OPENED_EVENT)) {
            attributes.putString("screen_name", activityName);
        }

        return attributes.build();
    }

    private void registerActivityLifecycleCallbacks() {
        synchronized (ACTIVITY_LIFECYCLE_CALLBACKS_MUTEX) {
            if (activityLifecycleCallbacks == null) {
                Application app = (Application) AndroidPlatformModule.getApplicationContext();
                if (app == null) {
                    EventBus.subscribe(ApplicationOpenEvent.class, applicationOpenEventListener);
                } else {
                    activityLifecycleCallbacks = new com.pushwoosh.appevents.PushwooshAppLifecycleCallbacks((eventName, activityName) -> {
                        switch (eventName) {
                            case PushwooshAppLifecycleCallbacks.APPLICATION_OPENED_EVENT:
                                postEvent(APPLICATION_OPENED_EVENT, buildAttributes(APPLICATION_OPENED_EVENT, activityName));
                                break;
                            case PushwooshAppLifecycleCallbacks.APPLICATION_CLOSED_EVENT:
                                postEvent(APPLICATION_CLOSED_EVENT, buildAttributes(APPLICATION_CLOSED_EVENT, activityName));
                                break;
                            case PushwooshAppLifecycleCallbacks.SCREEN_OPENED_EVENT:
                                postEvent(SCREEN_OPENED_EVENT, buildAttributes(SCREEN_OPENED_EVENT, activityName));
                                break;
                        }
                    });
                    app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
                }
            }
        }
    }
}
