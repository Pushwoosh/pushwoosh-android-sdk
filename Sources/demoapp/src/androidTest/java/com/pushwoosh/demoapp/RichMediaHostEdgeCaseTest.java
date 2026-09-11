package com.pushwoosh.demoapp;

import static com.pushwoosh.demoapp.HostProbeSupport.appContext;
import static com.pushwoosh.demoapp.HostProbeSupport.host;
import static com.pushwoosh.demoapp.HostProbeSupport.isLight;
import static com.pushwoosh.demoapp.HostProbeSupport.schemeIsLight;
import static com.pushwoosh.demoapp.HostProbeSupport.sleep;
import static com.pushwoosh.demoapp.HostProbeSupport.waitForResumed;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.ContextThemeWrapper;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.demoapp.hostprobe.ConfigChangesActivity;
import com.pushwoosh.demoapp.hostprobe.DarkPaletteLightFlagActivity;
import com.pushwoosh.demoapp.hostprobe.FinishInOnCreateActivity;
import com.pushwoosh.demoapp.hostprobe.OpaqueDarkActivity;
import com.pushwoosh.demoapp.hostprobe.OpaqueLightActivity;
import com.pushwoosh.demoapp.hostprobe.OpaqueOverrideActivity;
import com.pushwoosh.demoapp.hostprobe.OwnTaskActivity;
import com.pushwoosh.demoapp.hostprobe.ReferenceFlagActivity;
import com.pushwoosh.demoapp.hostprobe.RemoteProcessActivity;
import com.pushwoosh.demoapp.hostprobe.TranslucentActivity;
import com.pushwoosh.richmedia.RichMediaColorSchemeResolver;

import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/** SDK-961: the awkward corners of the host rule — dirty themes, activity chains, processes. */
@RunWith(AndroidJUnit4.class)
public class RichMediaHostEdgeCaseTest {

    // --- dirty themes ---

