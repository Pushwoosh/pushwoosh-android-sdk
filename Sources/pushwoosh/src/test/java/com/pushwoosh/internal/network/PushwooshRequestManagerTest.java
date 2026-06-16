/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.internal.network;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.specific.TestDeviceSpecific;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.RepositoryTestManager;
import com.pushwoosh.testutil.CallbackWrapper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;
import org.skyscreamer.jsonassert.JSONAssert;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PushwooshRequestManagerTest {

    public static final int TIMEOUT_TEST = 10000;
    private RegistrationPrefs registrationPrefs;
    private MockWebServer server;
    private String requestUrl;

    // class under test
    private PushwooshRequestManager requestManager;
    private PushRegistrar pushRegistrarMock;

    private static class TestRequest extends PushRequest<String> {
        private final String param;
        private final String result;
        private JSONObject response;

        public TestRequest(String param, String result) {
            this.param = param;
            this.result = result;
        }

        public JSONObject getResponse() {
            return response;
        }

        @Override
        public String getMethod() {
            return "testMethod";
        }

        @NonNull @Override
        protected String getHwid() throws InterruptedException {
            return "test_hwid";
        }

        @Override
        protected void buildParams(JSONObject params) throws JSONException {
            params.put("param", this.param);
        }

        @Override
        public String parseResponse(@NonNull JSONObject response) throws JSONException {
            this.response = response;
            return result;
        }
    }

    private static class FlatPayloadTestRequest extends PushRequest<Void> {
        private final String value;

        FlatPayloadTestRequest(String value) {
            this.value = value;
        }

        @Override
        public String getMethod() {
            return "flatMethod";
        }

        @Override
        public boolean shouldWrapRequest() {
            return false;
        }

        @NonNull @Override
        protected String getHwid() throws InterruptedException {
            return "test_hwid";
        }

        @Override
        protected JSONObject getParams() throws JSONException, InterruptedException {
            JSONObject params = new JSONObject();
            params.put("hwid", getHwid());
            params.put("custom", value);
            return params;
        }
    }

    private static class TestBadParamsRequest extends PushRequest<Void> {

        @Override
        public String getMethod() {
            return "testBadParams";
        }

        @Override
        protected void buildParams(JSONObject params) throws JSONException {
            throw new JSONException("test invalid params");
        }

        @NonNull @Override
        protected String getHwid() throws InterruptedException {
            return "test_hwid";
        }
    }

    private static class TestBadResponseRequest extends PushRequest<Void> {

        @Override
        public String getMethod() {
            return "testBadResponse";
        }

        @Override
        public Void parseResponse(@NonNull JSONObject response) throws JSONException {
            throw new JSONException("test invalid response");
        }

        @NonNull @Override
        protected String getHwid() throws InterruptedException {
            return "test_hwid";
        }
    }

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;

        server = new MockWebServer();
        server.start();
        HttpUrl baseUrl = server.url("/");
        requestUrl = baseUrl.url().toString();

        Config configMock = MockConfig.createMock();
        when(configMock.getRequestUrl()).thenReturn(requestUrl);

        AndroidPlatformModule.init(RuntimeEnvironment.application, true);

        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(configMock, mock(DeviceRegistrar.class));
        RepositoryModule.setRegistrationPreferences(registrationPrefs);

        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        Mockito.when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        requestManager = new PushwooshRequestManager(registrationPrefs, serverCommunicationManager, false);
        // baseRequestUrl is no longer snapshotted in the constructor; existing tests need an
        // explicit prime to point at the mock server.
        requestManager.updateBaseUrl(requestUrl);

        pushRegistrarMock = mock(PushRegistrar.class);

        new DeviceSpecificProvider.Builder()
                .setDeviceSpecific(new TestDeviceSpecific(pushRegistrarMock))
                .build(true);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        RepositoryTestManager.destroyRegistrationPrefs(registrationPrefs);
        RepositoryModule.setRegistrationPreferences(null);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestSync() throws Exception {
        TestRequest testRequest = new TestRequest("testParam", "testResult");
        server.enqueue(
                new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
        Result<String, NetworkException> result = requestManager.sendRequestSync(testRequest);

        assertThat(result.isSuccess(), is(true));
        assertThat(result.getData(), is(equalTo("testResult")));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/testMethod")));

        JSONObject requestParams = new JSONObject(request.getBody().readUtf8()).getJSONObject("request");
        assertThat(requestParams.getString("param"), is(equalTo("testParam")));
        assertThat(requestParams.getString("application"), is(equalTo(MockConfig.APP_ID)));
        assertThat(requestParams.has("v"), is(true));
        assertThat(requestParams.has("hwid"), is(true));
        assertThat(requestParams.has("device_type"), is(true));

        JSONObject testResponse = testRequest.getResponse();
        JSONAssert.assertEquals(new JSONObject("{\"result\" : \"test output\"}"), testResponse, true);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestSyncBlockedByRemoveAllDevice() throws Exception {
        TestRequest testRequest = new TestRequest("testParam", "testResult");
        registrationPrefs.removeAllDeviceData().set(true);
        server.enqueue(
                new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
        Result<String, NetworkException> result = requestManager.sendRequestSync(testRequest);

        assertThat(result.isSuccess(), is(false));
        NetworkException exception = result.getException();
        assertThat(
                exception.getMessage(), is("Device data was removed from Pushwoosh and all interactions were stopped"));
        Assert.assertEquals(0, server.getRequestCount());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void baseUrlSwitch() throws Exception {
        String body = String.format(
                "{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200, \"base_url\" : \"%s\"}",
                requestUrl + "newUrl/");
        server.enqueue(new MockResponse().setBody(body));
        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(true));
        assertThat(result.getData(), is(equalTo("testResult")));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/testMethod")));

        //		assertThat(registrationPrefs.baseUrl().get(), is(equalTo(requestUrl + "/newUrl/")));

        assertEquals(requestUrl + "newUrl/", registrationPrefs.baseUrl().get());

        server.enqueue(new MockResponse().setBody(body));
        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));
        request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/newUrl/testMethod")));
    }

    // Server-pushed base_url without trailing slash must be normalized so that subsequent
    // request URL composition (base + method) produces a syntactically correct URL.
    @Test(timeout = TIMEOUT_TEST)
    public void baseUrlSwitch_serverUrlWithoutTrailingSlash_isNormalized() throws Exception {
        String body = String.format(
                "{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200, \"base_url\" : \"%s\"}",
                requestUrl + "newUrl");
        server.enqueue(new MockResponse().setBody(body));
        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));
        server.takeRequest();

        assertEquals(requestUrl + "newUrl/", registrationPrefs.baseUrl().get());

        server.enqueue(new MockResponse().setBody(body));
        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));
        RecordedRequest second = server.takeRequest();
        assertThat(second.getPath(), is(equalTo("/newUrl/testMethod")));
    }

    // Server-pushed malformed base_url is rejected; in-memory and persisted state stay intact.
    @Test(timeout = TIMEOUT_TEST)
    public void baseUrlSwitch_serverMalformedUrl_isIgnored() throws Exception {
        String body =
                "{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200, \"base_url\" : \"not-a-url\"}";
        server.enqueue(new MockResponse().setBody(body));
        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));
        server.takeRequest();

        // baseUrl in prefs is whatever updateBaseUrl(requestUrl) put there during setUp.
        assertEquals(requestUrl, registrationPrefs.baseUrl().get());

        server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"x\"}, \"status_code\" : 200}"));
        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));
        RecordedRequest second = server.takeRequest();
        assertThat(second.getPath(), is(equalTo("/testMethod")));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void badStatusCode() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}")
                .setResponseCode(503));
        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(false));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/testMethod")));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void badPushwooshStatusCode() throws Exception {
        server.enqueue(
                new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 201}"));
        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(false));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/testMethod")));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void noPushwooshStatusCode() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}}"));
        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(false));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/testMethod")));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void badJsonResponse() throws Exception {
        server.enqueue(new MockResponse().setBody("[]"));
        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(false));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/testMethod")));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void noResponseKey() throws Exception {
        TestRequest testRequest = new TestRequest("testParam", "testResult");
        server.enqueue(new MockResponse().setBody("{\"status_code\" : 200}"));
        Result<String, NetworkException> result = requestManager.sendRequestSync(testRequest);

        assertThat(result.isSuccess(), is(true));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/testMethod")));
        JSONAssert.assertEquals(new JSONObject(), testRequest.getResponse(), true);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendBadParamsRequestSync() throws Exception {
        TestBadParamsRequest testRequest = new TestBadParamsRequest();
        server.enqueue(new MockResponse().setBody("{\"status_code\" : 200, \"response\" : null}"));
        Result<Void, NetworkException> result = requestManager.sendRequestSync(testRequest);

        assertThat(result.isSuccess(), is(false));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendBadResponseRequestSync() throws Exception {
        TestBadResponseRequest testRequest = new TestBadResponseRequest();
        server.enqueue(new MockResponse().setBody("{\"status_code\" : 200, \"response\" : null}"));
        Result<Void, NetworkException> result = requestManager.sendRequestSync(testRequest);

        assertThat(result.isSuccess(), is(false));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/testBadResponse")));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestBlockedByRemoveAllDevice() throws Exception {
        registrationPrefs.removeAllDeviceData().set(true);
        TestRequest testRequest = new TestRequest("testParam", "testResult");
        server.enqueue(
                new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
        Callback<String, NetworkException> callback = CallbackWrapper.spy();
        ArgumentCaptor<Result<String, NetworkException>> callbackCaptor = ArgumentCaptor.forClass(Result.class);

        requestManager.sendRequest(testRequest, callback);
        Thread.sleep(100); // wait for background executor
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(callback).process(callbackCaptor.capture());
        Result<String, NetworkException> result = callbackCaptor.getValue();
        assertThat(result.isSuccess(), is(false));
        NetworkException exception = result.getException();
        assertThat(
                exception.getMessage(), is("Device data was removed from Pushwoosh and all interactions were stopped"));
        assertThat(server.getRequestCount(), is(0));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestWithoutCallbackBlockedByRemoveAllDevice() throws Exception {
        registrationPrefs.removeAllDeviceData().set(true);
        TestRequest testRequest = new TestRequest("testParam", "testResult");
        server.enqueue(
                new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));

        requestManager.sendRequest(testRequest);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertThat(server.getRequestCount(), is(0));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendBadRequestWithoutCallback() throws Exception {
        TestBadResponseRequest testRequest = new TestBadResponseRequest();
        server.enqueue(
                new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));

        requestManager.sendRequest(testRequest);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/testBadResponse")));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestSync_nullBaseUrl_noReverseProxy_returnsBlocked() throws Exception {
        // Construct a fresh manager — baseRequestUrl is null, no reverse proxy.
        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        Mockito.when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        PushwooshRequestManager bare =
                new PushwooshRequestManager(registrationPrefs, serverCommunicationManager, false);

        Result<String, NetworkException> result = bare.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getException().getMessage(), is(equalTo("Base URL is not configured")));
        assertThat(server.getRequestCount(), is(0));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestSync_nullBaseUrl_withReverseProxy_proceeds() throws Exception {
        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        Mockito.when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        PushwooshRequestManager bare =
                new PushwooshRequestManager(registrationPrefs, serverCommunicationManager, false);
        bare.setReverseProxyUrl(requestUrl, null);

        server.enqueue(
                new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
        Result<String, NetworkException> result = bare.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(true));
        assertThat(server.getRequestCount(), is(1));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestSync_afterUpdateBaseUrl_usesUpdatedUrl() throws Exception {
        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        Mockito.when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        PushwooshRequestManager bare =
                new PushwooshRequestManager(registrationPrefs, serverCommunicationManager, false);
        bare.updateBaseUrl(requestUrl);

        server.enqueue(
                new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
        Result<String, NetworkException> result = bare.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(true));
        RecordedRequest received = server.takeRequest();
        assertThat(received.getPath(), is(equalTo("/testMethod")));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestWithCallback_callbackIsInvoked() throws Exception {
        TestRequest testRequest = new TestRequest("testParam", "testResult");
        server.enqueue(
                new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
        Callback<String, NetworkException> callback = CallbackWrapper.spy();
        ArgumentCaptor<Result<String, NetworkException>> callbackCaptor = ArgumentCaptor.forClass(Result.class);

        requestManager.sendRequest(testRequest, callback);

        // Wait for background executor to complete and post callback to main thread
        server.takeRequest(); // blocks until server receives request
        Thread.sleep(100); // small delay for callback to be posted to main looper

        // Process main thread tasks (callback invocation)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(callback).process(callbackCaptor.capture());
        Result<String, NetworkException> result = callbackCaptor.getValue();
        assertThat(result.isSuccess(), is(true));
        assertThat(result.getData(), is(equalTo("testResult")));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void reverseProxy_active_requestGoesToProxyUrl() throws Exception {
        MockWebServer proxyServer = new MockWebServer();
        proxyServer.start();
        try {
            String proxyUrl = proxyServer.url("/").toString();
            requestManager.setReverseProxyUrl(proxyUrl, null);
            proxyServer.enqueue(
                    new MockResponse().setBody("{\"response\" : {\"result\" : \"x\"}, \"status_code\" : 200}"));

            Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("p", "r"));

            assertThat(result.isSuccess(), is(true));
            assertThat(server.getRequestCount(), is(0));
            assertThat(proxyServer.getRequestCount(), is(1));
            RecordedRequest rec = proxyServer.takeRequest();
            assertThat(rec.getPath(), is(equalTo("/testMethod")));
        } finally {
            proxyServer.shutdown();
        }
    }

    @Test(timeout = TIMEOUT_TEST)
    public void reverseProxy_active_customHeadersApplied() throws Exception {
        MockWebServer proxyServer = new MockWebServer();
        proxyServer.start();
        try {
            String proxyUrl = proxyServer.url("/").toString();
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Custom-Auth", "abc123");
            headers.put("X-Tenant", "tenant-42");
            requestManager.setReverseProxyUrl(proxyUrl, headers);
            proxyServer.enqueue(
                    new MockResponse().setBody("{\"response\" : {\"result\" : \"x\"}, \"status_code\" : 200}"));

            requestManager.sendRequestSync(new TestRequest("p", "r"));

            RecordedRequest rec = proxyServer.takeRequest();
            assertThat(rec.getHeader("X-Custom-Auth"), is("abc123"));
            assertThat(rec.getHeader("X-Tenant"), is("tenant-42"));
            assertThat(rec.getHeader("Authorization"), startsWith("Token "));
            assertThat(rec.getHeader("Content-Type"), containsString("application/json"));
        } finally {
            proxyServer.shutdown();
        }
    }

    // Reverse proxy active: server-pushed base_url must NOT cause rotation, even though
    // the equals(baseUrl, baseRequestUrl) part of the rotation invariant is satisfied.
    @Test(timeout = TIMEOUT_TEST)
    public void reverseProxy_active_rotationSuppressed_evenWithBaseUrlInResponse() throws Exception {
        MockWebServer proxyServer = new MockWebServer();
        proxyServer.start();
        try {
            String proxyUrl = proxyServer.url("/").toString();
            requestManager.setReverseProxyUrl(proxyUrl, null);
            String body = "{\"status_code\":200,\"response\":{},\"base_url\":\"" + requestUrl + "rotated/\"}";
            proxyServer.enqueue(new MockResponse().setBody(body));
            String before = registrationPrefs.baseUrl().get();

            requestManager.sendRequestSync(new TestRequest("p", "r"));

            assertEquals(before, registrationPrefs.baseUrl().get());
        } finally {
            proxyServer.shutdown();
        }
    }

    @Test(timeout = TIMEOUT_TEST)
    public void reverseProxy_required_butNotConfigured_blocks() throws Exception {
        ServerCommunicationManager scm = mock(ServerCommunicationManager.class);
        when(scm.isServerCommunicationAllowed()).thenReturn(true);
        PushwooshRequestManager strict = new PushwooshRequestManager(registrationPrefs, scm, true);
        strict.updateBaseUrl(requestUrl);

        Result<String, NetworkException> result = strict.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getException().getMessage(), is("Reverse proxy is required but not configured"));
        assertThat(server.getRequestCount(), is(0));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void callerProvidedBaseUrl_differsFromCurrent_rotationSkipped() throws Exception {
        String otherUrl = server.url("/other/").toString();
        String body = "{\"status_code\":200,\"response\":{},\"base_url\":\"" + requestUrl + "rotated/\"}";
        server.enqueue(new MockResponse().setBody(body));
        String before = registrationPrefs.baseUrl().get();

        Callback<String, NetworkException> callback = CallbackWrapper.spy();
        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
        requestManager.sendRequest(new TestRequest("p", "r"), otherUrl, callback);
        server.takeRequest();
        Thread.sleep(100);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verify(callback).process(captor.capture());

        // Caller's baseUrl != sticky baseRequestUrl -> equals is false -> rotation skipped
        assertEquals(before, registrationPrefs.baseUrl().get());
    }

    // Captures brittle equals-based rotation contract: caller passing the exact same URL
    // as baseRequestUrl satisfies equals=true and rotation kicks in. The planned `Endpoint`
    // refactor must keep this behavior identical.
    @Test(timeout = TIMEOUT_TEST)
    public void callerProvidedBaseUrl_sameAsCurrent_rotationApplies() throws Exception {
        String body = "{\"status_code\":200,\"response\":{},\"base_url\":\"" + requestUrl + "rotated/\"}";
        server.enqueue(new MockResponse().setBody(body));

        Callback<String, NetworkException> callback = CallbackWrapper.spy();
        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);
        requestManager.sendRequest(new TestRequest("p", "r"), requestUrl, callback);
        server.takeRequest();
        Thread.sleep(100);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verify(callback).process(captor.capture());

        assertEquals(requestUrl + "rotated/", registrationPrefs.baseUrl().get());
    }

    // HTTP 4xx with empty body: Manager synthesizes envelope and overloads pushwooshStatus
    // with the HTTP-level statusCode. Both codes equal the HTTP value.
    @Test(timeout = TIMEOUT_TEST)
    public void error_4xx_emptyBody_returnsConnectionExceptionWithCodes() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404));

        Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        NetworkException ex = result.getException();
        assertThat(ex, instanceOf(ConnectionException.class));
        ConnectionException ce = (ConnectionException) ex;
        assertThat(ce.getStatusCode(), is(404));
        assertThat(ce.getPushwooshStatusCode(), is(404));
        assertThat(ex.getMessage(), containsString("\"status_code\":404"));
    }

    // HTTP 5xx with empty body: same synthetic envelope as 4xx — both codes = HTTP value.
    @Test(timeout = TIMEOUT_TEST)
    public void error_5xx_emptyBody_returnsConnectionExceptionWithCodes() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));

        Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        NetworkException ex = result.getException();
        assertThat(ex, instanceOf(ConnectionException.class));
        ConnectionException ce = (ConnectionException) ex;
        assertThat(ce.getStatusCode(), is(503));
        assertThat(ce.getPushwooshStatusCode(), is(503));
        assertThat(ex.getMessage(), containsString("\"status_code\":503"));
    }

    // HTTP 4xx with a parseable JSON envelope in body: body overrides the synthetic envelope.
    // pushwooshStatusCode is taken from the body's status_code field, not the HTTP status.
    @Test(timeout = TIMEOUT_TEST)
    public void error_4xx_withParseableBody_bodyOverridesSynthetic() throws Exception {
        server.enqueue(
                new MockResponse().setResponseCode(404).setBody("{\"status_code\":210,\"status_message\":\"Quota\"}"));

        Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        NetworkException ex = result.getException();
        assertThat(ex, instanceOf(ConnectionException.class));
        ConnectionException ce = (ConnectionException) ex;
        assertThat(ce.getStatusCode(), is(404));
        assertThat(ce.getPushwooshStatusCode(), is(210));
        assertThat(ex.getMessage(), containsString("\"status_code\":210"));
        assertThat(ex.getMessage(), containsString("\"status_message\":\"Quota\""));
    }

    // HTTP 4xx with a parseable JSON body that lacks status_code: body still overrides the synthetic
    // envelope for the message, but pushwooshStatusCode stays = HTTP status (synthetic survives the
    // swallowed JSONException from envelope.getInt("status_code")).
    @Test(timeout = TIMEOUT_TEST)
    public void error_4xx_withParseableBodyNoStatusCode_messageFromBodyCodesFromSynthetic() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"detail\":\"not found\"}"));

        Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        NetworkException ex = result.getException();
        assertThat(ex, instanceOf(ConnectionException.class));
        ConnectionException ce = (ConnectionException) ex;
        assertThat(ce.getStatusCode(), is(404));
        assertThat(ce.getPushwooshStatusCode(), is(404));
        assertThat(ex.getMessage(), containsString("\"detail\":\"not found\""));
    }

    // JSONException from request.parseResponse() gets narrowed to ConnectionException
    // on the fail-path. Codes reflect the (successful) HTTP/Pushwoosh response.
    @Test(timeout = TIMEOUT_TEST)
    public void parseResponse_throwsJsonException_resultIsConnectionException() throws Exception {
        TestBadResponseRequest req = new TestBadResponseRequest();
        server.enqueue(new MockResponse().setBody("{\"status_code\":200,\"response\":{}}"));

        Result<Void, NetworkException> result = requestManager.sendRequestSync(req);

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getException(), instanceOf(ConnectionException.class));
        ConnectionException ce = (ConnectionException) result.getException();
        assertThat(ce.getStatusCode(), is(200));
        assertThat(ce.getPushwooshStatusCode(), is(200));
    }

    @Test(timeout = TIMEOUT_TEST)
    public void serverCommunicationStopped_returnsBlocked() throws Exception {
        ServerCommunicationManager scm = mock(ServerCommunicationManager.class);
        when(scm.isServerCommunicationAllowed()).thenReturn(false);
        PushwooshRequestManager m = new PushwooshRequestManager(registrationPrefs, scm, false);
        m.updateBaseUrl(requestUrl);

        Result<String, NetworkException> result = m.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        assertThat(
                result.getException().getMessage(),
                is("Server communication stopped. Call Pushwoosh.startServerCommunication() to resume"));
        assertThat(server.getRequestCount(), is(0));
    }

    // IOException on connect (closed port) is narrowed to ConnectionException with codes = 0.
    // Critical contract before extract HttpTransport.
    @Test(timeout = TIMEOUT_TEST)
    public void connectFails_returnsConnectionException() throws Exception {
        MockWebServer dead = new MockWebServer();
        dead.start();
        String deadUrl = dead.url("/").toString();
        dead.shutdown(); // port closed — connect will fail

        ServerCommunicationManager scm = mock(ServerCommunicationManager.class);
        when(scm.isServerCommunicationAllowed()).thenReturn(true);
        PushwooshRequestManager m = new PushwooshRequestManager(registrationPrefs, scm, false);
        m.updateBaseUrl(deadUrl);

        Result<String, NetworkException> result = m.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getException(), instanceOf(ConnectionException.class));
        ConnectionException ce = (ConnectionException) result.getException();
        assertThat(ce.getStatusCode(), is(0));
        assertThat(ce.getPushwooshStatusCode(), is(0));
    }

    // A server that accepts the connection then goes silent blocks read() forever without a read
    // timeout, capturing the single network thread. The silent server is a ServerSocket we never
    // accept() on: the OS completes the TCP handshake so connect succeeds, but read() never gets a
    // response and hits the timeout. With the timeout set the request fails fast as
    // ConnectionException(0/0) — the exact RetriableRequestCallback retry trigger.
    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestSyncTimesOutWhenServerIsSilent() throws Exception {
        int originalReadTimeout = HttpTransport.readTimeoutMs;
        ServerSocket silentServer = new ServerSocket(0);
        try {
            HttpTransport.readTimeoutMs = 200;
            requestManager.updateBaseUrl("http://127.0.0.1:" + silentServer.getLocalPort() + "/");

            Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("p", "r"));

            assertThat(result.isSuccess(), is(false));
            assertThat(result.getException(), instanceOf(ConnectionException.class));
            ConnectionException ce = (ConnectionException) result.getException();
            assertThat(ce.getStatusCode(), is(0));
            assertThat(ce.getPushwooshStatusCode(), is(0));
        } finally {
            HttpTransport.readTimeoutMs = originalReadTimeout;
            silentServer.close();
        }
    }

    // HTTP 200 + empty body: Content-Length is 0 → Manager skips body parsing →
    // pushwooshStatus stays at 0 → fail-path with statusCode=200, pushwooshStatusCode=0.
    // Snapshots the `getContentLength() != 0` boundary at line 251 of PushwooshRequestManager.
    @Test(timeout = TIMEOUT_TEST)
    public void httpOk_contentLengthZero_returnsFailWithPushwooshStatusZero() throws Exception {
        server.enqueue(new MockResponse()); // default: HTTP 200, empty body, Content-Length: 0

        Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getException(), instanceOf(ConnectionException.class));
        ConnectionException ce = (ConnectionException) result.getException();
        assertThat(ce.getStatusCode(), is(200));
        assertThat(ce.getPushwooshStatusCode(), is(0));
    }

    // Envelope contract: default shouldWrapRequest()=true wraps payload in {"request": ...}.
    @Test(timeout = TIMEOUT_TEST)
    public void shouldWrapRequest_true_payloadWrappedInRequestKey() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"status_code\":200,\"response\":{}}"));

        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        RecordedRequest rec = server.takeRequest();
        JSONObject body = new JSONObject(rec.getBody().readUtf8());
        assertThat(body.has("request"), is(true));
        JSONObject inner = body.getJSONObject("request");
        assertThat(inner.getString("param"), is(equalTo("testParam")));
        assertThat(inner.has("hwid"), is(true));
    }

    // Envelope contract: shouldWrapRequest()=false sends a flat payload — no "request" wrapper.
    // This is the contract for the tracking endpoint (setMADID).
    @Test(timeout = TIMEOUT_TEST)
    public void shouldWrapRequest_false_payloadIsFlat() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"status_code\":200,\"response\":{}}"));

        requestManager.sendRequestSync(new FlatPayloadTestRequest("v1"));

        RecordedRequest rec = server.takeRequest();
        JSONObject body = new JSONObject(rec.getBody().readUtf8());
        assertThat(body.has("request"), is(false));
        assertThat(body.getString("custom"), is(equalTo("v1")));
        assertThat(body.getString("hwid"), is(equalTo("test_hwid")));
    }

    // Wire format in non-proxy mode: Authorization, Content-Type, Content-Length.
    // Existing sendRequestSync test verifies request body shape; this one asserts
    // headers and that Content-Length matches the actual request body size.
    @Test(timeout = TIMEOUT_TEST)
    public void wireFormat_normalMode_authHeadersAndContentLength() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"status_code\":200,\"response\":{}}"));

        requestManager.sendRequestSync(new TestRequest("p", "r"));

        RecordedRequest rec = server.takeRequest();
        assertThat(rec.getHeader("Authorization"), startsWith("Token "));
        assertThat(rec.getHeader("Content-Type"), containsString("application/json"));
        String contentLength = rec.getHeader("Content-Length");
        assertThat(contentLength, is(notNullValue()));
        assertEquals((long) Integer.parseInt(contentLength), rec.getBodySize());
    }

    // Verifies that HTTP status 399 (just below the error range) is not classified as an error.
    @Test(timeout = TIMEOUT_TEST)
    public void testIsErrorResponseCode_399_returnsFalse() {
        assertFalse(PushwooshRequestManager.isErrorResponseCode(399));
    }

    // Verifies that HTTP status 400 (lower error boundary) is classified as an error.
    @Test(timeout = TIMEOUT_TEST)
    public void testIsErrorResponseCode_400_returnsTrue() {
        assertTrue(PushwooshRequestManager.isErrorResponseCode(400));
    }

    // Verifies that HTTP status 599 (upper error boundary, inclusive) is classified as an error.
    @Test(timeout = TIMEOUT_TEST)
    public void testIsErrorResponseCode_599_returnsTrue() {
        assertTrue(PushwooshRequestManager.isErrorResponseCode(599));
    }

    // Verifies that HTTP status 600 (just above the error range) is not classified as an error.
    @Test(timeout = TIMEOUT_TEST)
    public void testIsErrorResponseCode_600_returnsFalse() {
        assertFalse(PushwooshRequestManager.isErrorResponseCode(600));
    }

    // Verifies that updateBaseUrl returns true when RegistrationPrefs accepts and normalizes the URL.
    @Test(timeout = TIMEOUT_TEST)
    public void testUpdateBaseUrl_returnsTrue_whenNormalizedNonNull() {
        RegistrationPrefs prefsMock = mock(RegistrationPrefs.class);
        when(prefsMock.updateBaseUrl("https://valid")).thenReturn("https://valid");
        ServerCommunicationManager scm = mock(ServerCommunicationManager.class);
        PushwooshRequestManager manager = new PushwooshRequestManager(prefsMock, scm, false);

        assertTrue(manager.updateBaseUrl("https://valid"));
    }

    // Verifies that updateBaseUrl returns false when RegistrationPrefs rejects the URL (returns null).
    @Test(timeout = TIMEOUT_TEST)
    public void testUpdateBaseUrl_returnsFalse_whenNormalizedNull() {
        RegistrationPrefs prefsMock = mock(RegistrationPrefs.class);
        when(prefsMock.updateBaseUrl("bad")).thenReturn(null);
        ServerCommunicationManager scm = mock(ServerCommunicationManager.class);
        PushwooshRequestManager manager = new PushwooshRequestManager(prefsMock, scm, false);

        assertFalse(manager.updateBaseUrl("bad"));
    }
}
