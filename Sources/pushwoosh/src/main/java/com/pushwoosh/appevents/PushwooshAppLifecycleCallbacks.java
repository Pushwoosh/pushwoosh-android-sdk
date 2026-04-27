package com.pushwoosh.appevents;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.event.ActivityBroughtOnTopEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;

/**
 * Tracks application lifecycle events and notifies about screen/application state changes.
 * Handles activity and fragment lifecycle callbacks to detect when app is opened, closed or screen is changed.
 */
class PushwooshAppLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "PushwooshAppLifecycleCallbacks";
    private static final int SCREEN_OPENED_EVENT_DELAY = 100;
    private static final long APP_CLOSED_DEBOUNCE_MS = 1000;
    static final String APPLICATION_OPENED_EVENT = "ApplicationOpened";
    static final String SCREEN_OPENED_EVENT = "ScreenOpened";
    static final String APPLICATION_CLOSED_EVENT = "ApplicationClosed";
    private final LifeCycleCallback lifecycleCallback;
    private int activitiesCount;
    private String activityName;
    private String currentFragmentName;
    private long foregroundTimestamp;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final PendingEvent appClosed;
    private final PendingEvent screenOpened;
    private boolean suppressScreenOpened;

    @Nullable private IdleDetector idleDetector;

    @Nullable private IdleEventCallback idleEventCallback;

    @Nullable private ExitIntentDetector exitIntentDetector;

    PushwooshAppLifecycleCallbacks(
            @NonNull LifeCycleCallback lifecycleCallback,
            int idleTimeoutSeconds,
            @Nullable IdleEventCallback idleEventCallback,
            int exitIntentTimeoutSeconds,
            @Nullable ExitIntentDetector.ExitIntentCallback exitIntentCallback) {
        this.lifecycleCallback = lifecycleCallback;
        this.appClosed = new PendingEvent(
                handler,
                APP_CLOSED_DEBOUNCE_MS,
                () -> lifecycleCallback.invoke(APPLICATION_CLOSED_EVENT, activityName));
        this.screenOpened = new PendingEvent(
                handler, SCREEN_OPENED_EVENT_DELAY, () -> lifecycleCallback.invoke(SCREEN_OPENED_EVENT, activityName));
        if (idleTimeoutSeconds > 0 && idleEventCallback != null) {
            this.idleEventCallback = idleEventCallback;
            this.idleDetector = new IdleDetector(idleTimeoutSeconds, this::onIdleDetected);
            PWLog.debug(TAG, "Idle detection enabled with timeout " + idleTimeoutSeconds + "s");
        }
        if (exitIntentTimeoutSeconds > 0 && exitIntentCallback != null) {
            this.exitIntentDetector = new ExitIntentDetector(exitIntentTimeoutSeconds, exitIntentCallback);
            PWLog.debug(TAG, "Exit Intent detection enabled with timeout " + exitIntentTimeoutSeconds + "s");
        }
    }

    // --- Activity lifecycle callbacks ---

    /**
     * Called when activity is created. Registers fragment listeners if activity supports fragments.
     * @param activity Created activity
     * @param savedInstanceState Saved instance state
     */
    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        if (!PushwooshPlatform.getInstance().getConfig().isCollectingLifecycleEventsAllowed()) {
            return;
        }
        currentFragmentName = null;
        if (activity instanceof FragmentActivity) {
            registerSupportFragmentListener((FragmentActivity) activity);
        }
        notifyScreenOpened();
    }

    /**
     * Called when activity is started. Tracks application open state.
     * @param activity Started activity
     */
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!PushwooshPlatform.getInstance().getConfig().isCollectingLifecycleEventsAllowed()) {
            return;
        }
        activityName = activity.getClass().getName();
        if (activitiesCount == 0) {
            if (appClosed.isPending()) {
                // User returned within debounce window — suppress the whole minimized/open/screen pack.
                // Flag gates the notifyScreenOpened() calls coming synchronously from
                // FragmentActivity.onStart() → dispatchStart() → onFragmentStarted, which would
                // otherwise re-schedule screenOpened right after our cancel(). The handler.post()
                // clear runs at the end of the current main-thread dispatch, after dispatchStart().
                appClosed.cancel();
                screenOpened.cancel();
                suppressScreenOpened = true;
                handler.post(() -> suppressScreenOpened = false);
            } else {
                foregroundTimestamp = SystemClock.elapsedRealtime();
                lifecycleCallback.invoke(APPLICATION_OPENED_EVENT, activityName);
            }
            if (exitIntentDetector != null) {
                exitIntentDetector.onAppForegrounded();
            }
        }
        activitiesCount++;
    }

    /**
     * Called when activity is resumed. Updates top activity reference.
     * @param activity Resumed activity
     */
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        PushwooshPlatform.getInstance().setTopActivity(activity);
        EventBus.sendEvent(ActivityBroughtOnTopEvent.getInstance());
        if (idleDetector != null) {
            idleDetector.onActivityResumed(activity);
        }
    }

    /**
     * Called when activity is paused. Clears top activity reference if needed.
     * @param activity Paused activity
     */
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (idleDetector != null) {
            idleDetector.onActivityPaused();
        }
        clearTopActivityIfCurrent(activity);
    }

    /**
     * Called when activity is stopped. Tracks application close state.
     * @param activity Stopped activity
     */
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        clearTopActivityIfCurrent(activity);
        if (!PushwooshPlatform.getInstance().getConfig().isCollectingLifecycleEventsAllowed()) {
            return;
        }
        activitiesCount--;
        if (activitiesCount != 0) {
            return;
        }
        if (idleDetector != null) {
            idleDetector.onAppBackgrounded();
        }
        if (exitIntentDetector != null) {
            exitIntentDetector.onAppBackgrounded(
                    composeScreenName(activity.getClass().getSimpleName()), sessionDurationSeconds());
        }
        screenOpened.cancel();
        appClosed.schedule();
    }

    /**
     * Called when activity state is saved
     * @param activity Activity being saved
     * @param outState Bundle to save state
     */
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    /**
     * Called when activity is destroyed. Clears top activity reference if needed.
     * @param activity Destroyed activity
     */
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        clearTopActivityIfCurrent(activity);
    }

    // --- Fragment listeners ---

    /**
     * Registers fragment lifecycle callbacks for support fragments
     * @param activity Activity containing support fragments
     */
    private void registerSupportFragmentListener(FragmentActivity activity) {
        activity.getSupportFragmentManager()
                .registerFragmentLifecycleCallbacks(
                        new FragmentManager.FragmentLifecycleCallbacks() {
                            @Override
                            public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
                                super.onFragmentStarted(fm, fragment);
                                if (isUserFragment(fragment.getClass())) {
                                    currentFragmentName = fragment.getClass().getSimpleName();
                                }
                                notifyScreenOpened();
                            }
                        },
                        true);
    }

    private static boolean isUserFragment(Class<?> fragmentClass) {
        String name = fragmentClass.getName();
        return !name.startsWith("androidx.")
                && !name.startsWith("android.")
                && !name.startsWith("com.google.")
                && !name.startsWith("com.pushwoosh.");
    }

    // --- Detector forwarding ---

    /**
     * Notifies about screen opened event with delay to avoid duplicate events
     */
    private void notifyScreenOpened() {
        if (suppressScreenOpened) {
            return;
        }
        screenOpened.schedule();
    }

    /**
     * Called when user idle is detected. Enriches event with session data and forwards to callback.
     * @param activitySimpleName Simple name of the activity where idle was detected
     * @param idleSeconds Number of seconds user was idle
     */
    private void onIdleDetected(String activitySimpleName, int idleSeconds) {
        if (idleEventCallback == null) {
            return;
        }
        idleEventCallback.onIdle(composeScreenName(activitySimpleName), sessionDurationSeconds(), idleSeconds);
    }

    // --- Computations & misc ---

    private long sessionDurationSeconds() {
        return (SystemClock.elapsedRealtime() - foregroundTimestamp) / 1000;
    }

    @Nullable private String composeScreenName(@Nullable String activitySimpleName) {
        if (activitySimpleName != null && currentFragmentName != null) {
            return activitySimpleName + "/" + currentFragmentName;
        }
        return activitySimpleName;
    }

    private static void clearTopActivityIfCurrent(@NonNull Activity activity) {
        if (PushwooshPlatform.getInstance().getTopActivity() != null
                && PushwooshPlatform.getInstance().getTopActivity() == activity) {
            PushwooshPlatform.getInstance().setTopActivity(null);
        }
    }

    // --- Nested types ---

    /**
     * Callback interface for lifecycle events
     */
    public interface LifeCycleCallback {
        void invoke(String eventName, String activityName);
    }

    interface IdleEventCallback {
        void onIdle(@Nullable String screenName, long sessionDurationSeconds, int idleSeconds);
    }

    private static final class PendingEvent {
        private final Handler handler;
        private final long delayMs;
        private final Runnable action;
        private final Runnable internal = this::fire;
        private boolean pending;

        PendingEvent(@NonNull Handler handler, long delayMs, @NonNull Runnable action) {
            this.handler = handler;
            this.delayMs = delayMs;
            this.action = action;
        }

        void schedule() {
            handler.removeCallbacks(internal);
            pending = true;
            handler.postDelayed(internal, delayMs);
        }

        void cancel() {
            handler.removeCallbacks(internal);
            pending = false;
        }

        boolean isPending() {
            return pending;
        }

        private void fire() {
            if (!pending) {
                return;
            }
            pending = false;
            action.run();
        }
    }
}
