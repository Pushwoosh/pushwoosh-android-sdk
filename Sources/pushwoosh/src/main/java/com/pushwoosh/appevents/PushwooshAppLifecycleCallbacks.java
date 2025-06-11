package com.pushwoosh.appevents;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.event.ActivityBroughtOnTopEvent;
import com.pushwoosh.internal.event.EventBus;

/**
 * Tracks application lifecycle events and notifies about screen/application state changes.
 * Handles activity and fragment lifecycle callbacks to detect when app is opened, closed or screen is changed.
 */
class PushwooshAppLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final int SCREEN_OPENED_EVENT_DELAY = 100;

    private Handler handler = new Handler();
    private LifeCycleCallback callback;
    private int activitiesCount;
    private String activityName;

    static final String APPLICATION_OPENED_EVENT = "ApplicationOpened";
    static final String SCREEN_OPENED_EVENT = "ScreenOpened";
    static final String APPLICATION_CLOSED_EVENT = "ApplicationClosed";

    /**
     * Creates new lifecycle callback handler
     * @param callback Callback to be invoked when lifecycle events occur
     */
    PushwooshAppLifecycleCallbacks(@NonNull LifeCycleCallback callback) {
        this.callback = callback;
    }

    /**
     * Called when activity is created. Registers fragment listeners if activity supports fragments.
     * @param activity Created activity
     * @param savedInstanceState Saved instance state
     */
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (PushwooshPlatform.getInstance().getConfig().isCollectingLifecycleEventsAllowed()) {
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
        activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentStarted(FragmentManager fm, Fragment fragment) {
                super.onFragmentStarted(fm, fragment);
                notifyScreenOpened();
            }
        }, true);
    }

    /**
     * Registers fragment lifecycle callbacks for native fragments (API 26+)
     * @param activity Activity containing native fragments
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void registerFragmentListener(Activity activity) {
        activity.getFragmentManager().registerFragmentLifecycleCallbacks(new android.app.FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentStarted(android.app.FragmentManager fm, android.app.Fragment fragment) {
                super.onFragmentStarted(fm, fragment);
                notifyScreenOpened();
            }
        }, true);
    }

    /**
     * Notifies about screen opened event with delay to avoid duplicate events
     */
    private void notifyScreenOpened() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(
                () -> callback.invoke(SCREEN_OPENED_EVENT, activityName),
                SCREEN_OPENED_EVENT_DELAY);
    }

    /**
     * Called when activity is started. Tracks application open state.
     * @param activity Started activity
     */
    @Override
    public void onActivityStarted(Activity activity) {
        if (PushwooshPlatform.getInstance().getConfig().isCollectingLifecycleEventsAllowed()) {
            activityName = activity.getClass().getName();
            if (activitiesCount == 0) {
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
    public void onActivityResumed(Activity activity) {
        PushwooshPlatform.getInstance().setTopActivity(activity);
        EventBus.sendEvent(ActivityBroughtOnTopEvent.getInstance());
    }

    /**
     * Called when activity is paused. Clears top activity reference if needed.
     * @param activity Paused activity
     */
    @Override
    public void onActivityPaused(Activity activity) {
        if (PushwooshPlatform.getInstance().getTopActivity() != null && PushwooshPlatform.getInstance().getTopActivity() == activity) {
            PushwooshPlatform.getInstance().setTopActivity(null);
        }
    }

    /**
     * Called when activity is stopped. Tracks application close state.
     * @param activity Stopped activity
     */
    @Override
    public void onActivityStopped(Activity activity) {
        if (PushwooshPlatform.getInstance().getTopActivity() != null && PushwooshPlatform.getInstance().getTopActivity() == activity) {
            PushwooshPlatform.getInstance().setTopActivity(null);
        }
        if (PushwooshPlatform.getInstance().getConfig().isCollectingLifecycleEventsAllowed()) {
            activitiesCount--;
        if (activitiesCount == 0) {
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
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    /**
     * Called when activity is destroyed. Clears top activity reference if needed.
     * @param activity Destroyed activity
     */
    @Override
    public void onActivityDestroyed(Activity activity) {
        if (PushwooshPlatform.getInstance().getTopActivity() != null && PushwooshPlatform.getInstance().getTopActivity() == activity) {
            PushwooshPlatform.getInstance().setTopActivity(null);
        }
    }

    /**
     * Callback interface for lifecycle events
     */
    public interface LifeCycleCallback {
        /**
         * Called when lifecycle event occurs
         * @param eventName Name of the event
         * @param activityName Name of the activity
         */
        void invoke(String eventName, String activityName);
    }
}
