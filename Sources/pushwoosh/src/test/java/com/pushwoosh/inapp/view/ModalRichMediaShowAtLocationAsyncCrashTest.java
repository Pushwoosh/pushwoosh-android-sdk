package com.pushwoosh.inapp.view;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

/**
 * Regression guard for crash-modalrichmedia-showatlocation-async.
 *
 * ModalRichMediaWindow.onPageLoaded() wraps the show in try/catch(Exception), but the show is scheduled
 * asynchronously: getParentViewAsync posts the callback via decorView.post (ModalRichMediaWindowUtils:56).
 * The try/catch therefore guards only the synchronous scheduling, which returns immediately; the real
 * showAtLocation (showAtLocationOrSubscribeToActivityBroughtOnTop:315) runs later, in a separate main-looper
 * message, OUTSIDE the try/catch. Its only guard is topActivity != null (:314) — it does NOT check
 * parentView/windowToken. When the Activity finishes between onPageFinished and the posted callback,
 * PopupWindow.showAtLocation throws WindowManager.BadTokenException (or NPE if the parent view is gone) onto
 * the bare main looper, uncaught.
 *
 * The fix wraps the posted callback body in getParentViewAsync in its own try/catch(Throwable), the only
 * place that can guard a decorView.post message. This test proves the escape is now swallowed there.
 *
 * The environmental condition (invalid window token -> BadTokenException) is forced deterministically: a
 * real Activity is destroyed after onPageLoaded has scheduled the async show, which makes its content view's
 * windowToken null (real Robolectric behavior). The framework rejection of a null token is supplied by the
 * showAtLocation override below — that is exactly what a real device's WindowManager does but Robolectric
 * stubs out. Everything else (onPageLoaded, getParentViewAsync's decorView.post, and
 * showAtLocationOrSubscribeToActivityBroughtOnTop) is the real inherited SDK code. The main looper is paused
 * to make the post genuinely deferred (in Robolectric LEGACY an unpaused post runs inline, masking the gap);
 * pausing substitutes determinism for production timing, the outcome is faithful.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class ModalRichMediaShowAtLocationAsyncCrashTest {

    // Supplies the WindowManager behavior Robolectric omits: showAtLocation on a parent with an invalid
    // (null) window token throws BadTokenException — the exact framework throw a real device raises when the
    // Activity has finished. The SDK code that routes into it is entirely real and inherited. showAttempted
    // records that the show was actually reached (so the graceful assertion is not vacuous).
    static class TokenCheckingModalWindow extends ModalRichMediaWindow {
        boolean showAttempted = false;

        TokenCheckingModalWindow(Context context, Resource resource) {
            super(context, resource);
        }

        @Override
        public void showAtLocation(View parent, int gravity, int x, int y) {
            showAttempted = true;
            if (parent == null || parent.getWindowToken() == null) {
                throw new WindowManager.BadTokenException(
                        "Unable to add window -- token null is not valid; is your activity running?");
            }
            super.showAtLocation(parent, gravity, x, y);
        }
    }

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        EventBus.clearSubscribersMap();
    }

    @After
    public void tearDown() {
        EventBus.clearSubscribersMap();
        platformTestManager.tearDown();
    }

    private static Resource inAppResource() {
        return new Resource("code1", "url1", "hash1", 0, InAppLayout.FULLSCREEN, null, false, 0);
    }

    // Builds the modal on a live Activity, then (looper paused) runs onPageLoaded so the async show is
    // scheduled but not yet executed, then destroys the Activity so its content view's windowToken goes null
    // — the state a real "Activity finished mid-load" produces. The posted show is still pending.
    private TokenCheckingModalWindow scheduleAsyncShowThenFinishActivity() {
        ActivityController<Activity> controller =
                Robolectric.buildActivity(Activity.class).setup();
        Activity activity = controller.get();
        PushwooshPlatform.getInstance().setTopActivity(activity);
        TokenCheckingModalWindow window = new TokenCheckingModalWindow(activity, inAppResource());

        ShadowLooper.pauseMainLooper();
        window.onPageLoaded(); // schedules getParentViewAsync via decorView.post; try/catch completes and returns
        controller.pause().stop().destroy(); // Activity finishes -> content view windowToken becomes null
        return window;
    }

    // REGRESSION: the show posted by onPageLoaded runs after onPageLoaded's try/catch has already returned. On
    // master its BadTokenException escaped the main looper uncaught; after the fix getParentViewAsync's posted
    // body has its own try/catch(Throwable), so idleMainLooper drains cleanly. showAttempted proves the show
    // was actually reached and threw (the throwable was swallowed, not skipped by a guard).
    @Test
    public void asyncShow_afterActivityFinished_isSwallowedGracefully() {
        TokenCheckingModalWindow window = scheduleAsyncShowThenFinishActivity();

        ShadowLooper.idleMainLooper(); // must NOT throw: the posted show's BadTokenException is now caught

        assertTrue(
                "the async show must have been attempted (throwable generated, then swallowed by the fix)",
                window.showAttempted);
    }

    // BARRIER (async-vs-sync proof): the IDENTICAL BadTokenException, when the show runs SYNCHRONOUSLY inside
    // onPageLoaded (looper not paused -> decorView.post executes inline within the try block), IS caught — on
    // master by onPageLoaded's try/catch, after the fix by getParentViewAsync's inner catch. Either way
    // onPageLoaded returns without throwing. The only difference from the crash is the async posting.
    @Test
    public void onPageLoaded_synchronousShowThrow_isCaughtByTryCatch() {
        ActivityController<Activity> controller =
                Robolectric.buildActivity(Activity.class).setup();
        Activity activity = controller.get();
        PushwooshPlatform.getInstance().setTopActivity(activity);
        ModalRichMediaWindow window = new TokenCheckingModalWindow(activity, inAppResource());
        controller.pause().stop().destroy(); // windowToken null before the (synchronous) show runs

        window.onPageLoaded(); // must NOT throw: the synchronous show's BadTokenException is swallowed
    }
}
