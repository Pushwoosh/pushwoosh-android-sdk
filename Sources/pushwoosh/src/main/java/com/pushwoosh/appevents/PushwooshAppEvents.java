package com.pushwoosh.appevents;

import android.app.Application;

import com.pushwoosh.inapp.InAppManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;

public class PushwooshAppEvents {
    private static volatile Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;
    private static final Object ACTIVITY_LIFECYCLE_CALLBACKS_MUTEX = new Object();
    private static final String APPLICATION_OPENED_EVENT = "_ApplicationOpened";
    private static final String SCREEN_OPENED_EVENT = "_ScreenOpened";
    private static final String APPLICATION_CLOSED_EVENT = "_ApplicationClosed";

    public static void init() {
        registerActivityLifecycleCallbacks();
    }

    private static Application.ActivityLifecycleCallbacks registerActivityLifecycleCallbacks() {
        synchronized (ACTIVITY_LIFECYCLE_CALLBACKS_MUTEX) {
            if (activityLifecycleCallbacks == null) {
                activityLifecycleCallbacks = new PushwooshAppLifecycleCallbacks((eventName, activityName) -> {
                    switch (eventName) {
                        case PushwooshAppLifecycleCallbacks.APPLICATION_OPENED_EVENT:
                            InAppManager.getInstance().postEvent(APPLICATION_OPENED_EVENT);
                            break;
                        case PushwooshAppLifecycleCallbacks.APPLICATION_CLOSED_EVENT:
                            InAppManager.getInstance().postEvent(APPLICATION_CLOSED_EVENT);
                            break;
                        case PushwooshAppLifecycleCallbacks.SCREEN_OPENED_EVENT:
                            InAppManager.getInstance().postEvent(SCREEN_OPENED_EVENT);
                            break;
                    }
                });
                Application app = (Application) AndroidPlatformModule.getApplicationContext();
                if (app != null) {
                    app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
                }
            }
        }
        return activityLifecycleCallbacks;
    }
}
