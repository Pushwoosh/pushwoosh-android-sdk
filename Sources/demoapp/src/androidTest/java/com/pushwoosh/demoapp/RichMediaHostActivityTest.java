package com.pushwoosh.demoapp;

import static com.pushwoosh.demoapp.HostProbeSupport.appContext;
import static com.pushwoosh.demoapp.HostProbeSupport.host;
import static com.pushwoosh.demoapp.HostProbeSupport.isLight;
import static com.pushwoosh.demoapp.HostProbeSupport.schemeIsLight;
import static com.pushwoosh.demoapp.HostProbeSupport.waitForResumed;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.util.TypedValue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.demoapp.hostprobe.DialogDarkActivity;
import com.pushwoosh.demoapp.hostprobe.FloatingLightActivity;
import com.pushwoosh.demoapp.hostprobe.NoAttrThemeActivity;
import com.pushwoosh.demoapp.hostprobe.OpaqueDarkActivity;
import com.pushwoosh.demoapp.hostprobe.OpaqueLightActivity;
import com.pushwoosh.demoapp.hostprobe.TranslucentActivity;
import com.pushwoosh.demoapp.hostprobe.TranslucentLightActivity;
import com.pushwoosh.richmedia.RichMediaColorSchemeResolver;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

/**
 * SDK-961: the host-activity rule against the real framework. The rule is "last started activity
 * with an opaque window"; these tests probe the theme filter, the ordering and the fallbacks.
 */
@RunWith(AndroidJUnit4.class)
public class RichMediaHostActivityTest {

    private static boolean translucentAttr(Activity activity) {
        TypedValue value = new TypedValue();
        boolean resolved = activity.getTheme().resolveAttribute(android.R.attr.windowIsTranslucent, value, true);
        return resolved && value.data != 0;
    }

    /** Called from inside ActivityScenario.onActivity, i.e. already on the main thread. */
    private static void startOnTop(Activity from, Class<? extends Activity> next) {
        from.startActivity(new Intent(from, next));
    }

