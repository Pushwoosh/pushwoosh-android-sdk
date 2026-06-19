package com.pushwoosh.inapp.view.utils;

import static org.junit.Assert.assertEquals;
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
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
}
