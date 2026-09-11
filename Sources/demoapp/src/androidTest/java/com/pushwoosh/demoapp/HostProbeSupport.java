package com.pushwoosh.demoapp;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.richmedia.RichMediaColorSchemeResolver;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/** Probes shared by the host-activity rule tests (SDK-961). */
final class HostProbeSupport {

    private HostProbeSupport() {}

    static Context appContext() {
        return ApplicationProvider.getApplicationContext();
    }

    /** Mirrors the resolver: an unresolved isLightTheme is treated as light. */
    static boolean isLight(Context context) {
        TypedValue value = new TypedValue();
        boolean resolved = context.getTheme().resolveAttribute(android.R.attr.isLightTheme, value, true);
        return !resolved || value.data != 0;
    }

    /** Scheme the SDK would report to a Legacy rich media right now (no activity context). */
    static boolean schemeIsLight() {
        return isLight(RichMediaColorSchemeResolver.wrapForCurrentScheme(appContext()));
    }

    static Activity host() {
        return PushwooshPlatform.getInstance().getHostActivity();
    }

    static <T extends Activity> T waitForResumed(Class<T> type) {
        for (int i = 0; i < 200; i++) {
            AtomicReference<T> found = new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                Collection<Activity> resumed =
                        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
                for (Activity activity : resumed) {
                    if (type.isInstance(activity)) {
                        found.set(type.cast(activity));
                    }
                }
            });
            if (found.get() != null) {
                return found.get();
            }
            sleep(50);
        }
        throw new AssertionError("activity never resumed: " + type.getSimpleName());
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