    @Test
    public void translucentParentOverriddenToOpaqueBecomesHost() {
        try (ActivityScenario<OpaqueOverrideActivity> scenario =
                ActivityScenario.launch(OpaqueOverrideActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(
                        "the attribute value must win over the parent theme",
                        PushwooshPlatform.isHostCandidate(activity));
                assertSame(activity, host());
            });
        }
    }

    @Test
    public void darkPaletteWithLightFlagIsReportedLight() {
        try (ActivityScenario<DarkPaletteLightFlagActivity> scenario =
                ActivityScenario.launch(DarkPaletteLightFlagActivity.class)) {
            scenario.onActivity(activity -> assertSame(activity, host()));
            assertTrue("the resolver must trust isLightTheme, not the palette", schemeIsLight());
        }
    }

    @Test
    public void themeChangedAtRuntimeIsPickedUp() {
        try (ActivityScenario<OpaqueLightActivity> scenario = ActivityScenario.launch(OpaqueLightActivity.class)) {
            assertTrue(schemeIsLight());
            scenario.onActivity(activity -> activity.setTheme(android.R.style.Theme_Material));
            assertFalse("a runtime setTheme on the host must change the reported scheme", schemeIsLight());
        }
    }

    @Test
    public void wrappedActivityContextFallsBackToHost() {
        try (ActivityScenario<OpaqueLightActivity> scenario = ActivityScenario.launch(OpaqueLightActivity.class)) {
            scenario.onActivity(activity -> {
                Context wrapped = new ContextThemeWrapper(activity, android.R.style.Theme_Material);
                assertTrue(
                        "a wrapped activity context is not an Activity, so the host decides",
                        isLight(RichMediaColorSchemeResolver.wrapForCurrentScheme(wrapped)));
            });
        }
    }

    @Test
    public void isLightThemeBehindAReferenceIsResolved() {
        try (ActivityScenario<ReferenceFlagActivity> scenario = ActivityScenario.launch(ReferenceFlagActivity.class)) {
            scenario.onActivity(activity -> assertSame(activity, host()));
            assertFalse("a bool reference must be followed, not treated as light", schemeIsLight());
        }
    }

    /**
     * An activity that handles uiMode itself is never recreated on a system theme switch. Its theme
     * still tracks the new configuration, so the scheme follows the system without a restart.
     */
    @Test
    public void configChangesHostKeepsItsThemeAcrossSystemThemeSwitch() {
        String initial = uiModeNight();
        try (ActivityScenario<ConfigChangesActivity> scenario = ActivityScenario.launch(ConfigChangesActivity.class)) {
            scenario.onActivity(activity -> assertSame(activity, host()));
            boolean lightBefore = schemeIsLight();

            setUiModeNight(lightBefore ? "yes" : "no");
            sleep(3000);
            boolean lightAfter = schemeIsLight();
            AtomicReference<Boolean> recreated = new AtomicReference<>(Boolean.FALSE);
            scenario.onActivity(activity -> recreated.set(host() != activity));

            assertFalse("the probe must not have been recreated", recreated.get());
            assertTrue("a configChanges host must still follow the new configuration", lightBefore != lightAfter);
        } finally {
            setUiModeNight(initial);
            sleep(2000);
        }
    }

    private static String uiModeNight() {
        int mode = appContext().getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES ? "yes" : "no";
    }

    private static void setUiModeNight(String value) {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("cmd uimode night " + value);
    }

    /**
     * A plain dialog is a window inside its activity, not an activity of its own, so it cannot
     * become the host no matter which theme it carries.
     */
    @Test
    public void plainDarkDialogDoesNotAffectTheScheme() {
        try (ActivityScenario<OpaqueLightActivity> scenario = ActivityScenario.launch(OpaqueLightActivity.class)) {
            AtomicReference<Activity> light = new AtomicReference<>();
            scenario.onActivity(activity -> {
                light.set(activity);
                new android.app.AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog)
                        .setTitle("dark dialog")
                        .show();
            });
            sleep(1000);

            assertSame(light.get(), host());
            assertTrue("a dialog window must not change the reported scheme", schemeIsLight());
        }
    }

    // --- activity chains ---

    @Test
    public void hostUnwindsThroughAThreeActivityChain() {
        try (ActivityScenario<OpaqueLightActivity> scenario = ActivityScenario.launch(OpaqueLightActivity.class)) {
            AtomicReference<Activity> light = new AtomicReference<>();
            scenario.onActivity(activity -> {
                light.set(activity);
                activity.startActivity(new Intent(activity, OpaqueDarkActivity.class));
            });
            Activity dark = waitForResumed(OpaqueDarkActivity.class);
            assertSame(dark, host());

            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(() -> dark.startActivity(new Intent(dark, TranslucentActivity.class)));
            Activity translucent = waitForResumed(TranslucentActivity.class);
            assertSame("a translucent top must not take the host", dark, host());

            InstrumentationRegistry.getInstrumentation().runOnMainSync(translucent::finish);
            waitForResumed(OpaqueDarkActivity.class);
            assertSame(dark, host());
            assertFalse(schemeIsLight());

            InstrumentationRegistry.getInstrumentation().runOnMainSync(dark::finish);
            waitForResumed(OpaqueLightActivity.class);
            assertSame(light.get(), host());
            assertTrue(schemeIsLight());
        }
    }

    @Test
    public void activityFinishingInOnCreateLeavesAUsableHost() {
        try (ActivityScenario<OpaqueLightActivity> scenario = ActivityScenario.launch(OpaqueLightActivity.class)) {
            AtomicReference<Activity> light = new AtomicReference<>();
            scenario.onActivity(activity -> {
                light.set(activity);
                activity.startActivity(new Intent(activity, FinishInOnCreateActivity.class));
            });
            waitForResumed(OpaqueLightActivity.class);
            sleep(1000);

            assertNotNull("a self-finishing activity must not leave the host empty", host());
            assertSame(light.get(), host());
            assertTrue(schemeIsLight());
        }
    }

    @Test
    public void activityInItsOwnTaskBecomesHost() {
        try (ActivityScenario<OpaqueDarkActivity> scenario = ActivityScenario.launch(OpaqueDarkActivity.class)) {
            scenario.onActivity(activity -> {
                Intent intent = new Intent(activity, OwnTaskActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            });
            Activity ownTask = waitForResumed(OwnTaskActivity.class);

            assertSame("the visible activity wins regardless of its task", ownTask, host());
            assertTrue(schemeIsLight());
        }
    }

    // --- two visible activities ---

    /**
     * With two windows on screen the rule is ambiguous by definition: the host is whichever activity
     * started last, which need not be the one a Legacy rich media appears over.
     */
    @Test
    public void twoVisibleActivitiesLeaveHostOnTheLastStarted() {
        try (ActivityScenario<OpaqueDarkActivity> scenario = ActivityScenario.launch(OpaqueDarkActivity.class)) {
            AtomicReference<Activity> dark = new AtomicReference<>();
            scenario.onActivity(activity -> {
                dark.set(activity);
                android.app.ActivityOptions options = android.app.ActivityOptions.makeBasic();
                try {
                    android.app.ActivityOptions.class
                            .getMethod("setLaunchWindowingMode", int.class)
                            .invoke(options, 5); // WINDOWING_MODE_FREEFORM
                } catch (ReflectiveOperationException e) {
                    throw new AssumptionViolatedException("setLaunchWindowingMode unavailable", e);
                }
                Intent intent = new Intent(activity, OwnTaskActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent, options.toBundle());
            });
            Activity ownTask = waitForResumed(OwnTaskActivity.class);
            sleep(1500);

            int resumed = countResumedProbes();
            android.util.Log.i("HostProbe", "multiWindow resumedProbes=" + resumed);
            Assume.assumeTrue("stand does not keep two probe activities resumed", resumed >= 2);

            assertSame("the host is the last started window, whichever it is", ownTask, host());
            assertTrue(schemeIsLight());
        }
    }

    private static int countResumedProbes() {
        AtomicReference<Integer> count = new AtomicReference<>(0);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumed =
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            int probes = 0;
            for (Activity activity : resumed) {
                if (activity.getClass().getName().contains("hostprobe")) {
                    probes++;
                }
            }
            count.set(probes);
        });
        return count.get();
    }

    // --- separate process ---

    @Test
    public void activityInAnotherProcessDoesNotChangeThisProcessHost() {
        try (ActivityScenario<OpaqueLightActivity> scenario = ActivityScenario.launch(OpaqueLightActivity.class)) {
            AtomicReference<Activity> light = new AtomicReference<>();
            scenario.onActivity(light::set);
            assertSame(light.get(), host());

            Intent intent = new Intent(appContext(), RemoteProcessActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext().startActivity(intent);
            sleep(3000);

            assertSame("an activity from another process must not touch this process's host", light.get(), host());
            assertTrue(schemeIsLight());
        }
    }
}
