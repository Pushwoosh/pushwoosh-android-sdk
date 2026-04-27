package com.pushwoosh.appevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.os.Looper;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.utils.Config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class PushwooshAppLifecycleCallbacksTest {

    private List<String> emittedEvents;
    private PushwooshAppLifecycleCallbacks callbacks;
    private Activity activity;
    private PushwooshPlatform pushwooshPlatform;
    private Config config;

    @Before
    public void setUp() {
        emittedEvents = new ArrayList<>();
        callbacks = new PushwooshAppLifecycleCallbacks(
                (eventName, activityName) -> emittedEvents.add(eventName), 0, null, 0, null);

        pushwooshPlatform = mock(PushwooshPlatform.class);
        config = mock(Config.class);
        when(pushwooshPlatform.getConfig()).thenReturn(config);
        when(config.isCollectingLifecycleEventsAllowed()).thenReturn(true);
        when(pushwooshPlatform.getTopActivity()).thenReturn(null);

        activity = Robolectric.buildActivity(Activity.class).get();
    }

    /** Reflection bridge for notifyScreenOpened() — imitates onFragmentStarted from support listener. */
    private void triggerFragmentStarted() throws Exception {
        Method method = PushwooshAppLifecycleCallbacks.class.getDeclaredMethod("notifyScreenOpened");
        method.setAccessible(true);
        method.invoke(callbacks);
    }

    private void idle(long millis) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis));
    }

    private MockedStatic<PushwooshPlatform> stubPlatform() {
        MockedStatic<PushwooshPlatform> mocked = mockStatic(PushwooshPlatform.class);
        mocked.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);
        return mocked;
    }

    @Test
    public void freshLaunchEmitsApplicationOpenAndScreenOpen() throws Exception {
        try (MockedStatic<PushwooshPlatform> ignored = stubPlatform()) {
            callbacks.onActivityCreated(activity, null);
            callbacks.onActivityStarted(activity);
            triggerFragmentStarted();

            idle(200);

            assertEquals(
                    Arrays.asList(
                            PushwooshAppLifecycleCallbacks.APPLICATION_OPENED_EVENT,
                            PushwooshAppLifecycleCallbacks.SCREEN_OPENED_EVENT),
                    emittedEvents);
        }
    }

    @Test
    public void rapidRestoreSuppressesAllThreeEvents() throws Exception {
        try (MockedStatic<PushwooshPlatform> ignored = stubPlatform()) {
            callbacks.onActivityCreated(activity, null);
            callbacks.onActivityStarted(activity);
            triggerFragmentStarted();
            idle(200);
            emittedEvents.clear();

            callbacks.onActivityStopped(activity);
            idle(500);

            callbacks.onActivityStarted(activity);
            triggerFragmentStarted();

            idle(600);

            assertTrue(
                    "No events should be emitted on rapid-restore, but got: " + emittedEvents, emittedEvents.isEmpty());
        }
    }

    @Test
    public void slowRestoreEmitsMinimizedThenOpenAndScreen() throws Exception {
        try (MockedStatic<PushwooshPlatform> ignored = stubPlatform()) {
            callbacks.onActivityCreated(activity, null);
            callbacks.onActivityStarted(activity);
            triggerFragmentStarted();
            idle(200);
            emittedEvents.clear();

            callbacks.onActivityStopped(activity);
            idle(1100);

            assertEquals(Arrays.asList(PushwooshAppLifecycleCallbacks.APPLICATION_CLOSED_EVENT), emittedEvents);

            callbacks.onActivityStarted(activity);
            triggerFragmentStarted();
            idle(200);

            assertEquals(
                    Arrays.asList(
                            PushwooshAppLifecycleCallbacks.APPLICATION_CLOSED_EVENT,
                            PushwooshAppLifecycleCallbacks.APPLICATION_OPENED_EVENT,
                            PushwooshAppLifecycleCallbacks.SCREEN_OPENED_EVENT),
                    emittedEvents);
        }
    }

    @Test
    public void inAppNavigationEmitsScreenOpen() throws Exception {
        try (MockedStatic<PushwooshPlatform> ignored = stubPlatform()) {
            callbacks.onActivityCreated(activity, null);
            callbacks.onActivityStarted(activity);
            triggerFragmentStarted();
            idle(200);
            emittedEvents.clear();

            triggerFragmentStarted();
            idle(200);

            assertEquals(Arrays.asList(PushwooshAppLifecycleCallbacks.SCREEN_OPENED_EVENT), emittedEvents);
        }
    }
}
