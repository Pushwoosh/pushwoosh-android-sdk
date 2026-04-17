package com.pushwoosh.appevents;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LifeCycleCallback callback;
    private int activitiesCount;
    private String activityName;
    private String currentScreenName;
    private long foregroundTimestamp;

    @Nullable private IdleDetector idleDetector;

    @Nullable private IdleEventCallback idleEventCallback;

    static final String APPLICATION_OPENED_EVENT = "ApplicationOpened";
    static final String SCREEN_OPENED_EVENT = "ScreenOpened";
    static final String APPLICATION_CLOSED_EVENT = "ApplicationClosed";

    PushwooshAppLifecycleCallbacks(
            @NonNull LifeCycleCallback callback,
            int idleTimeoutSeconds,
            @Nullable IdleEventCallback idleEventCallback) {
        this.callback = callback;
        if (idleTimeoutSeconds <= 0 || idleEventCallback == null) {
            return;
        }
        this.idleEventCallback = idleEventCallback;
        this.idleDetector = new IdleDetector(idleTimeoutSeconds, this::onIdleDetected);
        PWLog.debug(TAG, "Idle detection enabled with timeout " + idleTimeoutSeconds + "s");
    }

    /**
     * Called when activity is created. Registers fragment listeners if activity supports fragments.
     * @param activity Created activity
     * @param savedInstanceState Saved instance state
     */
    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        if (PushwooshPlatform.getInstance().getConfig().isCollectingLifecycleEventsAllowed()) {
            currentScreenName = null;
            if (activity instanceof FragmentActivity) {
                registerSupportFragmentListener((FragmentActivity) activity);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerFragmentListener(activity);
            }
            notifyScreenOpened();
        }
    }

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
                                    currentScreenName = fragment.getClass().getSimpleName();
                                }
                                notifyScreenOpened();
                            }
                        },
                        true);
    }

    /**
     * Registers fragment lifecycle callbacks for native fragments (API 26+)
     * @param activity Activity containing native fragments
     */
    @SuppressWarnings("deprecation")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void registerFragmentListener(Activity activity) {
        activity.getFragmentManager()
                .registerFragmentLifecycleCallbacks(
                        new android.app.FragmentManager.FragmentLifecycleCallbacks() {
                            @Override
                            public void onFragmentStarted(
                                    android.app.FragmentManager fm, android.app.Fragment fragment) {
                                super.onFragmentStarted(fm, fragment);
                                if (isUserFragment(fragment.getClass())) {
                                    currentScreenName = fragment.getClass().getSimpleName();
                                }
                                notifyScreenOpened();
                            }
                        },
                        true);
    }

    private static boolean isUserFragment(Class<?> fragmentClass) {
        String name = fragmentClass.getName();
        return !name.startsWith("androidx.") && !name.startsWith("android.") && !name.startsWith("com.google.")
                && !name.startsWith("com.pushwoosh.");
    }

    /**
     * Notifies about screen opened event with delay to avoid duplicate events
     */
    private void notifyScreenOpened() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> callback.invoke(SCREEN_OPENED_EVENT, activityName), SCREEN_OPENED_EVENT_DELAY);
    }

    /**
     * Called when activity is started. Tracks application open state.
     * @param activity Started activity
     */
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (PushwooshPlatform.getInstance().getConfig().isCollectingLifecycleEventsAllowed()) {
            activityName = activity.getClass().getName();
            if (activitiesCount == 0) {
                foregroundTimestamp = SystemClock.elapsedRealtime();
                callback.invoke(APPLICATION_OPENED_EVENT, activityName);
            }
            activitiesCount++;
        }
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
        if (PushwooshPlatform.getInstance().getTopActivity() != null
                && PushwooshPlatform.getInstance().getTopActivity() == activity) {
            PushwooshPlatform.getInstance().setTopActivity(null);
        }
    }

    /**
     * Called when activity is stopped. Tracks application close state.
     * @param activity Stopped activity
     */
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (PushwooshPlatform.getInstance().getTopActivity() != null
                && PushwooshPlatform.getInstance().getTopActivity() == activity) {
            PushwooshPlatform.getInstance().setTopActivity(null);
        }
        if (PushwooshPlatform.getInstance().getConfig().isCollectingLifecycleEventsAllowed()) {
            activitiesCount--;
            if (activitiesCount == 0) {
                if (idleDetector != null) {
                    idleDetector.onAppBackgrounded();
                }
                callback.invoke(APPLICATION_CLOSED_EVENT, activityName);
            }
        }
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
        if (PushwooshPlatform.getInstance().getTopActivity() != null
                && PushwooshPlatform.getInstance().getTopActivity() == activity) {
            PushwooshPlatform.getInstance().setTopActivity(null);
        }
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
        long sessionDuration = (SystemClock.elapsedRealtime() - foregroundTimestamp) / 1000;
        String screenName;
        if (currentScreenName != null) {
            screenName = activitySimpleName + "/" + currentScreenName;
        } else {
            screenName = activitySimpleName;
        }
        idleEventCallback.onIdle(screenName, idleSeconds, sessionDuration);
    }

    /**
     * Callback interface for lifecycle events
     */
    public interface LifeCycleCallback {
        void invoke(String eventName, String activityName);
    }

    interface IdleEventCallback {
        void onIdle(String activityName, int idleSeconds, long sessionDurationSeconds);
    }
}
