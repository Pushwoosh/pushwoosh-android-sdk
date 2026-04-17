package com.pushwoosh.appevents;

import android.app.Application;

import androidx.annotation.Nullable;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppManager;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.ApplicationOpenDetector.ApplicationOpenEvent;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.tags.TagsBundle;

/**
 * Handles default application events with additional metadata.
 * Tracks application lifecycle events and enriches them with device and app information.
 */
public class PushwooshDefaultEvents {
    private static volatile Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;
    private static final Object ACTIVITY_LIFECYCLE_CALLBACKS_MUTEX = new Object();

    static final String APPLICATION_OPENED_EVENT = "PW_ApplicationOpen";
    static final String SCREEN_OPENED_EVENT = "PW_ScreenOpen";
    static final String APPLICATION_CLOSED_EVENT = "PW_ApplicationMinimized";
    static final String USER_IDLE_EVENT = "PW_UserIdle";

    /**
     * Initializes event tracking by registering activity lifecycle callbacks
     */
    public void init() {
        registerActivityLifecycleCallbacks();
    }

    private EventListener<ApplicationOpenEvent> applicationOpenEventListener = (event) -> {
        EventBus.unsubscribe(ApplicationOpenEvent.class, PushwooshDefaultEvents.this.applicationOpenEventListener);
        registerActivityLifecycleCallbacks();
    };

    /**
     * Posts event with attributes to InApp manager
     * @param eventName Name of the event
     * @param attributes Additional event attributes
     */
    void postEvent(String eventName, TagsBundle attributes) {
        InAppManager.getInstance().postEvent(eventName, attributes);
    }

    /**
     * Builds attributes bundle for event with device and app information
     * @param eventName Name of the event
     * @param activityName Name of the activity (for screen events)
     * @return Bundle with event attributes
     */
    private static TagsBundle.Builder buildBaseAttributes() {
        TagsBundle.Builder attributes = new TagsBundle.Builder();
        attributes.putInt("device_type", DeviceSpecificProvider.getInstance().deviceType());
        if (AndroidPlatformModule.getAppInfoProvider() != null
                && AndroidPlatformModule.getAppInfoProvider().getVersionName() != null) {
            attributes.putString(
                    "application_version",
                    AndroidPlatformModule.getAppInfoProvider().getVersionName());
        }
        return attributes;
    }

    static TagsBundle buildAttributes(String eventName, String activityName) {
        TagsBundle.Builder attributes = buildBaseAttributes();
        if (eventName.equals(SCREEN_OPENED_EVENT) && activityName != null) {
            attributes.putString("screen_name", activityName);
        }
        return attributes.build();
    }

    static TagsBundle buildIdleAttributes(@Nullable String activityName, int idleSeconds, long sessionDurationSeconds) {
        TagsBundle.Builder attributes = buildBaseAttributes();
        if (activityName != null) {
            attributes.putString("screen_name", activityName);
        }
        attributes.putInt("idle_seconds", idleSeconds);
        attributes.putLong("session_duration", sessionDurationSeconds);
        return attributes.build();
    }

    /**
     * Registers activity lifecycle callbacks to track application events
     */
    private void registerActivityLifecycleCallbacks() {
        synchronized (ACTIVITY_LIFECYCLE_CALLBACKS_MUTEX) {
            if (activityLifecycleCallbacks == null) {
                Application app = (Application) AndroidPlatformModule.getApplicationContext();
                if (app == null) {
                    EventBus.subscribe(ApplicationOpenEvent.class, applicationOpenEventListener);
                } else {
                    int idleTimeout =
                            PushwooshPlatform.getInstance().getConfig().getIdleTimeoutSeconds();
                    PushwooshAppLifecycleCallbacks.IdleEventCallback idleEventCallback = null;
                    if (idleTimeout > 0) {
                        idleEventCallback = (actName, idleSec, sessionDur) -> {
                            SdkStateProvider.getInstance().executeOrQueue(() -> {
                                postEvent(USER_IDLE_EVENT, buildIdleAttributes(actName, idleSec, sessionDur));
                            });
                        };
                    }
                    activityLifecycleCallbacks = new PushwooshAppLifecycleCallbacks(
                            (eventName, activityName) -> {
                                SdkStateProvider.getInstance().executeOrQueue(() -> {
                                    switch (eventName) {
                                        case PushwooshAppLifecycleCallbacks.APPLICATION_OPENED_EVENT:
                                            postEvent(
                                                    APPLICATION_OPENED_EVENT,
                                                    buildAttributes(APPLICATION_OPENED_EVENT, activityName));
                                            break;
                                        case PushwooshAppLifecycleCallbacks.APPLICATION_CLOSED_EVENT:
                                            postEvent(
                                                    APPLICATION_CLOSED_EVENT,
                                                    buildAttributes(APPLICATION_CLOSED_EVENT, activityName));
                                            break;
                                        case PushwooshAppLifecycleCallbacks.SCREEN_OPENED_EVENT:
                                            postEvent(
                                                    SCREEN_OPENED_EVENT,
                                                    buildAttributes(SCREEN_OPENED_EVENT, activityName));
                                            break;
                                    }
                                });
                            },
                            idleTimeout,
                            idleEventCallback);
                    app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
                }
            }
        }
    }
}
