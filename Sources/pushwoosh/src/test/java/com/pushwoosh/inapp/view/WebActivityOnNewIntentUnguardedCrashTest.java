package com.pushwoosh.inapp.view;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.util.AndroidRuntimeException;
import android.webkit.WebView;

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
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowWebView;

/**
 * Regression guard for crash-webactivity-onnewintent-unguarded.
 *
 * WebActivity.onNewIntent -> processIntent used to have no try/catch (processIntent itself is
 * try {} finally {} with no catch), so any Throwable escaped onto the system. Its twin,
 * RichMediaWebActivity.onCreate, already wraps super.onCreate (-> processIntent) in
 * try/catch(Throwable)+finish(); onNewIntent is NOT overridden in the only subclass, so the guarded
 * and unguarded entry points ran the identical code. The fix wraps onNewIntent's processIntent call in
 * the same guard, so a Throwable from WebView construction is caught and the activity finishes instead
 * of crashing.
 *
 * The realistic Throwable is WebView construction failing when the system WebView provider is
 * mid-update (AndroidRuntimeException). That environmental condition is forced deterministically here
 * by a WebView shadow whose constructor throws — the outcome is faithful; only the reason the WebView
 * fails is a stand-in.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "AndroidManifest.xml",
        shadows = {WebActivityOnNewIntentUnguardedCrashTest.ThrowingWebViewShadow.class})
@LooperMode(LooperMode.Mode.LEGACY)
public class WebActivityOnNewIntentUnguardedCrashTest {

    // Forces the environmental "WebView provider unavailable" condition: constructing a WebView throws
    // the same exception class the provider mid-update throws in production. Extends ShadowWebView so the
    // View/ViewGroup super-constructor chain stays shadowed (otherwise the real provider stub throws its
    // own UnsupportedOperationException before this body runs).
    @Implements(WebView.class)
    public static class ThrowingWebViewShadow extends ShadowWebView {
        @Implementation
        protected void __constructor__(Context context) {
            throw new AndroidRuntimeException("WebView provider unavailable (repro stand-in)");
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

    private static Intent inAppIntent(String code) {
        // isInApp() == true (non-empty code, no "r-" prefix); required=false => SINGLE_TOP scenario.
        Resource resource =
                new Resource(code, "url-" + code, "hash-" + code, 0, InAppLayout.FULLSCREEN, null, false, 0);
        return WebActivity.applyIntentParams(new Intent(), resource, "", InAppView.MODE_DEFAULT);
    }

    // Builds the activity for the FIRST in-app. onCreate's processIntent hits the throwing WebView, which
    // RichMediaWebActivity.onCreate catches (finish()+return) — so create() completes and the instance
    // exists, ready to receive a second in-app via onNewIntent.
    private ActivityController<RichMediaWebActivity> buildFirstInApp() {
        Intent first = inAppIntent("code1");
        ActivityController<RichMediaWebActivity> controller =
                Robolectric.buildActivity(RichMediaWebActivity.class, first);
        controller.create(first.getExtras());
        return controller;
    }

    // Delivers a second, different in-app the way the system does under SINGLE_TOP: straight into the
    // live instance's onNewIntent. (ActivityController.newIntent is unsupported in this Robolectric
    // version; onNewIntent is package-visible protected, so this calls the exact same method the system
    // dispatches.)
    private static void deliverSecondInApp(RichMediaWebActivity activity, String code) {
        activity.onNewIntent(inAppIntent(code));
    }

    // REGRESSION GUARD: a second, different in-app delivered to the live RichMediaWebActivity via
    // onNewIntent (SINGLE_TOP) runs the once-unguarded processIntent; WebView construction throws. Before
    // the fix the AndroidRuntimeException escaped onNewIntent onto the system (this call would error).
    // The fix's guard (symmetric with the onCreate twin) catches it, so the call returns normally and the
    // activity is left finishing rather than crashing. The load-bearing regression signal is that no
    // Throwable escapes onNewIntent; isFinishing() confirms the graceful terminal state the guard chose.
    @Test
    public void onNewIntent_secondInApp_webViewThrows_isCaughtAndFinishesGracefully() {
        ActivityController<RichMediaWebActivity> controller = buildFirstInApp();

        deliverSecondInApp(controller.get(), "code2");

        assertTrue(
                "onNewIntent must catch the WebView throw and finish() the activity instead of crashing",
                controller.get().isFinishing());
    }

    // BARRIER (asymmetry proof, negative control): the IDENTICAL WebView throw on the onCreate path is
    // caught by RichMediaWebActivity.onCreate — create() does not propagate it and the activity is
    // finishing. This is the guard onNewIntent used to lack; kept unchanged as ground truth.
    @Test
    public void onCreate_sameWebViewThrow_isCaughtAndFinishesGracefully() {
        ActivityController<RichMediaWebActivity> controller = buildFirstInApp();

        assertTrue(
                "onCreate must catch the WebView throw and finish() the activity",
                controller.get().isFinishing());
    }
}
