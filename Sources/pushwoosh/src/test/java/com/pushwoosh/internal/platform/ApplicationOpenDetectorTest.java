package com.pushwoosh.internal.platform;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;

import com.pushwoosh.internal.event.EventBus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class ApplicationOpenDetectorTest {

    private Application application;
    private Activity activity;

    private List<ApplicationOpenDetector.ApplicationOpenEvent> openEvents;
    private List<ApplicationOpenDetector.ApplicationMovedToForegroundEvent> foregroundEvents;
    private List<ApplicationOpenDetector.ApplicationMovedToBackgroundEvent> backgroundEvents;

    private Application.ActivityLifecycleCallbacks callbacks;

    @Before
    public void setUp() {
        application = mock(Application.class);
        when(application.getApplicationContext()).thenReturn(application);
        activity = mock(Activity.class);

        openEvents = new ArrayList<>();
        foregroundEvents = new ArrayList<>();
        backgroundEvents = new ArrayList<>();

        EventBus.clearSubscribersMap();
        EventBus.subscribe(ApplicationOpenDetector.ApplicationOpenEvent.class, event -> openEvents.add(event));
        EventBus.subscribe(
                ApplicationOpenDetector.ApplicationMovedToForegroundEvent.class, event -> foregroundEvents.add(event));
        EventBus.subscribe(
                ApplicationOpenDetector.ApplicationMovedToBackgroundEvent.class, event -> backgroundEvents.add(event));
    }

    @After
    public void tearDown() {
        EventBus.clearSubscribersMap();
    }

    private Application.ActivityLifecycleCallbacks createDetectorAndCaptureCallbacks(boolean isFirstLaunch) {
        ApplicationOpenDetector detector = new ApplicationOpenDetector(application);
        detector.onApplicationCreated(isFirstLaunch);
        ArgumentCaptor<Application.ActivityLifecycleCallbacks> captor =
                ArgumentCaptor.forClass(Application.ActivityLifecycleCallbacks.class);
        verify(application).registerActivityLifecycleCallbacks(captor.capture());
        return captor.getValue();
    }

    // Pumps EventBus dispatch (posted via BackgroundExecutor.main with delay=0) without firing
    // the 700ms debounce runnables.
    private void flushImmediate() {
        ShadowLooper.idleMainLooper();
    }

    // Advances main looper past the debounce window and runs every pending task.
    private void flushPastDebounce() {
        ShadowLooper.idleMainLooper(ApplicationOpenDetector.TIMEOUT_MS + 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testColdStartSendsOpenAndForeground() {
        callbacks = createDetectorAndCaptureCallbacks(false);

        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);

        flushImmediate();

        assertEquals(1, openEvents.size());
        assertEquals(1, foregroundEvents.size());
        assertEquals(0, backgroundEvents.size());
    }

    @Test
    public void testRotationDoesNotEmitBackgroundOrSecondOpen() {
        callbacks = createDetectorAndCaptureCallbacks(false);
        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);
        flushImmediate();

        // Rotation: Stopped → Destroyed → Created → Started, all within debounce window.
        callbacks.onActivityStopped(activity);
        callbacks.onActivityDestroyed(activity);
        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);

        flushPastDebounce();

        assertEquals("rotation must not multiply OpenEvent", 1, openEvents.size());
        assertEquals("rotation must not multiply ForegroundEvent", 1, foregroundEvents.size());
        assertEquals("rotation must not emit BackgroundEvent", 0, backgroundEvents.size());
    }

    @Test
    public void testRealBackgroundEmitsBackgroundAfterDebounce() {
        callbacks = createDetectorAndCaptureCallbacks(false);
        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);
        flushImmediate();

        callbacks.onActivityStopped(activity);

        // No onActivityStarted within the debounce window.
        flushPastDebounce();

        assertEquals(1, backgroundEvents.size());
    }

    @Test
    public void testReturnFromBackgroundEmitsForegroundImmediately() {
        callbacks = createDetectorAndCaptureCallbacks(false);
        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);
        flushImmediate();

        callbacks.onActivityStopped(activity);
        callbacks.onActivityDestroyed(activity);
        flushPastDebounce();
        assertEquals(1, backgroundEvents.size());

        // User returns — fresh session, Open and Foreground emit again.
        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);
        flushImmediate();

        assertEquals(2, openEvents.size());
        assertEquals(2, foregroundEvents.size());
    }

    @Test
    public void testSixRotationsDoNotTriggerKnockPattern() {
        callbacks = createDetectorAndCaptureCallbacks(false);
        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);
        flushImmediate();

        for (int i = 0; i < 6; i++) {
            callbacks.onActivityStopped(activity);
            callbacks.onActivityDestroyed(activity);
            callbacks.onActivityCreated(activity, null);
            callbacks.onActivityStarted(activity);
        }

        flushPastDebounce();

        assertEquals("6 rotations must not multiply OpenEvent", 1, openEvents.size());
        assertEquals("6 rotations must not multiply ForegroundEvent", 1, foregroundEvents.size());
        assertEquals("6 rotations must not emit BackgroundEvent", 0, backgroundEvents.size());
    }

    @Test
    public void testFirstLaunchEmitsOpenImmediatelyAndSuppressesDuplicate() {
        callbacks = createDetectorAndCaptureCallbacks(true);
        flushImmediate();

        assertEquals("OpenEvent emitted on isFirstLaunch=true", 1, openEvents.size());

        // First Activity arrives within firstLaunchOpenTimeout (well below 1 min in test clock).
        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);
        flushImmediate();

        assertEquals("no duplicate OpenEvent within firstLaunch timeout", 1, openEvents.size());
        assertEquals(1, foregroundEvents.size());

        // Rotation after firstLaunch path must not duplicate OpenEvent either.
        callbacks.onActivityStopped(activity);
        callbacks.onActivityDestroyed(activity);
        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);
        flushPastDebounce();

        assertEquals("rotation after firstLaunch must not duplicate Open", 1, openEvents.size());
        assertEquals(1, foregroundEvents.size());
        assertEquals(0, backgroundEvents.size());
    }

    @Test
    public void testStartedWithinDebounceWindowCancelsBackground() {
        callbacks = createDetectorAndCaptureCallbacks(false);
        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);
        flushImmediate();

        // T=0: Stopped schedules runnable A at T=TIMEOUT_MS (700).
        callbacks.onActivityStopped(activity);

        // T=600: A still pending.
        ShadowLooper.idleMainLooper(ApplicationOpenDetector.TIMEOUT_MS - 100, TimeUnit.MILLISECONDS);
        assertEquals("background must not fire before TIMEOUT_MS", 0, backgroundEvents.size());

        // T=600: Started cancels A. Immediately Stop again to schedule runnable B at T=1300.
        // The second Stop is what makes this test prove cancellation: without it, the count==0
        // guard inside goBackgroundRunnable would suppress A on its own (count would be 1
        // after Started) and the test could not distinguish "A was removed" from
        // "A fired but guard rejected it".
        callbacks.onActivityStarted(activity);
        callbacks.onActivityStopped(activity);

        // T=800: past A's original deadline, well before B's. If removeCallbacks failed,
        // orphan A would fire here (count==0 && inForeground==true after Started).
        ShadowLooper.idleMainLooper(200, TimeUnit.MILLISECONDS);
        assertEquals("orphan runnable from cancelled debounce must not fire", 0, backgroundEvents.size());

        // T=1500: past B's deadline — exactly one Background event from B.
        ShadowLooper.idleMainLooper(ApplicationOpenDetector.TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals("fresh Stopped after cancel still schedules its own background", 1, backgroundEvents.size());
    }

    @Test
    public void testBackgroundFiresAfterDebounceWindowElapses() {
        callbacks = createDetectorAndCaptureCallbacks(false);
        callbacks.onActivityCreated(activity, null);
        callbacks.onActivityStarted(activity);
        flushImmediate();

        callbacks.onActivityStopped(activity);

        // Advance just past TIMEOUT_MS — Background event must fire.
        ShadowLooper.idleMainLooper(ApplicationOpenDetector.TIMEOUT_MS + 100, TimeUnit.MILLISECONDS);

        assertEquals(1, backgroundEvents.size());
    }
}
