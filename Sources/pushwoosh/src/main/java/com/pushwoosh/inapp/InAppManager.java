package com.pushwoosh.inapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.exception.ReloadInAppsException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.tags.TagsBundle;

/**
 * InAppManager is responsible for In-App messaging functionality.
 *
 * @see <a href="https://docs.pushwoosh.com/platform-docs/automation/behavior-based-messaging/in-app-messaging">In-App Messaging</a>
 */
public class InAppManager {
    private static InAppManager instance = new InAppManager();

    private final PushwooshInAppImpl impl;

    private InAppManager() {
        if (PushwooshPlatform.getInstance() != null) {
            impl = PushwooshPlatform.getInstance().pushwooshInApp();
        } else {
            PushwooshPlatform.notifyNotInitialized();
            impl = null;
        }
    }

    /**
     * @return InAppManager shared instance.
     */
    @NonNull
    public static InAppManager getInstance() {
        return instance;
    }

    /**
     * {@link #postEvent(String, TagsBundle, Callback)}
     */
    public void postEvent(@NonNull String event) {
        if (impl != null)
            impl.postEvent(event, null, null, false);
    }

    /**
     * {@link #postEvent(String, TagsBundle, Callback)}
     */
    public void postEvent(@NonNull String event, TagsBundle attributes) {
        if (impl != null)
            impl.postEvent(event, attributes, null, false);
    }

    public void postEventInternal(@NonNull String event, TagsBundle attributes) {
        if (impl != null)
            impl.postEvent(event, attributes, null, true);
    }

    /**
     * Post events for In-App Messages. This can trigger In-App message HTML as specified in Pushwoosh Control Panel.
     *
     * @param event      name of the event
     * @param attributes additional event attributes
     * @param callback   method completion callback
     */
    public void postEvent(@NonNull String event, @Nullable TagsBundle attributes, Callback<Void, PostEventException> callback) {
        if (impl != null)
            impl.postEvent(event, attributes, callback, false);
    }

    /**
     * Add JavaScript interface for In-Apps extension. All exported methods should be marked with @JavascriptInterface annotation.
     *
     * @param object java object that will be available inside In-App page
     * @param name   specified object will be available as window.`name`
     */
    public void addJavascriptInterface(@NonNull Object object, @NonNull String name) {
        if (impl != null)
            impl.addJavascriptInterface(object, name);
    }

    /**
     * Removes object registered with {@link #addJavascriptInterface(Object, String)}
     *
     * @param name object name
     */
    public void removeJavascriptInterface(@NonNull String name) {
        if (impl != null)
            impl.removeJavascriptInterface(name);
    }

    /**
     * Same as {@link #addJavascriptInterface(Object, String)} but uses class name instead of object
     */
    public void registerJavascriptInterface(@NonNull String className, @NonNull String name) {
        if (impl != null)
            impl.registerJavascriptInterface(className, name);
    }

    public void reloadInApps() {
        reloadInApps(null);
    }


    public void reloadInApps(Callback<Boolean, ReloadInAppsException> callback) {
        if (impl != null)
            impl.reloadInApps(callback);
    }
}
