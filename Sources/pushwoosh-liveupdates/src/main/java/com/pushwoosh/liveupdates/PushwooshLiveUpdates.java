package com.pushwoosh.liveupdates;

import android.annotation.SuppressLint;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.liveupdates.internal.LiveUpdateNotificationRenderer;

import java.util.Collections;
import java.util.List;

/**
 * Entry point for controlling Pushwoosh Live Updates that are already on screen.
 * <p>
 * A Live Update is an ongoing, progress-style notification (Android 16 / API 36+) that the SDK
 * renders automatically from server-sent push messages. There is no API to start or update one
 * from the app — the Pushwoosh backend is the source of truth and drives every {@code start} /
 * {@code update} / {@code end} transition. This facade covers only the app-side operations the
 * server cannot perform: dismissing a Live Update locally and querying which ones are on screen.
 * <p>
 * To customize how a Live Update looks, implement {@link LiveUpdateProgressStyleProvider} and
 * register it via manifest meta-data — see that interface for details.
 * <p>
 * <b>Availability:</b> Live Updates require Android 16 (API 36) or newer. On older devices the
 * plugin never activates and every method here is a safe no-op ({@link #getActiveIds()} returns
 * an empty list).
 * <p>
 * <b>Example:</b>
 * <pre>
 * {@code
 *   // Dismiss a specific Live Update once the user cancels the order in-app,
 *   // without waiting for the server's terminal "end" push
 *   PushwooshLiveUpdates.endLiveUpdate("order_4521");
 *
 *   // Or clear everything this app is currently showing, e.g. on logout
 *   PushwooshLiveUpdates.endAllLiveUpdates();
 * }
 * </pre>
 * <p>
 * All methods are safe to call from any thread.
 *
 * @see LiveUpdateProgressStyleProvider
 * @see LiveUpdateState
 */
public final class PushwooshLiveUpdates {

    private static final String TAG = "PushwooshLiveUpdates";

    private static volatile LiveUpdateNotificationRenderer renderer;

    private PushwooshLiveUpdates() {}

    /**
     * Installs the SDK-owned renderer. Called once by {@link LiveUpdatesPlugin} during
     * initialization on API 36+; not part of the public API.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void install(@NonNull LiveUpdateNotificationRenderer initial) {
        renderer = initial;
    }

    /**
     * Dismisses the Live Update with the given {@code activityId}.
     * <p>
     * Use this when the app knows an activity has finished before the server sends its terminal
     * {@code end} push — for example the user cancels an order from inside the app. If no Live
     * Update with this id is currently shown, the call does nothing.
     * <p>
     * Safe to call from any thread; a no-op on devices below API 36.
     *
     * @param activityId the id identifying the Live Update, as supplied in its push payload
     */
    @SuppressLint("NewApi")
    @AnyThread
    public static void endLiveUpdate(@NonNull String activityId) {
        PWLog.noise(TAG, "endLiveUpdate(activityId=" + activityId + ")");
        LiveUpdateNotificationRenderer r = renderer;
        if (r == null) return;
        try {
            r.dismiss(activityId);
        } catch (Throwable t) {
            PWLog.error(TAG, "renderer.dismiss failed for " + activityId, t);
        }
    }

    /**
     * Returns the ids of all Live Updates this app is currently showing.
     * <p>
     * Useful for reconciling app state with what is on screen — for example deciding whether to
     * call {@link #endLiveUpdate(String)} for an activity the app already considers finished.
     * <p>
     * Safe to call from any thread; runs a short binder call to
     * {@link android.app.NotificationManager#getActiveNotifications()} — keep it off the hot UI
     * path.
     *
     * @return the activityIds of active Live Updates, or an empty list if none are shown
     *         (including on devices below API 36)
     */
    @SuppressLint("NewApi")
    @AnyThread
    @NonNull public static List<String> getActiveIds() {
        PWLog.noise(TAG, "getActiveIds()");
        LiveUpdateNotificationRenderer r = renderer;
        if (r == null) return Collections.emptyList();
        try {
            return r.getActiveIds();
        } catch (Throwable t) {
            PWLog.error(TAG, "renderer.getActiveIds failed", t);
            return Collections.emptyList();
        }
    }

    /**
     * Dismisses every Live Update this app is currently showing.
     * <p>
     * A convenience over calling {@link #endLiveUpdate(String)} for each id returned by
     * {@link #getActiveIds()} — for example on logout, when nothing should remain on screen.
     * <p>
     * Safe to call from any thread; runs a short binder call to
     * {@link android.app.NotificationManager#getActiveNotifications()} — keep it off the hot UI
     * path. A no-op on devices below API 36.
     */
    @SuppressLint("NewApi")
    @AnyThread
    public static void endAllLiveUpdates() {
        PWLog.noise(TAG, "endAllLiveUpdates()");
        LiveUpdateNotificationRenderer r = renderer;
        if (r == null) return;
        List<String> ids;
        try {
            ids = r.getActiveIds();
        } catch (Throwable t) {
            PWLog.error(TAG, "renderer.getActiveIds failed", t);
            return;
        }
        for (String id : ids) {
            try {
                r.dismiss(id);
            } catch (Throwable t) {
                PWLog.error(TAG, "renderer.dismiss failed for " + id, t);
            }
        }
    }

    /** Test hook: directly sets the renderer (or clears it with {@code null}). */
    @VisibleForTesting
    public static void installForTest(@Nullable LiveUpdateNotificationRenderer r) {
        renderer = r;
    }

    /**
     * Returns the installed renderer, or {@code null} before initialization or below API 36.
     * Used by {@link com.pushwoosh.liveupdates.internal.LiveUpdatePushHandler} as its renderer
     * supplier; not part of the public API.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable public static LiveUpdateNotificationRenderer getActiveRenderer() {
        return renderer;
    }
}
