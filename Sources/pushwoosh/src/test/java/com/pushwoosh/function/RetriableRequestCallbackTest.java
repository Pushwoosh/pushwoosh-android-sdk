package com.pushwoosh.function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.FakeRequestManager;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class RetriableRequestCallbackTest {

    private PlatformTestManager platformTestManager;
    private FakeRequestManager fake;
    private PushRequest<String> mockRequest;
    private Callback<String, NetworkException> mockCallback;
    private RetriableRequestCallback<String> retriableCallback;

    @Before
    public void setUp() throws Exception {
        // Reuse existing test infrastructure
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();

        fake = platformTestManager.getRequestManager();
        mockRequest = Mockito.mock(PushRequest.class);
        when(mockRequest.getMethod()).thenReturn("testMethod");
        mockCallback = CallbackWrapper.spy();

        retriableCallback = new RetriableRequestCallback<>(mockCallback, mockRequest);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void testImmediateSuccess() {
        Result<String, NetworkException> successResult = Result.fromData("success");

        retriableCallback.process(successResult);

        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(mockCallback).process(captor.capture());

        assertThat(captor.getValue().isSuccess(), is(true));
        assertThat(captor.getValue().getData(), is(equalTo("success")));
    }

    @Test
    public void testNonRetriableError() {
        // Test HTTP 400 - should not retry
        ConnectionException nonRetriableException = new ConnectionException("Bad Request", 400, 0);
        Result<String, NetworkException> errorResult = Result.fromException(nonRetriableException);

        retriableCallback.process(errorResult);

        // Should immediately call callback without retry
        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(mockCallback).process(captor.capture());

        assertThat(captor.getValue().isSuccess(), is(false));
        assertThat(captor.getValue().getException(), is(equalTo(nonRetriableException)));
    }

    @Test
    public void testRetriableConnectionError() {
        // Setup connection failure (status codes both 0)
        ConnectionException connectionException = new ConnectionException("Connection failed", 0, 0);
        Result<String, NetworkException> failureResult = Result.fromException(connectionException);

        // Mock successful retry response
        fake.respondWith("testMethod", createSuccessResponse("retry_success"));

        retriableCallback.process(failureResult);

        // Advance time to trigger first retry
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(mockCallback, timeout(2000)).process(captor.capture());

        assertThat(captor.getValue().isSuccess(), is(true));
        fake.assertAllScripted();
    }

    @Test
    public void testMaxRetriesExceeded() {
        ConnectionException connectionException = new ConnectionException("Connection failed", 0, 0);
        Result<String, NetworkException> failureResult = Result.fromException(connectionException);

        // Mock all retries to fail
        fake.alwaysFailWith("testMethod", connectionException);

        retriableCallback.process(failureResult);

        // Wait for all retries to complete (total time is 1 + 5 + 10 = 16 seconds)
        // Since RetriableRequestCallback uses a real ScheduledExecutorService, we need to wait for real time
        // or use a longer timeout to allow the retries to happen

        // Final callback should be called with last failure after all retries (allow 20 seconds)
        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(mockCallback, timeout(20000)).process(captor.capture());

        assertThat(captor.getValue().isSuccess(), is(false));
        fake.assertAllScripted();
    }

    @Test
    public void testNullCallbackHandling() {
        RetriableRequestCallback<String> retriableCallbackWithNullCallback =
                new RetriableRequestCallback<>(null, mockRequest);

        Result<String, NetworkException> successResult = Result.fromData("success");

        // Should not throw exception
        retriableCallbackWithNullCallback.process(successResult);

        // No exceptions should be thrown
    }

    // The ladder re-checks retriability on every attempt: a 403 arriving mid-ladder ends it at once
    // instead of burning the remaining 5s + 10s attempts.
    @Test
    public void testRetryStopsWhenErrorBecomesNonRetriable() {
        ConnectionException retriable = new ConnectionException("Bad Gateway", 502, 0);
        ConnectionException nonRetriable = new ConnectionException("Forbidden", 403, 0);
        fake.failWith("testMethod", nonRetriable);

        retriableCallback.process(Result.fromException(retriable));

        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(mockCallback, timeout(5000)).process(captor.capture());

        assertThat(captor.getValue().getException(), is(equalTo(nonRetriable)));
        assertEquals(1, fake.count("testMethod"));
        fake.assertAllScripted();
    }

    // A stub handed out mid-ladder is terminal too: no further attempt is scheduled.
    @Test
    public void testRetryStopsWhenManagerBecomesNotInitialized() {
        ConnectionException retriable = new ConnectionException("Bad Gateway", 502, 0);
        NetworkModule.setRequestManager(null);

        retriableCallback.process(Result.fromException(retriable));

        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(mockCallback, timeout(5000)).process(captor.capture());

        assertThat(captor.getValue().getException().getMessage(), equalTo("SDK is not initialized"));
    }

    @Test
    public void testSeamNotInitializedErrorIsTerminal() {
        NetworkModule.setRequestManager(null);

        Result<String, NetworkException> notInitialized =
                NetworkModule.getRequestManager().sendRequestSync(mockRequest);

        retriableCallback.process(notInitialized);

        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(mockCallback, timeout(2000)).process(captor.capture());

        assertThat(captor.getValue().isSuccess(), is(false));
        assertThat(captor.getValue().getException().getMessage(), equalTo("SDK is not initialized"));
    }

    private JSONObject createSuccessResponse(String data) {
        try {
            JSONObject response = new JSONObject();
            response.put("status_code", 200);
            response.put("data", data);
            return response;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
