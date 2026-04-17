package com.pushwoosh.appevents;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

class IdleDetector {

    private static final String TAG = "IdleDetector";
    static final int MIN_IDLE_TIMEOUT_SECONDS = 30;
    private static final double KEYBOARD_HEIGHT_THRESHOLD = 0.15;

    private final int idleTimeoutMs;
    private final IdleCallback callback;
    private final Handler handler;
    private final Runnable idleRunnable;

    private final Set<Window> wrappedWindows = Collections.newSetFromMap(new WeakHashMap<>());

    @Nullable private Activity currentActivity;

    @Nullable private ViewTreeObserver.OnGlobalLayoutListener layoutListener;

    private boolean idleFired;
    private boolean keyboardVisible;
    private boolean windowFocused = true;

    IdleDetector(int idleTimeoutSeconds, @NonNull IdleCallback callback) {
        if (idleTimeoutSeconds < MIN_IDLE_TIMEOUT_SECONDS) {
            PWLog.warn(
                    TAG,
                    "Idle timeout " + idleTimeoutSeconds + "s is below minimum (" + MIN_IDLE_TIMEOUT_SECONDS
                            + "s). Using " + MIN_IDLE_TIMEOUT_SECONDS + "s.");
        }
        int effectiveTimeout = Math.max(idleTimeoutSeconds, MIN_IDLE_TIMEOUT_SECONDS);
        this.idleTimeoutMs = effectiveTimeout * 1000;
        this.callback = callback;
        this.handler = new Handler(Looper.getMainLooper());
        this.idleRunnable = this::onIdleTimeout;
    }

