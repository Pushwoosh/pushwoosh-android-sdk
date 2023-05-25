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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import com.pushwoosh.BuildConfig;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;
import org.skyscreamer.jsonassert.JSONAssert;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(constants = BuildConfig.class)
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

		@NonNull
		@Override
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

	private static class TestBadParamsRequest extends PushRequest<Void> {

		@Override
		public String getMethod() {
			return "testBadParams";
		}

		@Override
		protected void buildParams(JSONObject params) throws JSONException {
			throw new JSONException("test invalid params");
		}

		@NonNull
		@Override
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

		@NonNull
		@Override
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
		requestManager = new PushwooshRequestManager(registrationPrefs, null, serverCommunicationManager);

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
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
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
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
		Result<String, NetworkException> result = requestManager.sendRequestSync(testRequest);

		assertThat(result.isSuccess(), is(true));
		Assert.assertNull(result.getData());

		Assert.assertEquals(0, server.getRequestCount());
	}


	@Test(timeout = TIMEOUT_TEST)
	public void baseUrlSwitch() throws Exception {
		String body = String.format("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200, \"base_url\" : \"%s\"}", requestUrl + "newUrl/");
		server.enqueue(new MockResponse().setBody(body));
		Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

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

	@Test(timeout = TIMEOUT_TEST)
	public void badStatusCode() throws Exception {
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}").setResponseCode(503));
		Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

		assertThat(result.isSuccess(), is(false));

		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath(), is(equalTo("/testMethod")));
	}

	@Test(timeout = TIMEOUT_TEST)
	public void badPushwooshStatusCode() throws Exception {
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 201}"));
		Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

		assertThat(result.isSuccess(), is(false));

		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath(), is(equalTo("/testMethod")));
	}

	@Test(timeout = TIMEOUT_TEST)
	public void noPushwooshStatusCode() throws Exception {
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}}"));
		Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

		assertThat(result.isSuccess(), is(false));

		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath(), is(equalTo("/testMethod")));
	}

	@Test(timeout = TIMEOUT_TEST)
	public void badJsonResponse() throws Exception {
		server.enqueue(new MockResponse().setBody("[]"));
		Result<String, NetworkException> result = requestManager.sendRequestSync(new TestRequest("testParam", "testResult"));

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

	@Ignore
	@Test(timeout = TIMEOUT_TEST)
	public void sendRequest() throws Exception {
		TestRequest testRequest = new TestRequest("testParam", "testResult");
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
		Callback<String, NetworkException> callback = CallbackWrapper.spy();
		ArgumentCaptor<Result<String, NetworkException>> callbackCaptor = ArgumentCaptor.forClass(Result.class);

		requestManager.sendRequest(testRequest, callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		verify(callback, timeout(100)).process(callbackCaptor.capture());
		Result<String, NetworkException> result = callbackCaptor.getValue();

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
	public void sendRequestBlockedByRemoveAllDevice() throws Exception {
		registrationPrefs.removeAllDeviceData().set(true);
		TestRequest testRequest = new TestRequest("testParam", "testResult");
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
		Callback<String, NetworkException> callback = CallbackWrapper.spy();
		ArgumentCaptor<Result<String, NetworkException>> callbackCaptor = ArgumentCaptor.forClass(Result.class);

		requestManager.sendRequest(testRequest, callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		verify(callback).process(callbackCaptor.capture());
		Result<String, NetworkException> value = callbackCaptor.getValue();
		NetworkException exception = value.getException();
		assertThat(exception.getMessage(), is("Device data was removed from Pushwoosh and all interactions were stopped"));
		assertThat(server.getRequestCount(),is(0) );

	}

	@Ignore
	@Test(timeout = TIMEOUT_TEST)
	public void sendBadParamsRequest() throws Exception {
		TestBadParamsRequest testRequest = new TestBadParamsRequest();
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
		Callback<Void, NetworkException> callback = CallbackWrapper.spy();
		ArgumentCaptor<Result<Void, NetworkException>> callbackCaptor = ArgumentCaptor.forClass(Result.class);

		requestManager.sendRequest(testRequest, callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		verify(callback, timeout(100)).process(callbackCaptor.capture());
		Result<Void, NetworkException> result = callbackCaptor.getValue();

		assertThat(result.isSuccess(), is(false));
	}

	@Ignore
	@Test(timeout = TIMEOUT_TEST)
	public void sendBadResponseRequest() throws Exception {
		TestBadResponseRequest testRequest = new TestBadResponseRequest();
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));
		Callback<Void, NetworkException> callback = CallbackWrapper.spy();
		ArgumentCaptor<Result<Void, NetworkException>> callbackCaptor = ArgumentCaptor.forClass(Result.class);

		requestManager.sendRequest(testRequest, callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		verify(callback, timeout(100)).process(callbackCaptor.capture());
		Result<Void, NetworkException> result = callbackCaptor.getValue();

		assertThat(result.isSuccess(), is(false));

		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath(), is(equalTo("/testBadResponse")));
	}


	@Ignore
	@Test(timeout = TIMEOUT_TEST)
	public void sendRequestWithoutCallback() throws Exception {
		TestRequest testRequest = new TestRequest("testParam", "testResult");
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));

		requestManager.sendRequest(testRequest);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

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
	public void sendRequestWithoutCallbackBlockedByRemoveAllDevice() throws Exception {
		registrationPrefs.removeAllDeviceData().set(true);
		TestRequest testRequest = new TestRequest("testParam", "testResult");
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));

		requestManager.sendRequest(testRequest);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		assertThat(server.getRequestCount(),is(0));
	}

	@Test(timeout = TIMEOUT_TEST)
	public void sendBadRequestWithoutCallback() throws Exception {
		TestBadResponseRequest testRequest = new TestBadResponseRequest();
		server.enqueue(new MockResponse().setBody("{\"response\" : {\"result\" : \"test output\"}, \"status_code\" : 200}"));

		requestManager.sendRequest(testRequest);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath(), is(equalTo("/testBadResponse")));
	}
}