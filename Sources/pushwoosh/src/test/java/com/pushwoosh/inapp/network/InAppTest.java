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

package com.pushwoosh.inapp.network;

import androidx.annotation.NonNull;

import com.pushwoosh.BuildConfig;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.Expectation;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.RequestManagerMock;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.pushwoosh.internal.utils.MockConfig.APP_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(constants = BuildConfig.class)
public class InAppTest {
	private PlatformTestManager platformTestManager;

	private RequestManagerMock requestManagerMock;

	// class under test
	private PushwooshInAppImpl pushwooshInApp;
	private PushwooshRepository pushwooshRepository;

	@Before
	public void setUp() throws Exception {
		Config configMock = MockConfig.createMock();

		platformTestManager = new PlatformTestManager(configMock);
		platformTestManager.setUp();

		requestManagerMock = platformTestManager.getRequestManager();
		pushwooshInApp = platformTestManager.getPushwooshInApp();
		pushwooshRepository = platformTestManager.getPushwooshRepository();
	}

	@After
	public void tearDown() throws Exception {
		platformTestManager.tearDown();
	}

	//Tests postEvent method sends correct request successfuly
	@Test
	public void postEventTest() throws Exception {
		Callback<Void, PostEventException> callback = CallbackWrapper.spy();
		ArgumentCaptor<Result<Void, PostEventException>> captor = ArgumentCaptor.forClass(Result.class);
		Expectation<JSONObject> expectation = requestManagerMock.expect(PostEventRequest.class);
		ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);
		JSONObject jsonObject = createPostEventResponse();
		requestManagerMock.setResponse(jsonObject, PostEventRequest.class);


		// Steps:
		pushwooshInApp.setUserId("userId");
		pushwooshInApp.postEvent("eventName", Tags.intTag("intTag", 5), callback);