    // Activity became visible and interactive — install wrapper, start/resume idle timer
    void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
        keyboardVisible = false;
        windowFocused = true;
        installWindowCallbackWrapper(activity);
        installKeyboardListener(activity);
        if (!idleFired) {
            restartTimer();
        }
    }

    // Activity losing foreground (another activity on top, or going to background) — stop timer, clean up
    void onActivityPaused() {
        stopTimer();
        uninstallKeyboardListener();
        currentActivity = null;
    }

    // App went to background (all activities stopped) — full reset, next foreground = new idle session
    void onAppBackgrounded() {
        stopTimer();
        uninstallKeyboardListener();
        currentActivity = null;
        idleFired = false;
        keyboardVisible = false;
    }

    private void installWindowCallbackWrapper(@NonNull Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        Window.Callback current = window.getCallback();
        if (current instanceof IdleWindowCallback) {
            return;
        }
        if (wrappedWindows.contains(window)) {
            return;
        }
        window.setCallback(new IdleWindowCallback(current));
        wrappedWindows.add(window);
        PWLog.debug(TAG, "Window.Callback wrapper installed");
    }

    private void stopTimer() {
        handler.removeCallbacks(idleRunnable);
    }

    private void restartTimer() {
        handler.removeCallbacks(idleRunnable);
        handler.postDelayed(idleRunnable, idleTimeoutMs);
    }

    void onTouchDetected() {
        if (idleFired || keyboardVisible || !windowFocused || currentActivity == null) {
            return;
        }
        restartTimer();
    }

    private void onIdleTimeout() {
        if (idleFired || currentActivity == null) {
            return;
        }
        idleFired = true;
        String activityName = currentActivity.getClass().getSimpleName();
        int idleSeconds = idleTimeoutMs / 1000;
        PWLog.debug(TAG, "User idle detected on " + activityName + " after " + idleSeconds + "s");
        callback.onIdle(activityName, idleSeconds);
    }

    private void installKeyboardListener(@NonNull Activity activity) {
        uninstallKeyboardListener();
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        View decorView = window.getDecorView();
        layoutListener = () -> {
            boolean visible = isKeyboardVisible(decorView);
            if (visible && !keyboardVisible) {
                keyboardVisible = true;
                stopTimer();
                PWLog.debug(TAG, "Keyboard visible, idle timer stopped");
            } else if (!visible && keyboardVisible) {
                keyboardVisible = false;
                if (!idleFired) {
                    restartTimer();
                    PWLog.debug(TAG, "Keyboard hidden, idle timer restarted");
                }
            }
        };
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }

    private void uninstallKeyboardListener() {
        if (layoutListener != null && currentActivity != null) {
            Window window = currentActivity.getWindow();
            if (window == null) {
                layoutListener = null;
                return;
            }
            View decorView = window.getDecorView();
            ViewTreeObserver vto = decorView.getViewTreeObserver();
            if (vto.isAlive()) {
                vto.removeOnGlobalLayoutListener(layoutListener);
            }
        }
        layoutListener = null;
    }

    private boolean isKeyboardVisible(@NonNull View decorView) {
        Rect visibleFrame = new Rect();
        decorView.getWindowVisibleDisplayFrame(visibleFrame);
        int heightDiff = decorView.getHeight() - visibleFrame.height();
        return heightDiff > decorView.getHeight() * KEYBOARD_HEIGHT_THRESHOLD;
    }

    private void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && !windowFocused) {
            windowFocused = true;
            if (!idleFired && !keyboardVisible) {
                restartTimer();
                PWLog.debug(TAG, "Window focus gained, idle timer restarted");
            }
        } else if (!hasFocus && windowFocused) {
            windowFocused = false;
            stopTimer();
            PWLog.debug(TAG, "Window focus lost, idle timer stopped");
        }
    }

    interface IdleCallback {
        void onIdle(String activityName, int idleSeconds);
    }

    /**
     * Decorator over the Activity's {@link Window.Callback} that intercepts user input
     * to detect idle state. All methods delegate to the wrapped callback; the ones we care
     * about also notify {@link IdleDetector}.
     *
     * <p>Window.Callback is the single entry point for all input dispatched to an Activity's
     * window — touches, keys, trackball, motion (hover/joystick), accessibility, menus,
     * focus changes, and action modes. We must implement every method to avoid breaking
     * the host app's behaviour.</p>
     */
    class IdleWindowCallback implements Window.Callback {

        @Nullable private final Window.Callback wrapped;

        IdleWindowCallback(@Nullable Window.Callback wrapped) {
            this.wrapped = wrapped;
        }

        // --- Input dispatch: these are the methods we intercept for idle detection ---

        // Finger / stylus touches on screen — primary signal for idle reset
        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                onTouchDetected();
            }
            return wrapped != null && wrapped.dispatchTouchEvent(event);
        }

        // Hardware key presses: external/BT keyboard, gamepad buttons, volume, back, soft keyboard backspace/enter
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                onTouchDetected();
            }
            return wrapped != null && wrapped.dispatchKeyEvent(event);
        }

        // Ctrl/Alt/Meta + key combos (e.g. Ctrl+C) — subset of key events handled separately
        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            onTouchDetected();
            return wrapped != null && wrapped.dispatchKeyShortcutEvent(event);
        }

        // Trackball movement — rare, mostly legacy/accessibility devices
        @Override
        public boolean dispatchTrackballEvent(MotionEvent event) {
            onTouchDetected();
            return wrapped != null && wrapped.dispatchTrackballEvent(event);
        }

        // Non-touch pointer events: stylus hover, gamepad joysticks, mouse move
        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            onTouchDetected();
            return wrapped != null && wrapped.dispatchGenericMotionEvent(event);
        }

        // Accessibility services request content description for TalkBack etc.
        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            return wrapped != null && wrapped.dispatchPopulateAccessibilityEvent(event);
        }

        // --- Options menu / panel lifecycle ---

        // Custom view for the options panel (featureId = FEATURE_OPTIONS_PANEL etc.)
        @Nullable @Override
        public View onCreatePanelView(int featureId) {
            return wrapped != null ? wrapped.onCreatePanelView(featureId) : null;
        }

        // Inflate menu items into the panel
        @Override
        public boolean onCreatePanelMenu(int featureId, @NonNull android.view.Menu menu) {
            return wrapped != null && wrapped.onCreatePanelMenu(featureId, menu);
        }

        // Update menu items right before the panel is shown
        @Override
        public boolean onPreparePanel(int featureId, @Nullable View view, @NonNull android.view.Menu menu) {
            return wrapped != null && wrapped.onPreparePanel(featureId, view, menu);
        }

        // Options menu / action bar overflow opened
        @Override
        public boolean onMenuOpened(int featureId, @NonNull android.view.Menu menu) {
            return wrapped != null && wrapped.onMenuOpened(featureId, menu);
        }

        // User tapped a menu item
        @Override
        public boolean onMenuItemSelected(int featureId, @NonNull android.view.MenuItem item) {
            return wrapped != null && wrapped.onMenuItemSelected(featureId, item);
        }

        // Panel (menu) dismissed
        @Override
        public void onPanelClosed(int featureId, @NonNull android.view.Menu menu) {
            if (wrapped != null) {
                wrapped.onPanelClosed(featureId, menu);
            }
        }

        // --- Window state callbacks ---

        // LayoutParams changed (flags, softInputMode, dimming, etc.)
        @Override
        public void onWindowAttributesChanged(android.view.WindowManager.LayoutParams attrs) {
            if (wrapped != null) {
                wrapped.onWindowAttributesChanged(attrs);
            }
        }

        // Content view (setContentView) replaced or updated
        @Override
        public void onContentChanged() {
            if (wrapped != null) {
                wrapped.onContentChanged();
            }
        }

        // Window gained or lost focus — we stop idle timer on focus loss
        // (dialog, permission prompt, notification shade, modal in-app)
        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            IdleDetector.this.onWindowFocusChanged(hasFocus);
            if (wrapped != null) {
                wrapped.onWindowFocusChanged(hasFocus);
            }
        }

        // Window added to WindowManager
        @Override
        public void onAttachedToWindow() {
            if (wrapped != null) {
                wrapped.onAttachedToWindow();
            }
        }

        // Window removed from WindowManager
        @Override
        public void onDetachedFromWindow() {
            if (wrapped != null) {
                wrapped.onDetachedFromWindow();
            }
        }

        // --- Search & action mode ---

        // User triggered search (hardware search key or SearchView)
        @Override
        public boolean onSearchRequested() {
            return wrapped != null && wrapped.onSearchRequested();
        }

        // Search triggered with SearchEvent context (API 23+)
        @Override
        public boolean onSearchRequested(android.view.SearchEvent searchEvent) {
            return wrapped != null && wrapped.onSearchRequested(searchEvent);
        }

        // Long-press / text selection starts contextual action mode (copy/paste bar)
        @Nullable @Override
        public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback) {
            return wrapped != null ? wrapped.onWindowStartingActionMode(callback) : null;
        }

        // Same but with explicit type (TYPE_PRIMARY or TYPE_FLOATING, API 23+)
        @Nullable @Override
        public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback, int type) {
            return wrapped != null ? wrapped.onWindowStartingActionMode(callback, type) : null;
        }

        // Contextual action mode became visible
        @Override
        public void onActionModeStarted(android.view.ActionMode mode) {
            if (wrapped != null) {
                wrapped.onActionModeStarted(mode);
            }
        }

        // Contextual action mode dismissed
        @Override
        public void onActionModeFinished(android.view.ActionMode mode) {
            if (wrapped != null) {
                wrapped.onActionModeFinished(mode);
            }
        }
    }
}
