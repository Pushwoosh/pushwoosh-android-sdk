package com.pushwoosh.demoapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.ModalRichMediaWindow;
import com.pushwoosh.inapp.view.utils.ModalRichMediaWindowUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Regression guard for GitHub #193:
 * "PopupWindow$PopupDecorView ... not attached to window manager", brought up from a
 * ValueAnimator update callback inside the modal rich media slide animation.
 *
 * Before the fix, driving a slide animator frame while the popup's decor view was detached from
 * the WindowManager (but {@code mIsShowing} still true — the state the framework leaves the popup
 * in when the hosting activity is torn down underneath it) crashed with IllegalArgumentException
 * "not attached to window manager", N/N. This test recreates that exact state and asserts the
 * crash no longer happens: {@code ModalRichMediaWindowUtils.updateIfAttached(...)} skips
 * {@code PopupWindow.update()} once the content view is detached.
 *
 * This runs on a device because it exercises the real {@code WindowManagerGlobal.findViewLocked}
 * path that a JVM/Robolectric unit test mocks away — the unit suite verifies the guard logic, this
 * verifies the guard actually prevents the framework crash.
 *
 * Uses the REAL SDK classes: {@link ModalRichMediaWindow} (a PopupWindow) and the REAL animator
 * factory {@link ModalRichMediaWindowUtils#dismissWindowToRightAnimation}.
 */
@RunWith(AndroidJUnit4.class)
public class PopupWindowNotAttachedReproTest {

    private static ModalRichMediaWindow showWindow(MainActivity activity) {
        Resource resource = new Resource("r-REPRO-193", false);
        ModalRichMediaWindow window = new ModalRichMediaWindow(activity, resource);
        View parent = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        window.showAtLocation(parent, Gravity.CENTER, 0, 0);
        return window;
    }

    /**
     * Detach the PopupWindow's decor view from the WindowManager WITHOUT calling
     * {@code PopupWindow.dismiss()} — so {@code mIsShowing} stays true. This is exactly the
     * state the framework leaves the popup in when the hosting activity's window is torn down
     * underneath it (app sent to background / activity destroyed): the decor is gone from the
     * WindowManager, but the PopupWindow object still believes it is showing.
     *
     * Before the fix, a later {@code update()} sailed past its {@code isShowing()} guard into
     * {@code WindowManager.updateViewLayout(detachedDecor)} -> "not attached to window manager".
     * After the fix, {@code updateIfAttached} checks {@code getContentView().isAttachedToWindow()}
     * and skips the update, so no crash.
     */
    private static void detachDecorFromWindowManager(MainActivity activity, ModalRichMediaWindow window) {
        // Climb to the outermost view (the PopupDecorView actually added to the WindowManager),
        // not an intermediate PopupBackgroundView — removing the wrong node would itself throw
        // "not attached" and fake the result.
        View decor = window.getContentView();
        while (decor.getParent() instanceof View) {
            decor = (View) decor.getParent();
        }
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        wm.removeViewImmediate(decor);
    }

    private static void pumpOneAnimatorFrame(ValueAnimator animator) {
        animator.setDuration(1000);
        // setCurrentPlayTime synchronously runs animateValue() -> onAnimationUpdate listeners
        // on the calling (main) thread — the exact SDK lambda that calls window.update().
        animator.setCurrentPlayTime(500);
    }

    /** Window detached mid-animation -> update() must be skipped, no "not attached" crash. */
    @Test
    public void animatorOnDetachedPopupWindow_doesNotCrash() {
        AtomicReference<Boolean> wasShowing = new AtomicReference<>(null);
        AtomicReference<Boolean> showingAfterDetach = new AtomicReference<>(null);
        AtomicReference<Boolean> contentAttachedAfterDetach = new AtomicReference<>(null);
        AtomicReference<Throwable> setupError = new AtomicReference<>(null);
        AtomicReference<Throwable> caught = new AtomicReference<>(null);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ModalRichMediaWindow window = showWindow(activity);
                wasShowing.set(window.isShowing());

                ValueAnimator animator = ModalRichMediaWindowUtils.dismissWindowToRightAnimation(window, 1080);

                // Detach the decor from the WindowManager while the popup still thinks it is showing
                // — exactly the state left by the activity teardown (the 54% "background" case).
                // If the popup was never really attached, removeViewImmediate itself throws "not
                // attached"; capture that as a SETUP failure so it isn't misread as a guard regression.
                try {
                    detachDecorFromWindowManager(activity, window);
                } catch (Throwable t) {
                    setupError.set(t);
                    return;
                }

                // Prove the #193 crash state was actually reconstructed before pumping a frame:
                // the popup must still report showing while its content view is detached from the WM.
                // Otherwise the guard could skip update() for the wrong reason and the test passes vacuously.
                showingAfterDetach.set(window.isShowing());
                contentAttachedAfterDetach.set(window.getContentView().isAttachedToWindow());

                try {
                    pumpOneAnimatorFrame(animator);
                } catch (Throwable t) {
                    caught.set(t);
                }
            });
        }

        assertNull(
                "Detach setup failed (popup was not attached to the WindowManager), not a guard failure: "
                        + setupError.get(),
                setupError.get());
        assertEquals("Precondition: PopupWindow must have been really shown/attached", Boolean.TRUE, wasShowing.get());
        assertEquals(
                "Crash-state precondition: popup must still report showing after detach",
                Boolean.TRUE,
                showingAfterDetach.get());
        assertEquals(
                "Crash-state precondition: content view must be detached from the window manager",
                Boolean.FALSE,
                contentAttachedAfterDetach.get());
        assertNull(
                "Fix #193: animator frame on a detached window must not crash, but got: " + caught.get(), caught.get());
    }

    /**
     * Control: same animator frame, but the window stays attached. update() must NOT crash either.
     */
    @Test
    public void animatorUpdatesAttachedPopupWindow_doesNotThrow() {
        AtomicReference<Boolean> wasShowing = new AtomicReference<>(null);
        AtomicReference<Throwable> caught = new AtomicReference<>(null);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ModalRichMediaWindow window = showWindow(activity);
                wasShowing.set(window.isShowing());

                ValueAnimator animator = ModalRichMediaWindowUtils.dismissWindowToRightAnimation(window, 1080);
                try {
                    pumpOneAnimatorFrame(animator); // window still attached
                } catch (Throwable t) {
                    caught.set(t);
                } finally {
                    window.dismiss();
                }
            });
        }

        assertEquals(Boolean.TRUE, wasShowing.get());
        assertNull("update() on an attached window must not crash, but got: " + caught.get(), caught.get());
    }
}
