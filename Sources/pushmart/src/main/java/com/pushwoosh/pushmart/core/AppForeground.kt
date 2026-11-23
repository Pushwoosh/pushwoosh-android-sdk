package com.pushwoosh.pushmart.core

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Reports the app entering and leaving the foreground: the first started activity and the last
 * stopped one. A configuration change (rotation) stops and restarts the activity without a report.
 */
class AppForeground(private val onChange: (foreground: Boolean) -> Unit) :
    Application.ActivityLifecycleCallbacks {
    private var started = 0
    private var changingConfigurations = false

    override fun onActivityStarted(activity: Activity) {
        if (started++ == 0 && !changingConfigurations) onChange(true)
        changingConfigurations = false
    }

    override fun onActivityStopped(activity: Activity) {
        started--
        changingConfigurations = activity.isChangingConfigurations
        if (started == 0 && !changingConfigurations) onChange(false)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
