package com.pushwoosh.inapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.exception.MergeUserException;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.tags.TagsBundle;

/**
 * PushwooshInApp is responsible for In-App messages functionality.
 *
 * @deprecated
 * @see InAppManager
 */
@Deprecated
public class PushwooshInApp {
    private static PushwooshInApp instance = new PushwooshInApp();

    /**
     * @return PushwooshInApp shared instance.
     */
    @NonNull
    public static PushwooshInApp getInstance() {
        return instance;
    }

    /**
     * @deprecated
     * @see InAppManager#postEvent(String)
     */
    @Deprecated
    public void postEvent(@NonNull String event) {
        InAppManager.getInstance().postEvent(event);
    }

    /**
     * @deprecated
     * @see InAppManager#postEvent(String, TagsBundle)
     */
    @Deprecated
    public void postEvent(@NonNull String event, TagsBundle attributes) {
        InAppManager.getInstance().postEvent(event, attributes);
    }

    /**
     * @deprecated
     * @see InAppManager#postEvent(String, TagsBundle, Callback)
     */
    @Deprecated
    public void postEvent(@NonNull String event, @Nullable TagsBundle attributes, Callback<Void, PostEventException> callback) {
        InAppManager.getInstance().postEvent(event, attributes, callback);
    }

    /**
     * @deprecated
     * @see com.pushwoosh.Pushwoosh#setUserId(String)
     */
    @Deprecated
    public void setUserId(@NonNull String userId) {
        Pushwoosh.getInstance().setUserId(userId);
    }

    /**
     * @deprecated
     * @see Pushwoosh#getUserId()
     */
    @Deprecated
    @Nullable
    public String getUserId() {
        return Pushwoosh.getInstance().getUserId();
    }

    /**
     * @deprecated
     * @see com.pushwoosh.Pushwoosh#mergeUserId(String, String, boolean, Callback)
     */
    @Deprecated
    public void mergeUserId(@NonNull String oldUserId, @NonNull String newUserId, boolean doMerge, @Nullable Callback<Void, MergeUserException> callback) {
        Pushwoosh.getInstance().mergeUserId(oldUserId, newUserId, doMerge, callback);
    }

    /**
     * @deprecated
     * @see InAppManager#addJavascriptInterface(Object, String)
     */
    @Deprecated
    public void addJavascriptInterface(@NonNull Object object, @NonNull String name) {
        InAppManager.getInstance().addJavascriptInterface(object, name);
    }

    /**
     * @deprecated
     * @see InAppManager#removeJavascriptInterface(String)
     */
    @Deprecated
    public void removeJavascriptInterface(@NonNull String name) {
        InAppManager.getInstance().removeJavascriptInterface(name);
    }

    /**
     * @deprecated
     * @see InAppManager#registerJavascriptInterface(String, String)
     */
    @Deprecated
    public void registerJavascriptInterface(@NonNull String className, @NonNull String name) {
        InAppManager.getInstance().registerJavascriptInterface(className, name);
    }

    /**
     * @deprecated
     * @see InAppManager#resetBusinessCasesFrequencyCapping()
     */
    @Deprecated
    public void resetBusinessCasesFrequencyCapping() {
        InAppManager.getInstance().resetBusinessCasesFrequencyCapping();
    }
}
