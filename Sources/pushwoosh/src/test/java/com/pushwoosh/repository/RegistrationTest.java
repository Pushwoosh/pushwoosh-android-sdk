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

package com.pushwoosh.repository;


import com.ibm.icu.util.Calendar;
import com.pushwoosh.BuildConfig;
import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.notification.event.DeregistrationErrorEvent;
import com.pushwoosh.notification.event.DeregistrationSuccessEvent;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.EventListenerWrapper;
import com.pushwoosh.testutil.Expectation;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.RequestManagerMock;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;


import static com.pushwoosh.internal.utils.MockConfig.APP_ID;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(constants = BuildConfig.class)
public class RegistrationTest {
	private static final String PUSH_TOKEN = "test_pushToken";

	private PlatformTestManager platformTestManager;

	private RequestManagerMock requestManagerMock;
	private RegistrationPrefs registrationPrefs;
	private PushRegistrar pushRegistrarMock;
	private PushwooshNotificationManager notificationManager;

	@Before
	public void setUp() throws Exception {
		Config configMock = MockConfig.createMock();

		platformTestManager = new PlatformTestManager(configMock);
		platformTestManager.setUp();

		requestManagerMock = platformTestManager.getRequestManager();
		registrationPrefs = platformTestManager.getRegistrationPrefs();
		pushRegistrarMock = platformTestManager.getPushRegistrar();
		notificationManager = platformTestManager.getNotificationManager();
	}

	@After
	public void tearDown() throws Exception {
		platformTestManager.tearDown();
	}

	//
	// registerForPushNotifications() part
	//-----------------------------------------------------------------------

	//Tests registration fails and callback.onError called when checkDevice throws exception
	@Test
	@Ignore("java.lang.NoSuchMethodError")
	public void checkDeviceExceptionTest() throws Exception {
		Exception exception = new Exception();
		Mockito.doThrow(exception).when(pushRegistrarMock).checkDevice(any());
		ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor = ArgumentCaptor.forClass(Result.class);
		Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback = CallbackWrapper.spy();


		// Steps:
		notificationManager.registerForPushNotifications(callback, true, null);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();


		// Postconditions:
		verify(pushRegistrarMock, timeout(100).times(0)).registerPW(null);
		verify(callback, timeout(1000)).process(captor.capture());

		Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result = captor.getValue();
		assertThat(result.isSuccess(), is(false));
	}

	//Tests registration fails when when checkDevice throws exception and there is no callBack set
	@Test
	@Ignore("java.lang.NoSuchMethodError")
	public void exceptionTestWithNullCallback() throws Exception {
		Exception exception = new Exception();
		Mockito.doThrow(exception).when(pushRegistrarMock).checkDevice(any());

		// Steps:
		notificationManager.registerForPushNotifications(null, true, null);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postconditions:
		verify(pushRegistrarMock, timeout(100).times(0)).registerPW(null);
	}

	//Tests registration successful and callBack.onRegistered called when pushToken is not empty and last registrationTime < 10 mins
	@Test
	public void alreadyRegisteredTest() throws Exception {
		ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor = ArgumentCaptor.forClass(Result.class);
		Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(RegisterDeviceRequest.class);

		registrationPrefs.pushToken().set(PUSH_TOKEN);

		Calendar registrationTime = Calendar.getInstance();
		registrationPrefs.lastPushRegistration().set(registrationTime.getTimeInMillis());


		// Steps:
		notificationManager.registerForPushNotifications(callback, true, null);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();


		// Postcondition:
		verify(pushRegistrarMock, timeout(100).times(0)).registerPW(null);

		verify(callback, timeout(1000)).process(captor.capture());
		Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result = captor.getValue();
		assertThat(result.isSuccess(), is(true));

		RegisterForPushNotificationsResultData resultData = (RegisterForPushNotificationsResultData) result.getData();
		assertThat(resultData.getToken(), is(equalTo(PUSH_TOKEN)));

		verify(expectation, timeout(100).times(0)).fulfilled(any());
	}

