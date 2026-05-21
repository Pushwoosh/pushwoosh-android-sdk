/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.internal.platform;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.pushwoosh.internal.event.Event;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;

import java.util.Date;

public class ApplicationOpenDetector {
    private static final String TAG = "ApplicationOpenDetector";

    // Debounce window for closing transitions. Copied from AndroidX ProcessLifecycleOwner.TIMEOUT_MS:
    // long enough to ride out Activity recreate on rotation/configChange even on slow devices.
    @VisibleForTesting
    static final long TIMEOUT_MS = 700;

    private Date firstLaunchDate;

    public static class ApplicationOpenEvent implements Event {
        public ApplicationOpenEvent() {
            /*do nothing*/
        }
    }

    public static class ApplicationMovedToForegroundEvent implements Event {}

    public static class ApplicationMovedToBackgroundEvent implements Event {}

    private final Application context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    ApplicationOpenDetector(final Context context) {
        this.context = (Application) context.getApplicationContext();
    }

    public void onApplicationCreated(boolean isFirstLaunch) {
        if (isFirstLaunch) {
            EventBus.sendEvent(new ApplicationOpenEvent());
            firstLaunchDate = new Date();
            PWLog.debug(TAG, "First launch, ApplicationOpenEvent fired");
        }
        context.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private int activityCreatedDestroyedCount;
            private int activityStartedStoppedCount;
            private boolean inForeground;
            private boolean inOpened;

            private final Runnable goBackgroundRunnable = () -> {
                if (activityStartedStoppedCount == 0 && inForeground) {
                    inForeground = false;
                    EventBus.sendEvent(new ApplicationMovedToBackgroundEvent());
                    PWLog.debug(TAG, "ApplicationMovedToBackgroundEvent fired");
                }
            };

            private final Runnable resetOpenedRunnable = () -> {
                if (activityCreatedDestroyedCount == 0 && inOpened) {
                    inOpened = false;
                    PWLog.debug(TAG, "inOpened reset after debounce");
                }
            };

            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                final long appOpenEventTimeoutOnFirstLaunch = 1000 * 60; // 1 min

                // first launch condition
                if (firstLaunchDate != null) {
                    long timePassedFromFirstLaunch = new Date().getTime() - firstLaunchDate.getTime();
                    if (timePassedFromFirstLaunch >= appOpenEventTimeoutOnFirstLaunch) {
                        EventBus.sendEvent(new ApplicationOpenEvent());
                        PWLog.debug(TAG, "ApplicationOpenEvent fired");
                    }
                    firstLaunchDate = null;
                    // Open was already sent (either in onApplicationCreated for isFirstLaunch,
                    // or just above for the >=60s path). Mark opened so rotation/return debounce works.
                    inOpened = true;
                } else if (activityCreatedDestroyedCount == 0) {
                    if (!inOpened) {
                        inOpened = true;
                        EventBus.sendEvent(new ApplicationOpenEvent());
                        PWLog.debug(TAG, "ApplicationOpenEvent fired");
                    } else {
                        handler.removeCallbacks(resetOpenedRunnable);
                    }
                }
                activityCreatedDestroyedCount++;
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (activityStartedStoppedCount == 0) {
                    if (!inForeground) {
                        inForeground = true;
                        EventBus.sendEvent(new ApplicationMovedToForegroundEvent());
                        PWLog.debug(TAG, "ApplicationMovedToForegroundEvent fired");
                    } else {
                        handler.removeCallbacks(goBackgroundRunnable);
                    }
                }
                activityStartedStoppedCount++;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                // Stub
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // Stub
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                activityStartedStoppedCount--;
                if (activityStartedStoppedCount == 0) {
                    handler.postDelayed(goBackgroundRunnable, TIMEOUT_MS);
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                // Stub
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                activityCreatedDestroyedCount--;
                if (activityCreatedDestroyedCount == 0) {
                    handler.postDelayed(resetOpenedRunnable, TIMEOUT_MS);
                }
            }
        });
    }
}
