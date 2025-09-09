package com.pushwoosh.function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.RequestManagerMock;

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
    private RequestManagerMock requestManagerMock;
    private PushRequest<String> mockRequest;
    private Callback<String, NetworkException> mockCallback;
    private RetriableRequestCallback<String> retriableCallback;

    @Before
    public void setUp() throws Exception {
        // Reuse existing test infrastructure
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();

        requestManagerMock = platformTestManager.getRequestManager();
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

    ArgumentCaptor<Result<String, NetworkException>> captor =
            ArgumentCaptor.forClass(Result.class);
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
    ConnectionException connectionException = 
        new ConnectionException("Connection failed", 0, 0);
    Result<String, NetworkException> failureResult = Result.fromException(connectionException);

    // Mock successful retry response
    requestManagerMock.setResponse(
        createSuccessResponse("retry_success"), 
        mockRequest.getClass()
    );

    retriableCallback.process(failureResult);

    // Advance time to trigger first retry
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

    ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
    verify(mockCallback, timeout(2000)).process(captor.capture());

    assertThat(captor.getValue().isSuccess(), is(true));
}

@Test
public void testMaxRetriesExceeded() {
    ConnectionException connectionException = new ConnectionException("Connection failed", 0, 0);
    Result<String, NetworkException> failureResult = Result.fromException(connectionException);

    // Mock all retries to fail
    requestManagerMock.setException(connectionException, mockRequest.getClass());

    retriableCallback.process(failureResult);

    // Wait for all retries to complete (total time is 1 + 5 + 10 = 16 seconds)
    // Since RetriableRequestCallback uses a real ScheduledExecutorService, we need to wait for real time
    // or use a longer timeout to allow the retries to happen

    // Final callback should be called with last failure after all retries (allow 20 seconds)
    ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
    verify(mockCallback, timeout(20000)).process(captor.capture());

    assertThat(captor.getValue().isSuccess(), is(false));
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

@Test
public void testRequestManagerNull() {
    // Set NetworkModule requestManager to null
    NetworkModule.setRequestManager(null);

    ConnectionException connectionException = 
        new ConnectionException("Connection failed", 0, 0);
    Result<String, NetworkException> failureResult = Result.fromException(connectionException);

    retriableCallback.process(failureResult);

    ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

    ArgumentCaptor<Result<String, NetworkException>> captor =
            ArgumentCaptor.forClass(Result.class);
    verify(mockCallback, timeout(2000)).process(captor.capture());

    assertThat(captor.getValue().isSuccess(), is(false));
    assertThat(captor.getValue().getException().getMessage(),
            containsString("RequestManager is null"));
    
    // Restore RequestManager for other tests
    NetworkModule.setRequestManager(requestManagerMock);
}

@Test
public void testNeedToRetryMethod() {
    RetriableRequestCallback<String> callback = 
        new RetriableRequestCallback<>(mockCallback, mockRequest);
    
    // Test connection failure (both codes 0)
    ConnectionException connectionFailure = new ConnectionException("Connection failed", 0, 0);
    assertThat(callback.needToRetry(connectionFailure), is(true));
    
    // Test retriable HTTP status codes
    ConnectionException timeout = new ConnectionException("Timeout", 408, 0);
    assertThat(callback.needToRetry(timeout), is(true));
    
    ConnectionException tooManyRequests = new ConnectionException("Too Many", 429, 0);
    assertThat(callback.needToRetry(tooManyRequests), is(true));
    
    ConnectionException serverError = new ConnectionException("Server Error", 500, 0);
    assertThat(callback.needToRetry(serverError), is(true));
    
    ConnectionException badGateway = new ConnectionException("Bad Gateway", 502, 0);
    assertThat(callback.needToRetry(badGateway), is(true));
    
    ConnectionException serviceUnavailable = new ConnectionException("Service Unavailable", 503, 0);
    assertThat(callback.needToRetry(serviceUnavailable), is(true));
    
    ConnectionException gatewayTimeout = new ConnectionException("Gateway Timeout", 504, 0);
    assertThat(callback.needToRetry(gatewayTimeout), is(true));
    
    // Test non-retriable status codes
    ConnectionException badRequest = new ConnectionException("Bad Request", 400, 0);
    assertThat(callback.needToRetry(badRequest), is(false));
    
    ConnectionException notFound = new ConnectionException("Not Found", 404, 0);
    assertThat(callback.needToRetry(notFound), is(false));
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