	//Tests registration sends correct request and callBack.onRegistered called when requestFinished without exception
	@Test
	public void registerWithSuccessRequestTest() throws Exception {
		ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor = ArgumentCaptor.forClass(Result.class);
		ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);
		Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(RegisterDeviceRequest.class);

		// Steps:
		notificationManager.registerForPushNotifications(callback, true, null);
		assertThat(notificationManager.getPushToken(), is(nullValue())); // intermediate condition

		notificationManager.onRegisteredForRemoteNotifications(PUSH_TOKEN, null);

		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postconditions:
		verify(pushRegistrarMock, timeout(100)).registerPW(null);

		verify(callback, timeout(1000)).process(captor.capture());
		Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result = captor.getValue();
		assertThat(result.isSuccess(), is(true));

		RegisterForPushNotificationsResultData resultData = (RegisterForPushNotificationsResultData) result.getData();
		assertThat(resultData.getToken(), is(equalTo(PUSH_TOKEN)));

		verify(expectation, timeout(100)).fulfilled(requestCaptor.capture());
		JSONObject params = requestCaptor.getValue();

		assertThat(params.getString("application"), is(equalTo(APP_ID)));
		assertThat(params.getString("push_token"), is(equalTo(PUSH_TOKEN)));
	}

	//Tests registration sends correct request when there is no callBack set
	@Test
	public void registerWithSuccessRequestNullCallbackTest() throws Exception {
		ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);
		Expectation<JSONObject> expectation = requestManagerMock.expect(RegisterDeviceRequest.class);

		// Steps:
		notificationManager.registerForPushNotifications(null, true, null);
		notificationManager.onRegisteredForRemoteNotifications(PUSH_TOKEN, null);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postcondition:
		verify(pushRegistrarMock, timeout(100)).registerPW(null);

		verify(expectation, timeout(100)).fulfilled(requestCaptor.capture());
		JSONObject params = requestCaptor.getValue();

		assertThat(params.getString("application"), is(equalTo(APP_ID)));
		assertThat(params.getString("push_token"), is(equalTo(PUSH_TOKEN)));
	}

	//Tests registration fails and onFailedToRegisterForRemoteNotifications method called
	@Test
	public void onFailedToRegisterForRemoteNotificationsTest() throws Exception {
		ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor = ArgumentCaptor.forClass(Result.class);
		Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback = CallbackWrapper.spy();

		// Steps:
		notificationManager.registerForPushNotifications(callback, true, null);
		notificationManager.onFailedToRegisterForRemoteNotifications("test registration error");

		// Postcondition:
		verify(callback, timeout(1000)).process(captor.capture());
		Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result = captor.getValue();

		assertThat(result.isSuccess(), is(false));
	}

	//Tests registration when requestFinished with exception
	@Test
	public void registerWithErrorRequestTest() throws Exception {
		ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor = ArgumentCaptor.forClass(Result.class);
		Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback = CallbackWrapper.spy();
		requestManagerMock.setException(new NetworkException("test network fail"), RegisterDeviceRequest.class);

		// Steps:
		notificationManager.registerForPushNotifications(callback, true, null);
		notificationManager.onRegisteredForRemoteNotifications(PUSH_TOKEN, null);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		verify(pushRegistrarMock, timeout(1000)).registerPW(null);
		verify(callback, timeout(1000)).process(captor.capture());
		Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result = captor.getValue();

		assertThat(result.isSuccess(), is(false));
	}

	//Tests registration when requestFinished with exception and there is no callBack set
	@Test
	public void registerWithErrorRequestNullCallbackTest() throws Exception {
		requestManagerMock.setException(new NetworkException("test network fail"), RegisterDeviceRequest.class);

		// Steps:
		notificationManager.registerForPushNotifications(null, true, null);
		notificationManager.onRegisteredForRemoteNotifications(PUSH_TOKEN, null);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postcondition:
		// no exception
	}

	//
	// unregisterForPushNotifications() part
	//-----------------------------------------------------------------------

	//Tests successful unregister
	@Test
	public void unregisterTest() throws Exception {
		EventListener<DeregistrationSuccessEvent> successEventListener = EventListenerWrapper.spy();
		ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);
		Expectation<JSONObject> expectation = requestManagerMock.expect(UnregisterDeviceRequest.class);

		registrationPrefs.pushToken().set(PUSH_TOKEN);
		registrationPrefs.lastPushRegistration().set(System.currentTimeMillis());

		EventBus.subscribe(DeregistrationSuccessEvent.class, successEventListener);

		// Steps:
		notificationManager.unregisterForPushNotifications(null);
		notificationManager.onUnregisteredFromRemoteNotifications(PUSH_TOKEN);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postcondition:
		verify(pushRegistrarMock, timeout(100)).unregisterPW();
		verify(successEventListener, timeout(100)).onReceive(any());
		verify(expectation, timeout(1000)).fulfilled(requestCaptor.capture());
		JSONObject params = requestCaptor.getValue();

		assertThat(params.getString("application"), is(equalTo(APP_ID)));

		assertThat(notificationManager.getPushToken(), is(nullValue()));
		assertThat(registrationPrefs.lastPushRegistration().get(), is(equalTo(0L)));
	}

	//Tests unregister request throws exception
	@Test
	public void unregisterExceptionTest() throws Exception {
		EventListener<DeregistrationSuccessEvent> successEventListener = EventListenerWrapper.spy();
		EventListener<DeregistrationErrorEvent> errorEventListener = EventListenerWrapper.spy();

		EventBus.subscribe(DeregistrationSuccessEvent.class, successEventListener);
		EventBus.subscribe(DeregistrationErrorEvent.class, errorEventListener);

		requestManagerMock.setException(new NetworkException("test network fail"), UnregisterDeviceRequest.class);

		registrationPrefs.pushToken().set(PUSH_TOKEN);
		registrationPrefs.lastPushRegistration().set(System.currentTimeMillis());

		// Steps:
		notificationManager.unregisterForPushNotifications(null);
		notificationManager.onUnregisteredFromRemoteNotifications(PUSH_TOKEN);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postcondition:
		verify(pushRegistrarMock, timeout(100)).unregisterPW();

		verify(successEventListener, timeout(100).times(0)).onReceive(any());
		verify(errorEventListener, timeout(100)).onReceive(any());

		assertThat(notificationManager.getPushToken(), is(nullValue()));
		assertThat(registrationPrefs.lastPushRegistration().get(), is(equalTo(0L)));
	}

	//Tests deleteToken method throws exception and onFailedToUnregisterFromRemoteNotifications called
	@Test
	public void onFailedToUnregisterFromRemoteNotificationsTest() throws Exception {
		EventListener<DeregistrationSuccessEvent> successEventListener = EventListenerWrapper.spy();
		EventListener<DeregistrationErrorEvent> errorEventListener = EventListenerWrapper.spy();

		EventBus.subscribe(DeregistrationSuccessEvent.class, successEventListener);
		EventBus.subscribe(DeregistrationErrorEvent.class, errorEventListener);

		registrationPrefs.pushToken().set(PUSH_TOKEN);
		registrationPrefs.lastPushRegistration().set(System.currentTimeMillis());

		// Steps:
		notificationManager.unregisterForPushNotifications(null);
		notificationManager.onFailedToUnregisterFromRemoteNotifications("test deregistration error");
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postcondition:
		verify(pushRegistrarMock, timeout(100)).unregisterPW();

		verify(successEventListener, timeout(100).times(0)).onReceive(any());
		verify(errorEventListener, timeout(100)).onReceive(any());
	}

	//Tests getPushToken returns pushToken value
	@Test
	public void getPushTokenTest() throws Exception {
		// Steps:
		registrationPrefs.pushToken().set(PUSH_TOKEN);

		// Postcondition:
		assertThat(notificationManager.getPushToken(), is(equalTo(PUSH_TOKEN)));
	}
}
