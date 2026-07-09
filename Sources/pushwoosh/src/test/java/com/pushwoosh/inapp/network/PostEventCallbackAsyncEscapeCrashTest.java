package com.pushwoosh.inapp.network;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PostEventCallbackAsyncEscapeCrashTest {

    private PlatformTestManager platformTestManager;
    private InAppRepository realRepo;
    private PushwooshInAppImpl pushwooshInApp;
    private RequestManager requestManagerMock;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        SdkStateProvider.getInstance().setReady();

        // The real InAppRepository the real PushwooshInAppImpl was built on top of.
        realRepo = InAppModule.getInAppRepository();

        // Make io synchronous: the io.submit body (and the BackgroundExecutor.main hop it enqueues) run
        // inline on the test thread, so delivery is deterministic instead of racing a real background thread.
        WhiteboxHelper.setInternalState(realRepo, "io", InAppExecutorServiceHelper.createExecutorService());

        // Capture-only request manager: we drive the network callback ourselves via deliverThroughBarrier.
        requestManagerMock = mock(RequestManager.class);
        WhiteboxHelper.setInternalState(realRepo, "requestManager", requestManagerMock);

        pushwooshInApp = platformTestManager.getPushwooshInApp();
    }

    @After
    public void tearDown() {
        SdkStateProvider.getInstance().resetForTesting();
        platformTestManager.tearDown();
    }

    /**
     * Faithful stand-in for the production barrier. In prod PushwooshRequestManager delivers the
     * network callback via {@code BackgroundExecutor.main(() -> safeProcessCallback(cb, result))} —
     * wrapWithErrorHandling catches Throwable, safeProcessCallback catches Exception. The essence is
     * "the network callback runs inside a try/catch(Throwable) on the main thread"; modeled here.
     */
    private static void deliverThroughBarrier(
            Callback<PostEventResponse, NetworkException> networkCallback,
            Result<PostEventResponse, NetworkException> result) {
        try {
            networkCallback.process(result);
        } catch (Throwable t) {
            // swallowed — exactly what safeProcessCallback + wrapWithErrorHandling do in prod
        }
    }

    @SuppressWarnings("unchecked")
    private Callback<PostEventResponse, NetworkException> triggerAndCaptureNetworkCallback(
            Callback<Void, PostEventException> hostCallback) {
        pushwooshInApp.postEvent("CrashEvent", null, hostCallback);
        ArgumentCaptor<Callback<PostEventResponse, NetworkException>> cbCaptor =
                ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(PostEventRequest.class), cbCaptor.capture());
        return cbCaptor.getValue();
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
        Callback<PostEventResponse, NetworkException> networkCallback = triggerAndCaptureNetworkCallback(host);

        // success delivered: io.submit runs inline, the deferred main hop enqueues (looper paused).
        deliverThroughBarrier(networkCallback, Result.fromData(successResponse()));

        // the deferred body now runs through BackgroundExecutor.main's Throwable barrier, so the host
        // throw is absorbed on the main Looper instead of escaping the drain -> graceful, no crash.
        ShadowLooper.idleMainLooper();

        // non-vacuous: the callback WAS delivered on the success path (and its throw swallowed), not
        // silently dropped -- the success path now matches the error path's guarded outcome.
        assertEquals(1, host.invocations);
    }

    @Test
    public void postEventError_hostCallbackThrows_swallowedByBarrier() {
        ThrowingHostCallback host = new ThrowingHostCallback();
        Callback<PostEventResponse, NetworkException> networkCallback = triggerAndCaptureNetworkCallback(host);

        // error delivered through the barrier: host throw happens synchronously inside -> caught, no escape.
        deliverThroughBarrier(networkCallback, Result.fromException(new NetworkException("server error")));

        assertEquals("host callback must have been invoked (and its throw swallowed)", 1, host.invocations);
        // nothing was deferred to main -> draining must not throw.
        ShadowLooper.idleMainLooper();
    }
}