		// Postconditions:
		verify(callback, timeout(100)).process(captor.capture());
		Result<Void, PostEventException> result = captor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(100)).fulfilled(requestCaptor.capture());
		JSONObject params = requestCaptor.getValue();

		assertThat(params.getString("application"), is(equalTo(APP_ID)));
		assertThat(params.getString("userId"), is(equalTo("userId")));
		assertThat(params.getString("event"), is(equalTo("eventName")));
		JSONAssert.assertEquals(new JSONObject("{\"intTag\" : 5}"), params.getJSONObject("attributes"), true);
	}

	//Tests postEvent method sends request with exception
	@Test
	public void postEventWithExceptionTest() throws Exception {
		Callback<Void, PostEventException> callback = CallbackWrapper.spy();
		ArgumentCaptor<Result<Void, PostEventException>> captor = ArgumentCaptor.forClass(Result.class);
		requestManagerMock.setException(new NetworkException("test network fail"), PostEventRequest.class);


		// Steps:
		pushwooshInApp.setUserId("userId");
		pushwooshInApp.postEvent("eventName", Tags.intTag("intTag", 5), callback);


		// Postconditions:
		verify(callback, timeout(100)).process(captor.capture());
		Result<Void, PostEventException> result = captor.getValue();
		assertThat(result.isSuccess(), is(false));
	}

	//Tests postEvent method sends correct request successfuly without callBack
	@Test
	public void postEventWithoutCallBack() throws Exception {
		Expectation<JSONObject> expectation = requestManagerMock.expect(PostEventRequest.class);
		ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);


		// Steps:
		pushwooshInApp.setUserId("userId");
		pushwooshInApp.postEvent("eventName", Tags.intTag("intTag", 5), null);


		// Postconditions:
		verify(expectation, timeout(100)).fulfilled(requestCaptor.capture());
		JSONObject params = requestCaptor.getValue();

		assertThat(params.getString("application"), is(equalTo(APP_ID)));
		assertThat(params.getString("userId"), is(equalTo("userId")));
		assertThat(params.getString("event"), is(equalTo("eventName")));
		JSONAssert.assertEquals(new JSONObject("{\"intTag\" : 5}"), params.getJSONObject("attributes"), true);
	}

	//Tests postEvent method sends correct request successfuly
	@Test
	public void postEventWithNullAttributesTest() throws Exception {
		Callback<Void, PostEventException> callback = CallbackWrapper.spy();
		ArgumentCaptor<Result<Void, PostEventException>> captor = ArgumentCaptor.forClass(Result.class);
		Expectation<JSONObject> expectation = requestManagerMock.expect(PostEventRequest.class);
		ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);
		JSONObject jsonObject = createPostEventResponse();
		requestManagerMock.setResponse(jsonObject, PostEventRequest.class);


		// Steps:
		pushwooshInApp.setUserId("userId");
		pushwooshInApp.postEvent("eventName", null, callback);


		// Postconditions:
		verify(callback, timeout(100)).process(captor.capture());
		Result<Void, PostEventException> result = captor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(100)).fulfilled(requestCaptor.capture());
		JSONObject params = requestCaptor.getValue();

		assertThat(params.getString("application"), is(equalTo(APP_ID)));
		assertThat(params.getString("userId"), is(equalTo("userId")));
		assertThat(params.getString("event"), is(equalTo("eventName")));
	}

	@NonNull
	private JSONObject createPostEventResponse() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		JSONObject response = new JSONObject();
		response.put("code", "TEST_CODE");
		response.put("required", false);
		jsonObject.put("response", response);
		return jsonObject;
	}

	//Tests setUserId method sets new userId value and sends correct request
	@Test
	public void setUserIdTest() throws Exception {
		Expectation<JSONObject> expectation = requestManagerMock.expect(RegisterUserRequest.class);
		ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);

		// Steps:
		pushwooshInApp.setUserId("testUserId");

		// Postcondition:
		verify(expectation, timeout(100)).fulfilled(requestCaptor.capture());
		JSONObject params = requestCaptor.getValue();
		assertThat(params.getString("application"), is(equalTo(APP_ID)));
		assertThat(params.getString("userId"), is(equalTo("testUserId")));
	}

	//Tests setUserId method not duplicate request with same id
	@Test
	public void setSameUserIdTest() throws Exception {

		Expectation<JSONObject> expectation = requestManagerMock.expect(RegisterUserRequest.class);
		ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);

		// Steps:
		pushwooshInApp.setUserId("testUserId");
		pushwooshInApp.setUserId("testUserId");

		// Postcondition:
		verify(expectation, timeout(100).times(1)).fulfilled(requestCaptor.capture());
		JSONObject params = requestCaptor.getValue();
		assertThat(params.getString("application"), is(equalTo(APP_ID)));
		assertThat(params.getString("userId"), is(equalTo("testUserId")));
	}

	//
	// sendInappPurchase() part
	//-----------------------------------------------------------------------

	//Tests sendInappPurchase method sends PostEventRequest with correct parameters
	@Test
	public void sendInappPurchaseTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		Expectation<JSONObject> expectation = requestManagerMock.expect(PostEventRequest.class);
		Date date = new Date(1010101101010L);

		// steps:
		pushwooshRepository.sendInappPurchase("product1", BigDecimal.valueOf(42), "USD", date);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postconditions:
		verify(expectation, timeout(1000)).fulfilled(captor.capture());
		JSONObject requestJson = captor.getValue();
		JSONObject params = requestJson.getJSONObject("attributes");


		assertThat(params.getString("amount"), is("42"));
		assertThat(params.getString("currency"), is("USD"));
		assertThat(params.getString("productIdentifier"), is("product1"));
		assertThat(params.getInt("quantity"), is(1));
		assertThat(params.getString("status"), is("success"));
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String formattedDateString = dateFormat.format(date);
		assertThat(params.getString("transactionDate"), is(formattedDateString));
	}

}