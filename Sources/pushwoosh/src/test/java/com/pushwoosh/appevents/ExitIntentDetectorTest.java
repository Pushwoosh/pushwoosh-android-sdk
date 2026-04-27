package com.pushwoosh.appevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class ExitIntentDetectorTest {

    private static final int EXIT_INTENT_TIMEOUT_SECONDS = 15;

    private List<ExitIntentEvent> firedEvents;
    private ExitIntentDetector detector;

    @Before
    public void setUp() {
        firedEvents = new ArrayList<>();
        detector = new ExitIntentDetector(
                EXIT_INTENT_TIMEOUT_SECONDS,
                (screenName, sessionDurationSeconds, exitIntentTimeoutSeconds) -> firedEvents.add(
                        new ExitIntentEvent(screenName, sessionDurationSeconds, exitIntentTimeoutSeconds)));
    }

    @Test
    public void testFiresAfterTimeoutWithScreenName() {
        detector.onAppBackgrounded("MainActivity/ProfileFragment", 42L);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, firedEvents.size());
        assertEquals("MainActivity/ProfileFragment", firedEvents.get(0).screenName);
        assertEquals(42L, firedEvents.get(0).sessionDurationSeconds);
        assertEquals(EXIT_INTENT_TIMEOUT_SECONDS, firedEvents.get(0).exitIntentTimeoutSeconds);
    }

    @Test
    public void testFiresWithNullScreenName() {
        detector.onAppBackgrounded(null, 3L);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, firedEvents.size());
        assertNull(firedEvents.get(0).screenName);
        assertEquals(3L, firedEvents.get(0).sessionDurationSeconds);
    }

    @Test
    public void testCancelledByForegrounded() {
        detector.onAppBackgrounded("MainActivity", 42L);
        detector.onAppForegrounded();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertTrue(firedEvents.isEmpty());
    }

    @Test
    public void testSecondBackgroundedOverridesSnapshot() {
        detector.onAppBackgrounded("MainActivity", 10L);
        detector.onAppBackgrounded("CheckoutActivity", 20L);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, firedEvents.size());
        assertEquals("CheckoutActivity", firedEvents.get(0).screenName);
        assertEquals(20L, firedEvents.get(0).sessionDurationSeconds);
    }

    @Test
    public void testClampBelowMin() {
        ExitIntentDetector shortDetector = new ExitIntentDetector(
                5,
                (screenName, sessionDurationSeconds, exitIntentTimeoutSeconds) -> firedEvents.add(
                        new ExitIntentEvent(screenName, sessionDurationSeconds, exitIntentTimeoutSeconds)));

        shortDetector.onAppBackgrounded("MainActivity", 1L);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, firedEvents.size());
        assertEquals(ExitIntentDetector.MIN_EXIT_INTENT_TIMEOUT_SECONDS, firedEvents.get(0).exitIntentTimeoutSeconds);
    }

    @Test
    public void testClampAboveMax() {
        ExitIntentDetector longDetector = new ExitIntentDetector(
                60,
                (screenName, sessionDurationSeconds, exitIntentTimeoutSeconds) -> firedEvents.add(
                        new ExitIntentEvent(screenName, sessionDurationSeconds, exitIntentTimeoutSeconds)));

        longDetector.onAppBackgrounded("MainActivity", 1L);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, firedEvents.size());
        assertEquals(ExitIntentDetector.MAX_EXIT_INTENT_TIMEOUT_SECONDS, firedEvents.get(0).exitIntentTimeoutSeconds);
    }

    @Test
    public void testInRangeTimeoutPreserved() {
        ExitIntentDetector midDetector = new ExitIntentDetector(
                20,
                (screenName, sessionDurationSeconds, exitIntentTimeoutSeconds) -> firedEvents.add(
                        new ExitIntentEvent(screenName, sessionDurationSeconds, exitIntentTimeoutSeconds)));

        midDetector.onAppBackgrounded("MainActivity", 1L);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, firedEvents.size());
        assertEquals(20, firedEvents.get(0).exitIntentTimeoutSeconds);
    }

    private static class ExitIntentEvent {
        final String screenName;
        final long sessionDurationSeconds;
        final int exitIntentTimeoutSeconds;

        ExitIntentEvent(String screenName, long sessionDurationSeconds, int exitIntentTimeoutSeconds) {
            this.screenName = screenName;
            this.sessionDurationSeconds = sessionDurationSeconds;
            this.exitIntentTimeoutSeconds = exitIntentTimeoutSeconds;
        }
    }
}
