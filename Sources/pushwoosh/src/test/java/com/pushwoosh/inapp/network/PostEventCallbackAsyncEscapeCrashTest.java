package com.pushwoosh.inapp.network;

import static org.junit.Assert.assertEquals;

import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.network.FakeRequestManager;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

/**
 * Regression guard for crash-postevent-callback-async-escape.
 *
 * InAppRepository.handlePostEventResponse() used to deliver the postEvent-SUCCESS callback through a
 * raw {@code main.post} that lived OUTSIDE the barrier every other callback delivery is wrapped in,
 * so a host callback that threw escaped uncaught on the main Looper on the success path while the SAME
 * throw on the error path was swallowed. The fix routes the success delivery through
 * {@code BackgroundExecutor.main} (Throwable-catch), matching the error path. Both tests now assert the
 * host throw is swallowed on both paths — the escape is closed, the asymmetry is gone.
 *
 * <p>The network callback is delivered through the production barrier
 * ({@code PushwooshRequestManager.deliverOnMain}, reached via {@code FakeRequestManager.deliver}), so a
 * regression in the barrier itself breaks these tests instead of being masked by a hand-written copy.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PostEventCallbackAsyncEscapeCrashTest {

    private PlatformTestManager platformTestManager;
    private InAppRepository realRepo;
    private PushwooshInAppImpl pushwooshInApp;
    private FakeRequestManager fake;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        SdkStateProvider.getInstance().setReady();

        // The real InAppRepository the real PushwooshInAppImpl was built on top of.
        realRepo = InAppModule.getInAppRepository();

        // Make io synchronous: the io.submit body (and the BackgroundExecutor.main hop it enqueues) run
        // inline on the test thread, so delivery is deterministic instead of racing a real background thread.
        WhiteboxHelper.setInternalState(realRepo, "io", InAppExecutorServiceHelper.createExecutorService());

        // Capture-only: we drive the network callback ourselves through the production barrier.
        // The seam resolves the manager per call, so the double is global rather than a field on realRepo.
        fake = FakeRequestManager.install();
        fake.captureOnly("postEvent");

        pushwooshInApp = platformTestManager.getPushwooshInApp();
    }

    @After
    public void tearDown() {
        NetworkModule.setRequestManager(null);
        SdkStateProvider.getInstance().resetForTesting();
        platformTestManager.tearDown();
    }

    private FakeRequestManager.Sent triggerAndCaptureSend(Callback<Void, PostEventException> hostCallback) {
        pushwooshInApp.postEvent("CrashEvent", null, hostCallback);
        fake.awaitCount("postEvent", 1);
        return fake.last("postEvent");
    }

    private static PostEventResponse successResponse() throws JSONException {
        return new PostEventResponse(new JSONObject());
    }

    /** Host callback that throws — the developer bug the signal describes (dead Activity, null field, ...). */
    static class ThrowingHostCallback implements Callback<Void, PostEventException> {
        static final String BOOM = "host postEvent callback boom";
        int invocations = 0;

        @Override
        public void process(Result<Void, PostEventException> result) {
            invocations++;
            throw new RuntimeException(BOOM);
        }
    }

    @Test
    public void postEventSuccess_hostCallbackThrows_swallowedByBarrier() throws Exception {
        ShadowLooper.pauseMainLooper();

        ThrowingHostCallback host = new ThrowingHostCallback();
        FakeRequestManager.Sent sent = triggerAndCaptureSend(host);

        // success delivered: io.submit runs inline, the deferred main hop enqueues (looper paused).
        fake.deliver(sent, Result.fromData(successResponse()));

        // Two hops queue here (the barrier itself, then handlePostEventResponse's), but one drain is
        // enough: the scheduler keeps running same-time tasks queued during the drain. Do not add a second.
        ShadowLooper.idleMainLooper();

        // non-vacuous: the callback WAS delivered on the success path (and its throw swallowed), not
        // silently dropped -- the success path now matches the error path's guarded outcome.
        assertEquals(1, host.invocations);
        fake.assertAllScripted();
    }

    @Test
    public void postEventError_hostCallbackThrows_swallowedByBarrier() {
        ThrowingHostCallback host = new ThrowingHostCallback();
        FakeRequestManager.Sent sent = triggerAndCaptureSend(host);

        fake.deliver(sent, Result.fromException(new NetworkException("server error")));

        // Delivery is now a main-thread post through the production barrier, not an inline call, so
        // the assert must come after the drain.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals("host callback must have been invoked (and its throw swallowed)", 1, host.invocations);
        fake.assertAllScripted();
    }
}
