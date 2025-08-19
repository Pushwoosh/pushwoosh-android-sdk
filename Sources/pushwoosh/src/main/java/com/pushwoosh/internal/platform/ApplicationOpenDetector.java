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

import com.pushwoosh.internal.event.Event;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;

import java.util.Date;

public class ApplicationOpenDetector {
    private Date firstLaunchDate;

    public static class ApplicationOpenEvent implements Event {
        public ApplicationOpenEvent() {/*do nothing*/}
    }

    public static class ApplicationMovedToForegroundEvent implements Event {
    }

    public static class ApplicationMovedToBackgroundEvent implements Event {
    }

    private final Application context;


    ApplicationOpenDetector(final Context context) {
        this.context = (Application) context.getApplicationContext();
    }

    public void onApplicationCreated(boolean isFirstLaunch) {
        if (isFirstLaunch) {
            EventBus.sendEvent(new ApplicationOpenEvent());
            firstLaunchDate = new Date();
            PWLog.debug("ApplicationOpenDetector", "First launch, ApplicationOpenEvent fired");
        }
        context.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private int activityCreatedDestroyedCount;
            private int activityStartedStoppedCount;

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                final long appOpenEventTimeoutOnFirstLaunch = 1000 * 60; // 1 min

                // first launch condition
                if (firstLaunchDate != null) {
                    long timePassedFromFirstLaunch = new Date().getTime() - firstLaunchDate.getTime();
                    if (timePassedFromFirstLaunch >= appOpenEventTimeoutOnFirstLaunch) {
                        EventBus.sendEvent(new ApplicationOpenEvent());
                        PWLog.debug("ApplicationOpenDetector", "ApplicationOpenEvent fired");
                    }
                    firstLaunchDate = null;
                } else if (activityCreatedDestroyedCount == 0) {
                    EventBus.sendEvent(new ApplicationOpenEvent());
                    PWLog.debug("ApplicationOpenDetector", "ApplicationOpenEvent fired");
                }
                activityCreatedDestroyedCount++;
            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (activityStartedStoppedCount == 0) {
                    EventBus.sendEvent(new ApplicationMovedToForegroundEvent());
                }
                activityStartedStoppedCount++;
            }

            @Override
            public void onActivityResumed(Activity activity) {
                // Stub
            }

            @Override
            public void onActivityPaused(Activity activity) {
                // Stub
            }

            @Override
            public void onActivityStopped(Activity activity) {
                activityStartedStoppedCount--;
                if (activityStartedStoppedCount == 0) {
                    EventBus.sendEvent(new ApplicationMovedToBackgroundEvent());
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                // Stub
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                activityCreatedDestroyedCount--;
            }
        });
    }
}