    @Test
    public void opaqueLightActivityBecomesHostAndReportsLight() {
        try (ActivityScenario<OpaqueLightActivity> scenario = ActivityScenario.launch(OpaqueLightActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(PushwooshPlatform.isHostCandidate(activity));
                assertSame(activity, host());
            });
            assertTrue(schemeIsLight());
        }
    }

    @Test
    public void opaqueDarkActivityBecomesHostAndReportsDark() {
        try (ActivityScenario<OpaqueDarkActivity> scenario = ActivityScenario.launch(OpaqueDarkActivity.class)) {
            scenario.onActivity(activity -> assertSame(activity, host()));
            assertFalse(schemeIsLight());
        }
    }

    @Test
    public void translucentActivityDoesNotTakeOverHost() {
        try (ActivityScenario<OpaqueDarkActivity> scenario = ActivityScenario.launch(OpaqueDarkActivity.class)) {
            AtomicReference<Activity> opaque = new AtomicReference<>();
            scenario.onActivity(activity -> {
                opaque.set(activity);
                startOnTop(activity, TranslucentActivity.class);
            });
            Activity translucent = waitForResumed(TranslucentActivity.class);

            assertFalse(PushwooshPlatform.isHostCandidate(translucent));
            assertSame("host must stay on the opaque activity", opaque.get(), host());
            assertFalse(schemeIsLight());
        }
    }

    @Test
    public void translucentLightActivityDoesNotOverrideDarkHost() {
        try (ActivityScenario<OpaqueDarkActivity> scenario = ActivityScenario.launch(OpaqueDarkActivity.class)) {
            AtomicReference<Activity> opaque = new AtomicReference<>();
            scenario.onActivity(activity -> {
                opaque.set(activity);
                startOnTop(activity, TranslucentLightActivity.class);
            });
            Activity translucentLight = waitForResumed(TranslucentLightActivity.class);

            assertTrue("probe activity must really be light", isLight(translucentLight));
            assertFalse(PushwooshPlatform.isHostCandidate(translucentLight));
            assertSame(opaque.get(), host());
            assertFalse("a translucent light window must not lighten the scheme", schemeIsLight());
        }
    }

    @Test
    public void themeWithoutTranslucentAttributeBecomesHost() {
        try (ActivityScenario<NoAttrThemeActivity> scenario = ActivityScenario.launch(NoAttrThemeActivity.class)) {
            scenario.onActivity(activity -> {
                assertFalse(translucentAttr(activity));
                assertTrue(PushwooshPlatform.isHostCandidate(activity));
                assertSame(activity, host());
            });
            assertTrue("a theme with no isLightTheme must fall back to light", schemeIsLight());
        }
    }

    @Test
    public void dialogThemedActivityDoesNotTakeOverHost() {
        try (ActivityScenario<OpaqueLightActivity> scenario = ActivityScenario.launch(OpaqueLightActivity.class)) {
            AtomicReference<Activity> light = new AtomicReference<>();
            scenario.onActivity(activity -> {
                light.set(activity);
                startOnTop(activity, DialogDarkActivity.class);
            });
            Activity dialog = waitForResumed(DialogDarkActivity.class);

            assertFalse("a floating dialog activity is not the app's window", isLight(dialog));
            assertFalse(PushwooshPlatform.isHostCandidate(dialog));
            assertSame("host must stay on the activity underneath", light.get(), host());
            assertTrue(schemeIsLight());
        }
    }

    @Test
    public void floatingActivityDoesNotTakeOverHost() {
        try (ActivityScenario<OpaqueDarkActivity> scenario = ActivityScenario.launch(OpaqueDarkActivity.class)) {
            AtomicReference<Activity> dark = new AtomicReference<>();
            scenario.onActivity(activity -> {
                dark.set(activity);
                startOnTop(activity, FloatingLightActivity.class);
            });
            Activity floating = waitForResumed(FloatingLightActivity.class);

            assertTrue("probe must really be light", isLight(floating));
            assertFalse(PushwooshPlatform.isHostCandidate(floating));
            assertSame(dark.get(), host());
            assertFalse("a floating light window must not lighten the scheme", schemeIsLight());
        }
    }

    @Test
    public void hostRevertsWhenTopActivityFinishes() {
        try (ActivityScenario<OpaqueLightActivity> scenario = ActivityScenario.launch(OpaqueLightActivity.class)) {
            AtomicReference<Activity> light = new AtomicReference<>();
            scenario.onActivity(activity -> {
                light.set(activity);
                startOnTop(activity, OpaqueDarkActivity.class);
            });
            Activity dark = waitForResumed(OpaqueDarkActivity.class);
            assertSame(dark, host());
            assertFalse(schemeIsLight());

            InstrumentationRegistry.getInstrumentation().runOnMainSync(dark::finish);
            waitForResumed(OpaqueLightActivity.class);

            assertSame("host must fall back to the activity underneath", light.get(), host());
            assertTrue(schemeIsLight());
        }
    }

    @Test
    public void hostSurvivesRecreate() {
        try (ActivityScenario<OpaqueDarkActivity> scenario = ActivityScenario.launch(OpaqueDarkActivity.class)) {
            scenario.recreate();
            scenario.onActivity(activity -> assertSame(activity, host()));
            assertNotNull(host());
            assertFalse(schemeIsLight());
        }
    }

    @Test
    public void hostIsClearedWhenActivityIsDestroyed() {
        ActivityScenario<OpaqueDarkActivity> scenario = ActivityScenario.launch(OpaqueDarkActivity.class);
        scenario.onActivity(activity -> assertSame(activity, host()));
        scenario.close();

        assertNull("a destroyed host must not linger", host());
        // No host left: the scheme comes from the manifest application theme and must not crash.
        RichMediaColorSchemeResolver.wrapForCurrentScheme(appContext());
    }

    @Test
    public void modalPathPrefersItsOwnOpaqueActivity() {
        try (ActivityScenario<OpaqueLightActivity> lightScenario = ActivityScenario.launch(OpaqueLightActivity.class)) {
            AtomicReference<Activity> light = new AtomicReference<>();
            lightScenario.onActivity(light::set);
            assertSame(light.get(), host());

            try (ActivityScenario<OpaqueDarkActivity> darkScenario =
                    ActivityScenario.launch(OpaqueDarkActivity.class)) {
                darkScenario.onActivity(dark -> assertFalse(
                        "modal must report the theme of the activity it is shown in",
                        isLight(RichMediaColorSchemeResolver.wrapForCurrentScheme(dark))));
            }
        }
    }

    @Test
    public void modalPathFallsBackToHostWhenItsContextIsTranslucent() {
        try (ActivityScenario<OpaqueDarkActivity> scenario = ActivityScenario.launch(OpaqueDarkActivity.class)) {
            scenario.onActivity(activity -> startOnTop(activity, TranslucentLightActivity.class));
            Activity translucentLight = waitForResumed(TranslucentLightActivity.class);

            assertFalse(
                    "a translucent context must not be used as the scheme source",
                    isLight(RichMediaColorSchemeResolver.wrapForCurrentScheme(translucentLight)));
        }
    }
}
