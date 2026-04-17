package com.pushwoosh.appevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.Window;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class IdleDetectorTest {

    private static final int IDLE_TIMEOUT_SECONDS = 30;

    private List<IdleEvent> firedEvents;
    private IdleDetector idleDetector;
    private ActivityController<Activity> activityController;

    @Before
    public void setUp() {
        firedEvents = new ArrayList<>();
        idleDetector = new IdleDetector(IDLE_TIMEOUT_SECONDS, (activityName, idleSeconds) -> {
            firedEvents.add(new IdleEvent(activityName, idleSeconds));
        });
        activityController = Robolectric.buildActivity(Activity.class);
        activityController.create().start().resume();
    }

    @Test
    public void testTimerFiresAfterTimeout() {
        Activity activity = activityController.get();
        idleDetector.onActivityResumed(activity);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, firedEvents.size());
        assertEquals(IDLE_TIMEOUT_SECONDS, firedEvents.get(0).idleSeconds);
        assertEquals("Activity", firedEvents.get(0).activityName);
    }

    @Test
    public void testTouchResetsTimer() {
        Activity activity = activityController.get();
        idleDetector.onActivityResumed(activity);

        simulateTouch();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, firedEvents.size());
    }

    @Test
    public void testOnActivityPausedStopsTimer() {
        Activity activity = activityController.get();
        idleDetector.onActivityResumed(activity);

        idleDetector.onActivityPaused();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertTrue(firedEvents.isEmpty());
    }

    @Test
    public void testReWrapOnNewActivityResumed() {
        Activity activity1 = activityController.get();
        idleDetector.onActivityResumed(activity1);
        idleDetector.onActivityPaused();

        ActivityController<Activity> controller2 = Robolectric.buildActivity(Activity.class);
        controller2.create().start().resume();
        Activity activity2 = controller2.get();
        idleDetector.onActivityResumed(activity2);

        Window.Callback callback = activity2.getWindow().getCallback();
        assertTrue(callback instanceof IdleDetector.IdleWindowCallback);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertEquals(1, firedEvents.size());
    }

    @Test
    public void testOneShotPerSession() {
        Activity activity = activityController.get();
        idleDetector.onActivityResumed(activity);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertEquals(1, firedEvents.size());

        simulateTouch();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, firedEvents.size());
    }

    @Test
    public void testOneShotResetsOnBackground() {
        Activity activity = activityController.get();
        idleDetector.onActivityResumed(activity);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertEquals(1, firedEvents.size());

        idleDetector.onAppBackgrounded();
        idleDetector.onActivityResumed(activity);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertEquals(2, firedEvents.size());
    }

    @Test
    public void testWindowCallbackInstalled() {
        Activity activity = activityController.get();
        Window.Callback originalCallback = activity.getWindow().getCallback();
        assertFalse(originalCallback instanceof IdleDetector.IdleWindowCallback);

        idleDetector.onActivityResumed(activity);

        Window.Callback wrappedCallback = activity.getWindow().getCallback();
        assertTrue(wrappedCallback instanceof IdleDetector.IdleWindowCallback);
    }

    @Test
    public void testWindowCallbackSurvivesPause() {
        Activity activity = activityController.get();
        idleDetector.onActivityResumed(activity);
        idleDetector.onActivityPaused();

        Window.Callback callback = activity.getWindow().getCallback();
        assertTrue(callback instanceof IdleDetector.IdleWindowCallback);
    }

    @Test
    public void testNoDoubleWrap() {
        Activity activity = activityController.get();
        idleDetector.onActivityResumed(activity);
        Window.Callback firstWrap = activity.getWindow().getCallback();

        idleDetector.onActivityResumed(activity);
        Window.Callback secondWrap = activity.getWindow().getCallback();

        assertEquals(firstWrap, secondWrap);
    }

    @Test
    public void testNoDoubleWrapAfterExternalWrapper() {
        Activity activity = activityController.get();
        idleDetector.onActivityResumed(activity);
        Window.Callback ourWrap = activity.getWindow().getCallback();

        // Simulate another SDK wrapping on top of us
        Window.Callback externalWrap = activity.getWindow().getCallback();
        activity.getWindow().setCallback(new FakeExternalCallback(externalWrap));

        // Pause and resume — should NOT double-wrap thanks to wrappedWindows set
        idleDetector.onActivityPaused();
        idleDetector.onActivityResumed(activity);

        // Our wrapper should still be the same instance inside the chain
        Window.Callback top = activity.getWindow().getCallback();
        assertTrue(top instanceof FakeExternalCallback);
    }

    @Test
    public void testMinimumTimeout() {
        IdleDetector shortDetector = new IdleDetector(1, (activityName, idleSeconds) -> {
            firedEvents.add(new IdleEvent(activityName, idleSeconds));
        });

        Activity activity = activityController.get();
        shortDetector.onActivityResumed(activity);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, firedEvents.size());
        assertEquals(30, firedEvents.get(0).idleSeconds);
    }

    private void simulateTouch() {
        idleDetector.onTouchDetected();
    }

    /** Simulates another SDK's Window.Callback wrapper (e.g. Sentry, Embrace) */
    private static class FakeExternalCallback implements Window.Callback {
        private final Window.Callback delegate;

        FakeExternalCallback(Window.Callback delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean dispatchKeyEvent(android.view.KeyEvent event) {
            return delegate != null && delegate.dispatchKeyEvent(event);
        }

        @Override
        public boolean dispatchKeyShortcutEvent(android.view.KeyEvent event) {
            return delegate != null && delegate.dispatchKeyShortcutEvent(event);
        }

        @Override
        public boolean dispatchTouchEvent(android.view.MotionEvent event) {
            return delegate != null && delegate.dispatchTouchEvent(event);
        }

        @Override
        public boolean dispatchTrackballEvent(android.view.MotionEvent event) {
            return delegate != null && delegate.dispatchTrackballEvent(event);
        }

        @Override
        public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
            return delegate != null && delegate.dispatchGenericMotionEvent(event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {
            return delegate != null && delegate.dispatchPopulateAccessibilityEvent(event);
        }

        @Override
        public android.view.View onCreatePanelView(int featureId) {
            return delegate != null ? delegate.onCreatePanelView(featureId) : null;
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, android.view.Menu menu) {
            return delegate != null && delegate.onCreatePanelMenu(featureId, menu);
        }

        @Override
        public boolean onPreparePanel(int featureId, android.view.View view, android.view.Menu menu) {
            return delegate != null && delegate.onPreparePanel(featureId, view, menu);
        }

        @Override
        public boolean onMenuOpened(int featureId, android.view.Menu menu) {
            return delegate != null && delegate.onMenuOpened(featureId, menu);
        }

        @Override
        public boolean onMenuItemSelected(int featureId, android.view.MenuItem item) {
            return delegate != null && delegate.onMenuItemSelected(featureId, item);
        }

        @Override
        public void onWindowAttributesChanged(android.view.WindowManager.LayoutParams attrs) {
            if (delegate != null) delegate.onWindowAttributesChanged(attrs);
        }

        @Override
        public void onContentChanged() {
            if (delegate != null) delegate.onContentChanged();
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            if (delegate != null) delegate.onWindowFocusChanged(hasFocus);
        }

        @Override
        public void onAttachedToWindow() {
            if (delegate != null) delegate.onAttachedToWindow();
        }

        @Override
        public void onDetachedFromWindow() {
            if (delegate != null) delegate.onDetachedFromWindow();
        }

        @Override
        public void onPanelClosed(int featureId, android.view.Menu menu) {
            if (delegate != null) delegate.onPanelClosed(featureId, menu);
        }

        @Override
        public boolean onSearchRequested() {
            return delegate != null && delegate.onSearchRequested();
        }

        @Override
        public boolean onSearchRequested(android.view.SearchEvent searchEvent) {
            return delegate != null && delegate.onSearchRequested(searchEvent);
        }

        @Override
        public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback) {
            return delegate != null ? delegate.onWindowStartingActionMode(callback) : null;
        }

        @Override
        public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback, int type) {
            return delegate != null ? delegate.onWindowStartingActionMode(callback, type) : null;
        }

        @Override
        public void onActionModeStarted(android.view.ActionMode mode) {
            if (delegate != null) delegate.onActionModeStarted(mode);
        }

        @Override
        public void onActionModeFinished(android.view.ActionMode mode) {
            if (delegate != null) delegate.onActionModeFinished(mode);
        }
    }

    private static class IdleEvent {
        final String activityName;
        final int idleSeconds;

        IdleEvent(String activityName, int idleSeconds) {
            this.activityName = activityName;
            this.idleSeconds = idleSeconds;
        }
    }
}
