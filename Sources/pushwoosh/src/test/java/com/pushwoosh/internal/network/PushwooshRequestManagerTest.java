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
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Looper;

import androidx.annotation.NonNull;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.specific.TestDeviceSpecific;
import com.pushwoosh.internal.utils.BackgroundExecutor;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PushwooshRequestManagerTest {

    public static final int TIMEOUT_TEST = 10000;
    private static final long AWAIT_MS = 2000;
    private static final String BASE_URL = "https://requests.test.pushwoosh.local/";
    private static final String OK_BODY = "{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}";

    private RegistrationPrefs registrationPrefs;
    private FakeHttpTransport transport;

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

        Config configMock = MockConfig.createMock();
        when(configMock.getRequestUrl()).thenReturn(BASE_URL);

        AndroidPlatformModule.init(RuntimeEnvironment.application, true);

        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(configMock, mock(DeviceRegistrar.class));
        RepositoryModule.setRegistrationPreferences(registrationPrefs);

        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        transport = new FakeHttpTransport();
        requestManager = new PushwooshRequestManager(registrationPrefs, serverCommunicationManager, false, transport);
        // baseRequestUrl is not snapshotted in the constructor; prime it explicitly.
        requestManager.updateBaseUrl(BASE_URL);

        pushRegistrarMock = mock(PushRegistrar.class);

        new DeviceSpecificProvider.Builder()
                .setDeviceSpecific(new TestDeviceSpecific(pushRegistrarMock))
                .build(true);
    }

    @After
    public void tearDown() throws Exception {
        RepositoryTestManager.destroyRegistrationPrefs(registrationPrefs);
        RepositoryModule.setRegistrationPreferences(null);
        transport.assertAllScripted();
    }

    // The network executor is single-threaded, so a barrier task cannot run before the request task
    // ahead of it finished — by then the main-thread callback is already queued and safe to pump.
    private static void awaitAsyncRoundTrip() throws InterruptedException {
        CountDownLatch drained = new CountDownLatch(1);
        BackgroundExecutor.network(drained::countDown);
        assertTrue("network executor did not drain", drained.await(AWAIT_MS, TimeUnit.MILLISECONDS));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    private static ServerCommunicationManager allowingCommunication() {
        ServerCommunicationManager scm = mock(ServerCommunicationManager.class);
        when(scm.isServerCommunicationAllowed()).thenReturn(true);
        return scm;
    }

    // The only test on a real socket: manager + real HttpTransport + MockWebServer, one successful
    // round trip. Guards the fake from drifting away from the real transport contract.
    @Test(timeout = TIMEOUT_TEST)
    public void smoke_realTransport_roundTripsPayloadAndEnvelope() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            PushwooshRequestManager real =
                    new PushwooshRequestManager(registrationPrefs, allowingCommunication(), false, new HttpTransport());
            real.updateBaseUrl(server.url("/").toString());
            server.enqueue(new MockResponse().setBody(OK_BODY));
            TestRequest testRequest = new TestRequest("testParam", "testResult");

            Result<String, NetworkException> result = real.sendRequestSync(testRequest);

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
            JSONAssert.assertEquals(new JSONObject("{\"result\" : \"test output\"}"), testRequest.getResponse(), true);
        } finally {
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestSyncBlockedByRemoveAllDevice() throws Exception {
        registrationPrefs.removeAllDeviceData().set(true);

        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(false));
        assertThat(
                result.getException().getMessage(),
                is("Device data was removed from Pushwoosh and all interactions were stopped"));
        assertEquals(0, transport.count());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void baseUrlSwitch() throws Exception {
        String body = String.format(
                "{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200, \"base_url\" : \"%s\"}",
                BASE_URL + "newUrl/");
        transport.respondWith(200, body).respondWith(200, body);

        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(true));
        assertThat(result.getData(), is(equalTo("testResult")));
        assertEquals(BASE_URL, transport.last().url);
        assertEquals("testMethod", transport.last().method);
        assertEquals(BASE_URL + "newUrl/", registrationPrefs.baseUrl().get());

        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertEquals(BASE_URL + "newUrl/", transport.last().url);
    }

    // Server-pushed base_url without trailing slash must be normalized so that subsequent
    // request URL composition (base + method) produces a syntactically correct URL.
    @Test(timeout = TIMEOUT_TEST)
    public void baseUrlSwitch_serverUrlWithoutTrailingSlash_isNormalized() throws Exception {
        String body = String.format(
                "{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200, \"base_url\" : \"%s\"}",
                BASE_URL + "newUrl");
        transport.respondWith(200, body).respondWith(200, body);

        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertEquals(BASE_URL + "newUrl/", registrationPrefs.baseUrl().get());

        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertEquals(BASE_URL + "newUrl/", transport.last().url);
    }

    // Server-pushed malformed base_url is rejected; in-memory and persisted state stay intact.
    @Test(timeout = TIMEOUT_TEST)
    public void baseUrlSwitch_serverMalformedUrl_isIgnored() throws Exception {
        transport
                .respondWith(
                        200,
                        "{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200,"
                                + " \"base_url\" : \"not-a-url\"}")
                .respondWith(200, "{\"response\" : {\"result\" : \"x\"}, \"status_code\" : 200}");

        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertEquals(BASE_URL, registrationPrefs.baseUrl().get());

        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertEquals(BASE_URL, transport.last().url);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void badStatusCode() throws Exception {
        transport.respondWith(503, "Service Unavailable", OK_BODY);

        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(false));
        assertEquals("testMethod", transport.last().method);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void badPushwooshStatusCode() throws Exception {
        transport.respondWith(200, "{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 201}");

        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(false));
        assertEquals("testMethod", transport.last().method);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void noPushwooshStatusCode() throws Exception {
        transport.respondWith(200, "{\"response\" : {\"result\" : \"test output\"}}");

        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(false));
        assertEquals("testMethod", transport.last().method);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void badJsonResponse() throws Exception {
        transport.respondWith(200, "[]");

        Result<String, NetworkException> result =
                requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        assertThat(result.isSuccess(), is(false));
        assertEquals("testMethod", transport.last().method);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void noResponseKey() throws Exception {
        TestRequest testRequest = new TestRequest("testParam", "testResult");
        transport.respondWith(200, "{\"status_code\" : 200}");

        Result<String, NetworkException> result = requestManager.sendRequestSync(testRequest);

        assertThat(result.isSuccess(), is(true));
        assertEquals("testMethod", transport.last().method);
        JSONAssert.assertEquals(new JSONObject(), testRequest.getResponse(), true);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendBadParamsRequestSync() throws Exception {
        Result<Void, NetworkException> result = requestManager.sendRequestSync(new TestBadParamsRequest());

        assertThat(result.isSuccess(), is(false));
        assertEquals(0, transport.count());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendBadResponseRequestSync() throws Exception {
        transport.respondWith(200, "{\"status_code\" : 200, \"response\" : null}");

        Result<Void, NetworkException> result = requestManager.sendRequestSync(new TestBadResponseRequest());

        assertThat(result.isSuccess(), is(false));
        assertEquals("testBadResponse", transport.last().method);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestBlockedByRemoveAllDevice() throws Exception {
        registrationPrefs.removeAllDeviceData().set(true);
        Callback<String, NetworkException> callback = CallbackWrapper.spy();
        ArgumentCaptor<Result<String, NetworkException>> callbackCaptor = ArgumentCaptor.forClass(Result.class);

        requestManager.sendRequest(new TestRequest("testParam", "testResult"), callback);
        awaitAsyncRoundTrip();

        verify(callback).process(callbackCaptor.capture());
        Result<String, NetworkException> result = callbackCaptor.getValue();
        assertThat(result.isSuccess(), is(false));
        assertThat(
                result.getException().getMessage(),
                is("Device data was removed from Pushwoosh and all interactions were stopped"));
        assertEquals(0, transport.count());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestWithoutCallbackBlockedByRemoveAllDevice() throws Exception {
        registrationPrefs.removeAllDeviceData().set(true);

        requestManager.sendRequest(new TestRequest("testParam", "testResult"));
        awaitAsyncRoundTrip();

        assertEquals(0, transport.count());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendBadRequestWithoutCallback() throws Exception {
        transport.respondWith(200, OK_BODY);

        requestManager.sendRequest(new TestBadResponseRequest());
        awaitAsyncRoundTrip();

        assertEquals("testBadResponse", transport.last().method);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestWithCallback_callbackIsInvoked() throws Exception {
        transport.respondWith(200, OK_BODY);
        Callback<String, NetworkException> callback = CallbackWrapper.spy();
        ArgumentCaptor<Result<String, NetworkException>> callbackCaptor = ArgumentCaptor.forClass(Result.class);

        requestManager.sendRequest(new TestRequest("testParam", "testResult"), callback);
        awaitAsyncRoundTrip();

        verify(callback).process(callbackCaptor.capture());
        Result<String, NetworkException> result = callbackCaptor.getValue();
        assertThat(result.isSuccess(), is(true));
        assertThat(result.getData(), is(equalTo("testResult")));
    }

    // Verifies that the callback lands on the main thread, which is the @MainThread contract every
    // public SDK callback inherits. verify(callback) alone stays green even when the callback is
    // invoked straight from the network executor, so the delivering looper is captured explicitly.
    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestWithCallback_callbackDeliveredOnMainThread() throws Exception {
        transport.respondWith(200, OK_BODY);
        AtomicReference<Looper> callbackLooper = new AtomicReference<>();

        requestManager.sendRequest(new TestRequest("p", "r"), result -> callbackLooper.set(Looper.myLooper()));
        awaitAsyncRoundTrip();

        assertEquals(Looper.getMainLooper(), callbackLooper.get());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestSync_nullBaseUrl_noReverseProxy_returnsBlocked() throws Exception {
        // Fresh manager — baseRequestUrl is null, no reverse proxy.
        PushwooshRequestManager bare =
                new PushwooshRequestManager(registrationPrefs, allowingCommunication(), false, transport);

        Result<String, NetworkException> result = bare.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getException().getMessage(), is(equalTo("Base URL is not configured")));
        assertEquals(0, transport.count());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestSync_nullBaseUrl_withReverseProxy_proceeds() throws Exception {
        PushwooshRequestManager bare =
                new PushwooshRequestManager(registrationPrefs, allowingCommunication(), false, transport);
        bare.setReverseProxyUrl(BASE_URL, null);
        transport.respondWith(200, OK_BODY);

        Result<String, NetworkException> result = bare.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(true));
        assertEquals(1, transport.count());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendRequestSync_afterUpdateBaseUrl_usesUpdatedUrl() throws Exception {
        PushwooshRequestManager bare =
                new PushwooshRequestManager(registrationPrefs, allowingCommunication(), false, transport);
        bare.updateBaseUrl(BASE_URL);
        transport.respondWith(200, OK_BODY);

        Result<String, NetworkException> result = bare.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(true));
        assertEquals(BASE_URL, transport.last().url);
        assertEquals("testMethod", transport.last().method);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void reverseProxy_active_requestGoesToProxyUrl() throws Exception {
        String proxyUrl = "https://proxy.test.pushwoosh.local/";
        requestManager.setReverseProxyUrl(proxyUrl, null);
        transport.respondWith(200, "{\"response\" : {\"result\" : \"x\"}, \"status_code\" : 200}");

        Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(true));
        assertEquals(1, transport.count());
        assertEquals(proxyUrl, transport.last().url);
        assertEquals("testMethod", transport.last().method);
    }

    // Custom headers reach the transport; Content-Type and Authorization are the transport's own
    // business and are asserted in HttpTransportTest.
    @Test(timeout = TIMEOUT_TEST)
    public void reverseProxy_active_customHeadersApplied() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom-Auth", "abc123");
        headers.put("X-Tenant", "tenant-42");
        requestManager.setReverseProxyUrl("https://proxy.test.pushwoosh.local/", headers);
        transport.respondWith(200, "{\"response\" : {\"result\" : \"x\"}, \"status_code\" : 200}");

        requestManager.sendRequestSync(new TestRequest("p", "r"));

        assertThat(transport.last().headers.get("X-Custom-Auth"), is("abc123"));
        assertThat(transport.last().headers.get("X-Tenant"), is("tenant-42"));
        assertThat(transport.last().apiToken, startsWith("Token "));
    }

    // Reverse proxy active: server-pushed base_url must NOT cause rotation, even though
    // the equals(baseUrl, baseRequestUrl) part of the rotation invariant is satisfied.
    @Test(timeout = TIMEOUT_TEST)
    public void reverseProxy_active_rotationSuppressed_evenWithBaseUrlInResponse() throws Exception {
        requestManager.setReverseProxyUrl("https://proxy.test.pushwoosh.local/", null);
        transport.respondWith(200, "{\"status_code\":200,\"response\":{},\"base_url\":\"" + BASE_URL + "rotated/\"}");
        String before = registrationPrefs.baseUrl().get();

        requestManager.sendRequestSync(new TestRequest("p", "r"));

        assertEquals(before, registrationPrefs.baseUrl().get());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void reverseProxy_required_butNotConfigured_blocks() throws Exception {
        PushwooshRequestManager strict =
                new PushwooshRequestManager(registrationPrefs, allowingCommunication(), true, transport);
        strict.updateBaseUrl(BASE_URL);

        Result<String, NetworkException> result = strict.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getException().getMessage(), is("Reverse proxy is required but not configured"));
        assertEquals(0, transport.count());
    }

    // The full matrix of rejection branches belongs to RegistrationPrefsTest; here we only prove the
    // seam runs the very same normalization.
    @Test(timeout = TIMEOUT_TEST)
    public void setReverseProxyUrl_invalidUrl_returnsFalse() {
        String[] invalidInputs = new String[] {
            "",
            "   ",
            "not-a-url",
            "ftp://proxy.test.pushwoosh.local/",
            "https://proxy.test.pushwoosh.local/json 1.3/",
            "https://[bad"
        };

        for (String invalidInput : invalidInputs) {
            assertFalse(
                    "input=\"" + invalidInput + "\" must be rejected",
                    requestManager.setReverseProxyUrl(invalidInput, null));
        }
    }

    @Test(timeout = TIMEOUT_TEST)
    public void setReverseProxyUrl_urlWithoutTrailingSlash_requestGoesToNormalizedUrl() throws Exception {
        assertTrue(requestManager.setReverseProxyUrl("https://proxy.test.pushwoosh.local", null));
        transport.respondWith(200, OK_BODY);

        requestManager.sendRequestSync(new TestRequest("p", "r"));

        assertEquals("https://proxy.test.pushwoosh.local/", transport.last().url);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void setReverseProxyUrl_urlWithSurroundingWhitespace_requestGoesToTrimmedUrl() throws Exception {
        assertTrue(requestManager.setReverseProxyUrl("  https://proxy.test.pushwoosh.local/  ", null));
        transport.respondWith(200, OK_BODY);

        requestManager.sendRequestSync(new TestRequest("p", "r"));

        assertEquals("https://proxy.test.pushwoosh.local/", transport.last().url);
    }

    // Assignment used to happen before any check, so a malformed second call wiped a working proxy.
    @Test(timeout = TIMEOUT_TEST)
    public void setReverseProxyUrl_rejectedUrl_keepsAcceptedProxyAndHeaders() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom-Auth", "abc123");
        requestManager.setReverseProxyUrl("https://proxy.test.pushwoosh.local/", headers);

        assertFalse(requestManager.setReverseProxyUrl("https://proxy.test.pushwoosh.local/json 1.3/", null));

        transport.respondWith(200, OK_BODY);
        requestManager.sendRequestSync(new TestRequest("p", "r"));
        assertEquals("https://proxy.test.pushwoosh.local/", transport.last().url);
        assertThat(transport.last().headers.get("X-Custom-Auth"), is("abc123"));
    }

    // The mirror of the previous test: reject keeps the old settings, accept replaces them wholesale,
    // so null headers on an accepted call drop the ones applied before.
    @Test(timeout = TIMEOUT_TEST)
    public void setReverseProxyUrl_secondAcceptedUrl_replacesProxyAndClearsHeaders() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom-Auth", "abc123");
        requestManager.setReverseProxyUrl("https://proxy-a.test.pushwoosh.local/", headers);

        assertTrue(requestManager.setReverseProxyUrl("https://proxy-b.test.pushwoosh.local/", null));

        transport.respondWith(200, OK_BODY);
        requestManager.sendRequestSync(new TestRequest("p", "r"));
        assertEquals("https://proxy-b.test.pushwoosh.local/", transport.last().url);
        assertFalse(transport.last().headers.containsKey("X-Custom-Auth"));
    }

    // A rejected call is all-or-nothing: the headers it came with must not slip in either.
    @Test(timeout = TIMEOUT_TEST)
    public void setReverseProxyUrl_rejectedUrl_doesNotApplyItsHeaders() throws Exception {
        Map<String, String> acceptedHeaders = new HashMap<>();
        acceptedHeaders.put("X-Custom-Auth", "abc123");
        requestManager.setReverseProxyUrl("https://proxy.test.pushwoosh.local/", acceptedHeaders);
        Map<String, String> rejectedHeaders = new HashMap<>();
        rejectedHeaders.put("X-Custom-Auth", "hijacked");

        assertFalse(requestManager.setReverseProxyUrl("not-a-url", rejectedHeaders));

        transport.respondWith(200, OK_BODY);
        requestManager.sendRequestSync(new TestRequest("p", "r"));
        assertThat(transport.last().headers.get("X-Custom-Auth"), is("abc123"));
    }

    // In required mode there is no default endpoint, so the gate must follow acceptance: a rejected
    // URL leaves the SDK blocked instead of firing requests at a proxy that never passed validation.
    @Test(timeout = TIMEOUT_TEST)
    public void reverseProxy_required_gateOpensOnlyForAcceptedUrl() throws Exception {
        PushwooshRequestManager strict =
                new PushwooshRequestManager(registrationPrefs, allowingCommunication(), true, transport);
        strict.updateBaseUrl(BASE_URL);

        assertFalse(strict.setReverseProxyUrl("https://proxy.test.pushwoosh.local/json 1.3/", null));

        Result<String, NetworkException> blocked = strict.sendRequestSync(new TestRequest("p", "r"));
        assertThat(blocked.isSuccess(), is(false));
        assertThat(blocked.getException().getMessage(), is("Reverse proxy is required but not configured"));
        assertEquals(0, transport.count());

        assertTrue(strict.setReverseProxyUrl("https://proxy.test.pushwoosh.local/", null));
        transport.respondWith(200, OK_BODY);

        Result<String, NetworkException> allowed = strict.sendRequestSync(new TestRequest("p", "r"));

        assertThat(allowed.isSuccess(), is(true));
        assertEquals("https://proxy.test.pushwoosh.local/", transport.last().url);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void setReverseProxyUrl_null_resetsProxyAndRequestGoesToBaseUrl() throws Exception {
        requestManager.setReverseProxyUrl("https://proxy.test.pushwoosh.local/", null);

        assertTrue(requestManager.setReverseProxyUrl(null, null));

        transport.respondWith(200, OK_BODY);
        requestManager.sendRequestSync(new TestRequest("p", "r"));
        assertEquals(BASE_URL, transport.last().url);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void callerProvidedBaseUrl_differsFromCurrent_rotationSkipped() throws Exception {
        String otherUrl = BASE_URL + "other/";
        transport.respondWith(200, "{\"status_code\":200,\"response\":{},\"base_url\":\"" + BASE_URL + "rotated/\"}");
        String before = registrationPrefs.baseUrl().get();
        Callback<String, NetworkException> callback = CallbackWrapper.spy();
        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);

        requestManager.sendRequest(new TestRequest("p", "r"), otherUrl, callback);
        awaitAsyncRoundTrip();

        verify(callback).process(captor.capture());
        assertEquals(otherUrl, transport.last().url);
        // Caller's baseUrl != sticky baseRequestUrl -> equals is false -> rotation skipped
        assertEquals(before, registrationPrefs.baseUrl().get());
    }

    // Captures the brittle equals-based rotation contract: a caller passing the exact same URL
    // as baseRequestUrl satisfies equals=true and rotation kicks in.
    @Test(timeout = TIMEOUT_TEST)
    public void callerProvidedBaseUrl_sameAsCurrent_rotationApplies() throws Exception {
        transport.respondWith(200, "{\"status_code\":200,\"response\":{},\"base_url\":\"" + BASE_URL + "rotated/\"}");
        Callback<String, NetworkException> callback = CallbackWrapper.spy();
        ArgumentCaptor<Result<String, NetworkException>> captor = ArgumentCaptor.forClass(Result.class);

        requestManager.sendRequest(new TestRequest("p", "r"), BASE_URL, callback);
        awaitAsyncRoundTrip();

        verify(callback).process(captor.capture());
        assertEquals(BASE_URL + "rotated/", registrationPrefs.baseUrl().get());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void serverCommunicationStopped_returnsBlocked() throws Exception {
        ServerCommunicationManager scm = mock(ServerCommunicationManager.class);
        when(scm.isServerCommunicationAllowed()).thenReturn(false);
        PushwooshRequestManager m = new PushwooshRequestManager(registrationPrefs, scm, false, transport);
        m.updateBaseUrl(BASE_URL);

        Result<String, NetworkException> result = m.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(false));
        assertThat(
                result.getException().getMessage(),
                is("Server communication stopped. Call Pushwoosh.startServerCommunication() to resume"));
        assertEquals(0, transport.count());
    }

    // HTTP 4xx with empty body: Manager synthesizes envelope and overloads pushwooshStatus
    // with the HTTP-level statusCode. Both codes equal the HTTP value.
    @Test(timeout = TIMEOUT_TEST)
    public void error_4xx_emptyBody_returnsConnectionExceptionWithCodes() throws Exception {
        transport.respondWith(404, "Not Found", "");

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
        transport.respondWith(503, "Service Unavailable", "");

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
        transport.respondWith(404, "Not Found", "{\"status_code\":210,\"status_message\":\"Quota\"}");

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
        transport.respondWith(404, "Not Found", "{\"detail\":\"not found\"}");

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
        transport.respondWith(200, "{\"status_code\":200,\"response\":{}}");

        Result<Void, NetworkException> result = requestManager.sendRequestSync(new TestBadResponseRequest());

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getException(), instanceOf(ConnectionException.class));
        ConnectionException ce = (ConnectionException) result.getException();
        assertThat(ce.getStatusCode(), is(200));
        assertThat(ce.getPushwooshStatusCode(), is(200));
    }

    // Any throw out of the transport (connect refused, DNS, reset, read timeout) is narrowed to
    // ConnectionException with codes 0/0. The socket-level half lives in HttpTransportTest.
    @Test(timeout = TIMEOUT_TEST)
    public void transportThrows_returnsTransientConnectionExceptionWithZeroCodes() throws Exception {
        Exception[] failures = {new IOException("Failed to connect"), new SocketTimeoutException("Read timed out")};

        for (Exception failure : failures) {
            String label = "case " + failure.getClass().getSimpleName();
            transport.failWith(failure);

            Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("p", "r"));

            assertThat(label, result.isSuccess(), is(false));
            assertThat(label, result.getException(), instanceOf(ConnectionException.class));
            ConnectionException ce = (ConnectionException) result.getException();
            assertThat(label, ce.getStatusCode(), is(0));
            assertThat(label, ce.getPushwooshStatusCode(), is(0));
            // Zero codes are what RetriableRequestCallback and PushStatisticsWorker read as "retry".
            assertTrue(label, ce.isTransient());
        }
    }

    // HTTP 200 + empty body: no envelope to parse, so pushwooshStatus stays at 0 and the manager
    // takes the fail-path with statusCode=200, pushwooshStatusCode=0.
    @Test(timeout = TIMEOUT_TEST)
    public void httpOk_emptyBody_returnsFailWithPushwooshStatusZero() throws Exception {
        transport.respondWith(200, "");

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
        transport.respondWith(200, "{\"status_code\":200,\"response\":{}}");

        requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

        JSONObject body = new JSONObject(transport.last().payload);
        assertThat(body.has("request"), is(true));
        JSONObject inner = body.getJSONObject("request");
        assertThat(inner.getString("param"), is(equalTo("testParam")));
        assertThat(inner.has("hwid"), is(true));
    }

    // Envelope contract: shouldWrapRequest()=false sends a flat payload — no "request" wrapper.
    // This is the contract for the tracking endpoint (setMADID).
    @Test(timeout = TIMEOUT_TEST)
    public void shouldWrapRequest_false_payloadIsFlat() throws Exception {
        transport.respondWith(200, "{\"status_code\":200,\"response\":{}}");

        requestManager.sendRequestSync(new FlatPayloadTestRequest("v1"));

        JSONObject body = new JSONObject(transport.last().payload);
        assertThat(body.has("request"), is(false));
        assertThat(body.getString("custom"), is(equalTo("v1")));
        assertThat(body.getString("hwid"), is(equalTo("test_hwid")));
    }

    // Verifies that updateBaseUrl returns true when RegistrationPrefs accepts and normalizes the URL.
    @Test(timeout = TIMEOUT_TEST)
    public void testUpdateBaseUrl_returnsTrue_whenNormalizedNonNull() {
        RegistrationPrefs prefsMock = mock(RegistrationPrefs.class);
        when(prefsMock.updateBaseUrl("https://valid")).thenReturn("https://valid");
        PushwooshRequestManager manager =
                new PushwooshRequestManager(prefsMock, mock(ServerCommunicationManager.class), false, transport);

        assertTrue(manager.updateBaseUrl("https://valid"));
    }

    // Verifies that updateBaseUrl returns false when RegistrationPrefs rejects the URL (returns null).
    @Test(timeout = TIMEOUT_TEST)
    public void testUpdateBaseUrl_returnsFalse_whenNormalizedNull() {
        RegistrationPrefs prefsMock = mock(RegistrationPrefs.class);
        when(prefsMock.updateBaseUrl("bad")).thenReturn(null);
        PushwooshRequestManager manager =
                new PushwooshRequestManager(prefsMock, mock(ServerCommunicationManager.class), false, transport);

        assertFalse(manager.updateBaseUrl("bad"));
    }

    // Pins the seam itself: the manager must dispatch through the transport it was constructed
    // with, not one it builds for itself.
    @Test(timeout = TIMEOUT_TEST)
    public void constructorTransport_isTheOneUsedForDispatch() throws Exception {
        FakeHttpTransport injected = new FakeHttpTransport();
        injected.respondWith(200, "{\"status_code\":200,\"response\":{}}");
        PushwooshRequestManager manager =
                new PushwooshRequestManager(registrationPrefs, allowingCommunication(), false, injected);
        manager.updateBaseUrl(BASE_URL);

        Result<String, NetworkException> result = manager.sendRequestSync(new TestRequest("p", "r"));

        assertThat(result.isSuccess(), is(true));
        assertEquals(1, injected.count());
    }
}
