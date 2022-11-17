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

class PushwooshAppLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final int SCREEN_OPENED_EVENT_DELAY = 100;

    private Handler handler = new Handler();
    private LifeCycleCallback callback;
    private int activitiesCount;
    private String activityName;

    static final String APPLICATION_OPENED_EVENT = "ApplicationOpened";
    static final String SCREEN_OPENED_EVENT = "ScreenOpened";
    static final String APPLICATION_CLOSED_EVENT = "ApplicationClosed";

    PushwooshAppLifecycleCallbacks(@NonNull LifeCycleCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof FragmentActivity) {
            registerSupportFragmentListener((FragmentActivity) activity);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerFragmentListener(activity);
        }
        notifyScreenOpened();
    }

    private void registerSupportFragmentListener(FragmentActivity activity) {
        activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentStarted(FragmentManager fm, Fragment fragment) {
                super.onFragmentStarted(fm, fragment);
                notifyScreenOpened();
            }
        }, true);
    }

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

    private void notifyScreenOpened() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(
                () -> callback.invoke(SCREEN_OPENED_EVENT, activityName),
                SCREEN_OPENED_EVENT_DELAY);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        activityName = activity.getClass().getName();
        if (activitiesCount == 0) {
            callback.invoke(APPLICATION_OPENED_EVENT, activityName);
        }
        activitiesCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        activitiesCount--;
        if (activitiesCount == 0) {
            callback.invoke(APPLICATION_CLOSED_EVENT, activityName);
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    public interface LifeCycleCallback {
        void invoke(String activityName, String eventName);
    }
}
