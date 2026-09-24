package com.pushwoosh.inapp.view.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.animation.ValueAnimator;
import android.view.View;

import com.pushwoosh.inapp.view.ModalRichMediaWindow;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Collections;

/**
 * Covers GitHub #193: the slide animators' per-frame update() must be skipped once the popup's
 * content view is detached from the window manager, otherwise PopupWindow.update() throws
 * "not attached to window manager". The crash is shared by all 8 slide animators and the drag
 * path; this suite drives one synchronous animator frame through each public factory and
 * through movePopupOnDragEvent — the exact paths the crash came from. FADE animators are out of
 * scope: they only call setAlpha(), never update().
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class ModalRichMediaWindowUtilsTest {

    private static final int SCREEN_WIDTH = 1080;
    private static final int SCREEN_HEIGHT = 1920;

    @Mock
    ModalRichMediaWindow window;

    @Mock
    View contentView;

    @Mock
    ModalRichmediaConfig config;

    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(window.getContentView()).thenReturn(contentView);
        when(window.getTopInset()).thenReturn(0);
        when(window.getBottomInset()).thenReturn(0);
        lenient().when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // setCurrentPlayTime runs the update listener synchronously on the calling thread,
    // exactly the SDK lambda that calls window.update().
    private static void pumpOneFrame(ValueAnimator animator) {
        animator.setDuration(1000);
        animator.setCurrentPlayTime(500);
    }

    private void attached() {
        when(window.isShowing()).thenReturn(true);
        when(contentView.isAttachedToWindow()).thenReturn(true);
    }

    private void detached() {
        when(window.isShowing()).thenReturn(true);
        when(contentView.isAttachedToWindow()).thenReturn(false);
    }

    // Builds the slide animator for a given factory, kept as a single point so the sweep tests
    // stay readable. Indexes follow the 4 arg-shapes: width-based dismiss/present and
    // height+config dismiss/present.
    private ValueAnimator slideAnimator(int index) {
        switch (index) {
            case 0:
                return ModalRichMediaWindowUtils.dismissWindowToLeftAnimation(window, SCREEN_WIDTH);
            case 1:
                return ModalRichMediaWindowUtils.dismissWindowToRightAnimation(window, SCREEN_WIDTH);
            case 2:
                return ModalRichMediaWindowUtils.dismissWindowToTopAnimation(window, SCREEN_HEIGHT, config);
            case 3:
                return ModalRichMediaWindowUtils.dismissWindowToBottomAnimation(window, SCREEN_HEIGHT, config);
            case 4:
                return ModalRichMediaWindowUtils.presentWindowFromLeftAnimation(window, SCREEN_WIDTH);
            case 5:
                return ModalRichMediaWindowUtils.presentWindowFromRightAnimation(window, SCREEN_WIDTH);
            case 6:
                return ModalRichMediaWindowUtils.presentWindowFromTopAnimation(window, SCREEN_HEIGHT, config);
            case 7:
                return ModalRichMediaWindowUtils.presentWindowFromBottomAnimation(window, SCREEN_HEIGHT, config);
            default:
                throw new IllegalArgumentException("no slide animator " + index);
        }
    }

    private static final int SLIDE_ANIMATOR_COUNT = 8;

    // Verifies that none of the 8 slide animators call update() when the content view is detached
    // from the window manager — the whole animator family shares the #193 guard, not just to-right.
    // The shared detached window is reused across factories: every frame must be a no-op, so the
    // verify(never()) holds regardless of which factory ran before.
    @Test
    public void testAllSlideAnimatorsSkipUpdateWhenDetached() {
        detached();

        for (int i = 0; i < SLIDE_ANIMATOR_COUNT; i++) {
            pumpOneFrame(slideAnimator(i));
        }

        verify(window, never()).update(anyInt(), anyInt(), anyInt(), anyInt());
    }

    // Verifies that every slide animator calls update() on an attached window — the guard must not
    // suppress the real animation frame. All 8 factories share one observable (one update() per
    // pumped frame), so the family is swept in a single parameterized test, mirroring the detached
    // sweep above.
    @Test
    public void testAllSlideAnimatorsCallUpdateWhenAttached() {
        attached();

        for (int i = 0; i < SLIDE_ANIMATOR_COUNT; i++) {
            pumpOneFrame(slideAnimator(i));
        }

        verify(window, times(SLIDE_ANIMATOR_COUNT)).update(anyInt(), anyInt(), anyInt(), anyInt());
    }

    // Verifies that the not-showing window short-circuits the guard even when the content view
    // still reports attached — PopupWindow.update() must not be called on a dismissed popup.
    @Test
    public void testSlideAnimatorSkipsUpdateWhenNotShowing() {
        when(window.isShowing()).thenReturn(false);
        when(contentView.isAttachedToWindow()).thenReturn(true);

        pumpOneFrame(slideAnimator(1));

        verify(window, never()).update(anyInt(), anyInt(), anyInt(), anyInt());
    }

    // Covers the content == null leg of the guard: getContentView() can return null on a torn-down
    // window (close() clears resourceWebView), and the guard must skip update() rather than NPE on
    // content.isAttachedToWindow(). Without this, a refactor dropping the null check stays green.
    @Test
    public void testSlideAnimatorSkipsUpdateWhenContentViewNull() {
        when(window.isShowing()).thenReturn(true);
        when(window.getContentView()).thenReturn(null);

        pumpOneFrame(slideAnimator(1));

        verify(window, never()).update(anyInt(), anyInt(), anyInt(), anyInt());
    }

    // Verifies the guarded update() receives the real animated coordinates, not just "some int":
    // the inset term (top + bottom) must land in y and the width/height args must stay -1. The
    // anyInt() sweep tests above would not catch a regression that drops the inset or corrupts the
    // dimensions, so this one asserts the actual values (criterion: animation behavior unchanged).
    @Test
    public void testSlideAnimatorPassesAnimatedCoordinatesToUpdate() {
        attached();
        when(window.getTopInset()).thenReturn(7);
        when(window.getBottomInset()).thenReturn(11);

        ArgumentCaptor<Integer> x = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> y = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> width = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> height = ArgumentCaptor.forClass(Integer.class);

        // dismissWindowToRight: x = animatedValue in (0, SCREEN_WIDTH], y = topInset + bottomInset.
        pumpOneFrame(ModalRichMediaWindowUtils.dismissWindowToRightAnimation(window, SCREEN_WIDTH));

        verify(window).update(x.capture(), y.capture(), width.capture(), height.capture());
        assertEquals(7 + 11, (int) y.getValue());
        assertEquals(-1, (int) width.getValue());
        assertEquals(-1, (int) height.getValue());
        assertTrue("x must be the live animated offset", x.getValue() > 0 && x.getValue() <= SCREEN_WIDTH);
    }

    // Verifies that the drag path skips the 5-arg force-update when the content view is detached —
    // movePopupOnDragEvent shares the #193 crash class via update(x, y, w, h, force).
    @Test
    public void testDragSkipsUpdateWhenDetached() {
        detached();
        when(config.getSwipeGestures()).thenReturn(Collections.singleton(ModalRichMediaSwipeGesture.DOWN));

        ModalRichMediaWindowUtils.movePopupOnDragEvent(window, 10, 20, config);

        verify(window, never()).update(anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean());
    }

    // Verifies that the drag path calls the 5-arg force-update on an attached window so dragging
    // still repositions the popup.
    @Test
    public void testDragCallsForceUpdateWhenAttached() {
        attached();
        when(config.getSwipeGestures()).thenReturn(Collections.singleton(ModalRichMediaSwipeGesture.DOWN));

        ModalRichMediaWindowUtils.movePopupOnDragEvent(window, 10, 20, config);

        verify(window).update(anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean());
    }

    // SDK-984: with animations off the present factory must return null — this skips animation
    // entirely, same as the NONE animation type.
    @Test
    public void testPresentFactoryReturnsNullForSlideUpWhenAnimationsOff() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_UP);

        assertNull(ModalRichMediaWindowUtils.getPresentValueAnimatorForWindow(window, config, true));
    }

    // FADE_IN is not affected by the original defect (alpha starts at 1, nothing sets it before the
    // animator runs), so suppressing its animator is safe — spec decision: one early exit for all types.
    @Test
    public void testPresentFactoryReturnsNullForFadeInWhenAnimationsOff() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN);

        assertNull(ModalRichMediaWindowUtils.getPresentValueAnimatorForWindow(window, config, true));
    }

    // SDK-988: NONE never produces a present animator — no animation is applied.
    @Test
    public void testPresentFactoryReturnsNullForNone() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.NONE);

        assertNull(ModalRichMediaWindowUtils.getPresentValueAnimatorForWindow(window, config, false));
    }

    // Guard rail: with animations ON the factory keeps producing animators — the early exit must
    // not eat the normal path.
    @Test
    public void testPresentFactoryReturnsAnimatorForSlideUpWhenAnimationsOn() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_UP);

        assertNotNull(ModalRichMediaWindowUtils.getPresentValueAnimatorForWindow(window, config, false));
    }

    // SDK-984: with animations off the window is shown at its resting position, not the off-screen
    // SLIDE_UP start coordinate (y == screenHeight) where it would stay without animation frames.
    @Test
    public void testShowPositionYForSlideUpIsStaticWhenAnimationsOff() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_UP);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);

        assertEquals(0, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, true));
    }

    // Guard rail for the same case with animations ON: start coordinate stays the off-screen
    // screenHeight (package-visible static field) so the slide still starts from below.
    @Test
    public void testShowPositionYForSlideUpIsOffScreenWhenAnimationsOn() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_UP);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.BOTTOM);

        assertEquals(
                ModalRichMediaWindowUtils.screenHeight,
                ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, false));
    }

    // DROP_DOWN parks at x == screenWidth, y == -screenHeight; both must collapse to the static
    // position when animations are off.
    @Test
    public void testShowPositionForDropDownIsStaticWhenAnimationsOff() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.DROP_DOWN);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);

        assertEquals(0, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionX(config, true));
        assertEquals(0, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, true));
    }

    @Test
    public void testShowPositionXForSlideFromLeftIsStaticWhenAnimationsOff() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_FROM_LEFT);

        assertEquals(0, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionX(config, true));
    }

    // Guard rail with animations ON: SLIDE_FROM_LEFT still starts off-screen to the left.
    @Test
    public void testShowPositionXForSlideFromLeftIsOffScreenWhenAnimationsOn() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_FROM_LEFT);

        assertEquals(
                -ModalRichMediaWindowUtils.screenWidth,
                ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionX(config, false));
    }

    // With animations off a TOP modal keeps the status bar inset — the same resting position the
    // FADE_IN and NONE branches use; the inset is stubbed non-zero so the assert can tell it from 0.
    @Test
    public void testShowPositionYForTopKeepsStatusBarInsetWhenAnimationsOff() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_UP);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.TOP);

        try (MockedStatic<ModalRichMediaWindowUtils> utils =
                Mockito.mockStatic(ModalRichMediaWindowUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(ModalRichMediaWindowUtils::getSystemWindowInsetTop).thenReturn(63);

            assertEquals(63, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, true));
        }
    }

    // BOTTOM mirrors TOP: the static position sits above the nav bar, where drag-snap lands.
    @Test
    public void testShowPositionYForBottomKeepsNavBarInsetWhenAnimationsOff() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_UP);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.BOTTOM);

        try (MockedStatic<ModalRichMediaWindowUtils> utils =
                Mockito.mockStatic(ModalRichMediaWindowUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(() -> ModalRichMediaWindowUtils.getSystemWindowInsetBottom(config))
                    .thenReturn(126);

            assertEquals(126, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, true));
        }
    }

    // SDK-987: FADE_IN never moves the window, so show must land at the resting position — above
    // the nav bar, not y == 0 glued under it.
    @Test
    public void testShowPositionYForFadeInBottomIsNavBarInset() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.BOTTOM);

        try (MockedStatic<ModalRichMediaWindowUtils> utils =
                Mockito.mockStatic(ModalRichMediaWindowUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(() -> ModalRichMediaWindowUtils.getSystemWindowInsetBottom(config))
                    .thenReturn(126);

            assertEquals(126, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, false));
        }
    }

    // Guard rail: the TOP leg of the FADE_IN branch keeps the status bar inset — symmetry intact.
    @Test
    public void testShowPositionYForFadeInTopIsStatusBarInset() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.TOP);

        try (MockedStatic<ModalRichMediaWindowUtils> utils =
                Mockito.mockStatic(ModalRichMediaWindowUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(ModalRichMediaWindowUtils::getSystemWindowInsetTop).thenReturn(63);

            assertEquals(63, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, false));
        }
    }

    // Guard rail: CENTER has no insets (gravity centers the window) — both insets are stubbed
    // non-zero to prove the branch ignores them.
    @Test
    public void testShowPositionYForFadeInCenterIsZero() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);

        try (MockedStatic<ModalRichMediaWindowUtils> utils =
                Mockito.mockStatic(ModalRichMediaWindowUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(ModalRichMediaWindowUtils::getSystemWindowInsetTop).thenReturn(63);
            utils.when(() -> ModalRichMediaWindowUtils.getSystemWindowInsetBottom(config))
                    .thenReturn(126);

            assertEquals(0, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, false));
        }
    }

    // SDK-988: NONE never moves the window either, so show must land at the resting position —
    // above the nav bar, not y == 0 glued under it. Same contract as FADE_IN, one switch branch.
    @Test
    public void testShowPositionYForNoneBottomIsNavBarInset() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.NONE);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.BOTTOM);

        try (MockedStatic<ModalRichMediaWindowUtils> utils =
                Mockito.mockStatic(ModalRichMediaWindowUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(() -> ModalRichMediaWindowUtils.getSystemWindowInsetBottom(config))
                    .thenReturn(126);

            assertEquals(126, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, false));
        }
    }

    // The TOP leg of the same defect: content must start below the status bar, not under the clock.
    @Test
    public void testShowPositionYForNoneTopIsStatusBarInset() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.NONE);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.TOP);

        try (MockedStatic<ModalRichMediaWindowUtils> utils =
                Mockito.mockStatic(ModalRichMediaWindowUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(ModalRichMediaWindowUtils::getSystemWindowInsetTop).thenReturn(63);

            assertEquals(63, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, false));
        }
    }

    // Guard rail: CENTER has no insets (gravity centers the window) — both insets are stubbed
    // non-zero to prove the NONE branch ignores them.
    @Test
    public void testShowPositionYForNoneCenterIsZero() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.NONE);
        when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);

        try (MockedStatic<ModalRichMediaWindowUtils> utils =
                Mockito.mockStatic(ModalRichMediaWindowUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(ModalRichMediaWindowUtils::getSystemWindowInsetTop).thenReturn(63);
            utils.when(() -> ModalRichMediaWindowUtils.getSystemWindowInsetBottom(config))
                    .thenReturn(126);

            assertEquals(0, ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config, false));
        }
    }
}
