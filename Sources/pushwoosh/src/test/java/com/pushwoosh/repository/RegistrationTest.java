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

import static com.pushwoosh.internal.utils.MockConfig.APP_ID;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestDriver;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.ibm.icu.util.Calendar;
import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.FakeRequestManager;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.RequestBlockedException;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.registrar.RegisterDeviceWorker;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.notification.RegistrationCallbackHolder;
import com.pushwoosh.notification.event.DeregistrationErrorEvent;
import com.pushwoosh.notification.event.DeregistrationSuccessEvent;
import com.pushwoosh.notification.event.RegistrationErrorEvent;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.EventListenerWrapper;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class RegistrationTest {
    private static final String PUSH_TOKEN = "test_pushToken";

    private PlatformTestManager platformTestManager;

    private FakeRequestManager fake;
    private RegistrationPrefs registrationPrefs;
    private PushRegistrar pushRegistrarMock;
    private PushwooshNotificationManager notificationManager;
    private WorkManager workManager;
    private TestDriver testDriver;

    @Before
    public void setUp() throws Exception {
        Config configMock = MockConfig.createMock();

        platformTestManager = new PlatformTestManager(configMock);
        platformTestManager.setUp();
        SdkStateProvider.getInstance().setReady();

        Context context = AndroidPlatformModule.getApplicationContext();
        Configuration workConfig = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .setTaskExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, workConfig);
        workManager = WorkManager.getInstance(context);
        testDriver = WorkManagerTestInitHelper.getTestDriver(context);

        fake = platformTestManager.getRequestManager();
        registrationPrefs = platformTestManager.getRegistrationPrefs();
        pushRegistrarMock = platformTestManager.getPushRegistrar();
        notificationManager = platformTestManager.getNotificationManager();
    }

    @After
    public void tearDown() throws Exception {
        RegisterDeviceWorker.sdkReadyTimeoutMillis = RegisterDeviceWorker.DEFAULT_SDK_READY_TIMEOUT_MILLIS;
        platformTestManager.tearDown();
    }

    private UUID pendingDeliveryId() throws Exception {
        for (WorkInfo info :
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get()) {
            if (!info.getState().isFinished()) {
                return info.getId();
            }
        }
        throw new AssertionError("no pending RegisterDeviceWorker");
    }

    private WorkInfo.State runDeliveryWorker() throws Exception {
        UUID id = pendingDeliveryId();
        testDriver.setAllConstraintsMet(id);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        return workManager.getWorkInfoById(id).get().getState();
    }

    //
    // registerForPushNotifications() part
    // -----------------------------------------------------------------------

    // Tests registration successful and callBack.onRegistered called when pushToken is not empty and last
    // registrationTime < 10 mins
    // Pre-Tiramisu test: since API 33 registerForPushNotifications goes through
    // POST_NOTIFICATIONS permission flow (PermissionActivity) before registerPW.
    @Test
    @org.robolectric.annotation.Config(sdk = 23)
    public void alreadyRegisteredTest() throws Exception {
        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback =
                CallbackWrapper.spy();

        registrationPrefs.pushToken().set(PUSH_TOKEN);

        Calendar registrationTime = Calendar.getInstance();
        registrationPrefs.lastPushRegistration().set(registrationTime.getTimeInMillis());

        // Steps:
        notificationManager.registerForPushNotifications(callback, true, null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Postcondition:
        verify(pushRegistrarMock, timeout(100).times(0)).registerPW(null);

        verify(callback, timeout(1000)).process(captor.capture());
        Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result =
                captor.getValue();
        assertThat(result.isSuccess(), is(true));

        RegisterForPushNotificationsResultData resultData = (RegisterForPushNotificationsResultData) result.getData();
        assertThat(resultData.getToken(), is(equalTo(PUSH_TOKEN)));

        assertEquals(0, fake.count("registerDevice"));
    }

    // Tests registration sends correct request and callBack.onRegistered called when requestFinished without exception
    // Pre-Tiramisu test: since API 33 registerForPushNotifications goes through
    // POST_NOTIFICATIONS permission flow (PermissionActivity) before registerPW.
    @Test
    @org.robolectric.annotation.Config(sdk = 23)
    public void registerWithSuccessRequestTest() throws Exception {
        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback =
                CallbackWrapper.spy();
        // Unscripted requests fail by default; the flow's success is gated on this request's result
        // (DeviceRegistrar's callback), so it must be scripted for isSuccess() to hold below.
        fake.respondWith("registerDevice", new JSONObject());

        // Steps:
        notificationManager.registerForPushNotifications(callback, true, null);
        assertThat(notificationManager.getPushToken(), is(nullValue())); // intermediate condition

        notificationManager.onTokenReceived(PUSH_TOKEN, null);
        runDeliveryWorker();

        // Postconditions:
        verify(pushRegistrarMock, timeout(100)).registerPW(null);

        verify(callback, timeout(1000)).process(captor.capture());
        Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result =
                captor.getValue();
        assertThat(result.isSuccess(), is(true));

        RegisterForPushNotificationsResultData resultData = (RegisterForPushNotificationsResultData) result.getData();
        assertThat(resultData.getToken(), is(equalTo(PUSH_TOKEN)));

        JSONObject params = fake.awaitLast("registerDevice").params;

        assertThat(params.getString("application"), is(equalTo(APP_ID)));
        assertThat(params.getString("push_token"), is(equalTo(PUSH_TOKEN)));
        fake.assertAllScripted();
    }

    // Tests registration sends correct request when there is no callBack set
    // Pre-Tiramisu test: since API 33 registerForPushNotifications goes through
    // POST_NOTIFICATIONS permission flow (PermissionActivity) before registerPW.
    @Test
    @org.robolectric.annotation.Config(sdk = 23)
    public void registerWithSuccessRequestNullCallbackTest() throws Exception {
        // Unscripted requests fail by default; script a success so the flow completes as before.
        fake.respondWith("registerDevice", new JSONObject());

        // Steps:
        notificationManager.registerForPushNotifications(null, true, null);
        notificationManager.onTokenReceived(PUSH_TOKEN, null);
        runDeliveryWorker();

        // Postcondition:
        verify(pushRegistrarMock, timeout(100)).registerPW(null);

        JSONObject params = fake.awaitLast("registerDevice").params;

        assertThat(params.getString("application"), is(equalTo(APP_ID)));
        assertThat(params.getString("push_token"), is(equalTo(PUSH_TOKEN)));
        fake.assertAllScripted();
    }

    // Tests registration fails and onFailedToRegisterForRemoteNotifications method called
    @Test
    public void onFailedToRegisterForRemoteNotificationsTest() throws Exception {
        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback =
                CallbackWrapper.spy();

        // Steps:
        notificationManager.registerForPushNotifications(callback, false, null);
        notificationManager.onFailedToRegisterForRemoteNotifications("test registration error");

        // Postcondition:
        verify(callback, timeout(10000)).process(captor.capture());
        Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result =
                captor.getValue();

        assertThat(result.isSuccess(), is(false));
    }

    // Tests registration when requestFinished with exception
    // Pre-Tiramisu test: since API 33 registerForPushNotifications goes through
    // POST_NOTIFICATIONS permission flow (PermissionActivity) before registerPW.
    @Test
    @org.robolectric.annotation.Config(sdk = 23)
    public void registerWithErrorRequestTest() throws Exception {
        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback =
                CallbackWrapper.spy();
        fake.alwaysFailWith("registerDevice", new NetworkException("test network fail"));

        // Steps:
        notificationManager.registerForPushNotifications(callback, true, null);
        notificationManager.onTokenReceived(PUSH_TOKEN, null);
        runDeliveryWorker();

        verify(pushRegistrarMock, timeout(1000)).registerPW(null);
        verify(callback, timeout(1000)).process(captor.capture());
        Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result =
                captor.getValue();

        assertThat(result.isSuccess(), is(false));
        fake.assertAllScripted();
    }

    // Tests registration when requestFinished with exception and there is no callBack set
    @Test
    public void registerWithErrorRequestNullCallbackTest() throws Exception {
        fake.alwaysFailWith("registerDevice", new NetworkException("test network fail"));

        // Steps:
        notificationManager.registerForPushNotifications(null, true, null);
        notificationManager.onTokenReceived(PUSH_TOKEN, null);
        runDeliveryWorker();

        // Postcondition:
        // no exception
        fake.assertAllScripted();
    }

    //
    // unregisterForPushNotifications() part
    // -----------------------------------------------------------------------

    // Tests successful unregister
    @Test
    public void unregisterTest() throws Exception {
        EventListener<DeregistrationSuccessEvent> successEventListener = EventListenerWrapper.spy();
        // DeregistrationSuccessEvent is gated on this request's result (DeviceRegistrar's callback).
        fake.respondWith("unregisterDevice", new JSONObject());

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
        JSONObject params = fake.awaitLast("unregisterDevice").params;

        assertThat(params.getString("application"), is(equalTo(APP_ID)));

        assertThat(notificationManager.getPushToken(), is(nullValue()));
        assertThat(registrationPrefs.lastPushRegistration().get(), is(equalTo(0L)));
        fake.assertAllScripted();
    }

    // Tests unregister request throws exception
    @Test
    public void unregisterExceptionTest() throws Exception {
        EventListener<DeregistrationSuccessEvent> successEventListener = EventListenerWrapper.spy();
        EventListener<DeregistrationErrorEvent> errorEventListener = EventListenerWrapper.spy();

        EventBus.subscribe(DeregistrationSuccessEvent.class, successEventListener);
        EventBus.subscribe(DeregistrationErrorEvent.class, errorEventListener);

        fake.alwaysFailWith("unregisterDevice", new NetworkException("test network fail"));

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
        fake.assertAllScripted();
    }

    // Tests deleteToken method throws exception and onFailedToUnregisterFromRemoteNotifications called
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

    // Tests getPushToken returns pushToken value
    @Test
    public void getPushTokenTest() throws Exception {
        // Steps:
        registrationPrefs.pushToken().set(PUSH_TOKEN);

        // Postcondition:
        assertThat(notificationManager.getPushToken(), is(equalTo(PUSH_TOKEN)));
    }

    // onTokenReceived() part

    @Test
    public void deliverySendsSavedTokenWithTagsAndAppliesSuccess() throws Exception {
        EventListener<RegistrationSuccessEvent> successListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationSuccessEvent.class, successListener);
        fake.respondWith("registerDevice", new JSONObject());
        String tagsJson = new JSONObject().put("test_tag", "test_value").toString();

        notificationManager.onTokenReceived(PUSH_TOKEN, tagsJson);
        assertEquals(0, fake.count("registerDevice"));
        assertThat(registrationPrefs.pushToken().get(), is(equalTo(PUSH_TOKEN)));

        WorkInfo.State state = runDeliveryWorker();

        assertEquals(WorkInfo.State.SUCCEEDED, state);
        JSONObject params = fake.last("registerDevice").params;
        assertThat(params.getString("application"), is(equalTo(APP_ID)));
        assertThat(params.getString("push_token"), is(equalTo(PUSH_TOKEN)));
        assertThat(params.getJSONObject("tags").getString("test_tag"), is(equalTo("test_value")));
        assertEquals(DeviceSpecificProvider.getInstance().deviceType(), params.getInt("device_type"));
        assertThat(registrationPrefs.registeredOnServer().get(), is(true));
        assertTrue(registrationPrefs.lastPushRegistration().get() > 0);
        ArgumentCaptor<RegistrationSuccessEvent> captor = ArgumentCaptor.forClass(RegistrationSuccessEvent.class);
        verify(successListener, timeout(1000)).onReceive(captor.capture());
        assertThat(captor.getValue().getData().getToken(), is(equalTo(PUSH_TOKEN)));
        fake.assertAllScripted();
    }

    private void assertTransientFailureRetries(NetworkException failure) throws Exception {
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback =
                CallbackWrapper.spy();
        RegistrationCallbackHolder.setCallback(callback);
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);
        fake.alwaysFailWith("registerDevice", failure);

        notificationManager.onTokenReceived(PUSH_TOKEN, null);
        WorkInfo.State state = runDeliveryWorker();

        assertEquals(WorkInfo.State.ENQUEUED, state);
        assertEquals(1, fake.count("registerDevice"));
        verify(errorListener, timeout(1000)).onReceive(any());
        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        verify(callback, timeout(1000)).process(captor.capture());
        assertThat(captor.getValue().isSuccess(), is(false));
        assertThat(registrationPrefs.registeredOnServer().get(), is(false));
        assertThat(registrationPrefs.lastPushRegistration().get(), is(equalTo(0L)));
    }

    @Test
    public void deliveryRetriesOnTransientFailure() throws Exception {
        assertTransientFailureRetries(new ConnectionException("connection reset", 0, 0));
    }

    private void assertTerminalFailureFails(NetworkException failure) throws Exception {
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);
        fake.alwaysFailWith("registerDevice", failure);

        notificationManager.onTokenReceived(PUSH_TOKEN, null);
        WorkInfo.State state = runDeliveryWorker();

        assertEquals(WorkInfo.State.FAILED, state);
        verify(errorListener, timeout(1000)).onReceive(any());
        assertThat(registrationPrefs.registeredOnServer().get(), is(false));
    }

    @Test
    public void deliveryFailsOnBadRequest() throws Exception {
        assertTerminalFailureFails(new ConnectionException("Bad Request", 400, 0));
    }

    @Test
    public void deliveryFailsWhenRequestBlocked() throws Exception {
        assertTerminalFailureFails(new RequestBlockedException("Server communication stopped"));
    }

    @Test
    public void deliverySucceedsWithoutRequestWhenTokenCleared() throws Exception {
        notificationManager.onTokenReceived(PUSH_TOKEN, null);
        registrationPrefs.pushToken().set("");

        assertEquals(WorkInfo.State.SUCCEEDED, runDeliveryWorker());
        fake.assertNoRequests();
    }

    @Test
    public void deliveryWaitsForSdkReady() throws Exception {
        SdkStateProvider.getInstance().resetForTesting();
        fake.respondWith("registerDevice", new JSONObject());
        notificationManager.onTokenReceived(PUSH_TOKEN, null);
        assertEquals(0, fake.count("registerDevice"));
        Thread ready = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            SdkStateProvider.getInstance().setReady();
        });
        ready.start();

        WorkInfo.State state = runDeliveryWorker();
        ready.join();

        assertEquals(WorkInfo.State.SUCCEEDED, state);
        assertEquals(1, fake.count("registerDevice"));
    }

    @Test
    public void deliveryRetriesWhenSdkInError() throws Exception {
        SdkStateProvider.getInstance().setError();
        notificationManager.onTokenReceived(PUSH_TOKEN, null);

        assertEquals(WorkInfo.State.ENQUEUED, runDeliveryWorker());
        fake.assertNoRequests();
    }

    @Test
    public void deliveryRetriesWhenSdkReadyTimesOut() throws Exception {
        RegisterDeviceWorker.sdkReadyTimeoutMillis = 50L;
        SdkStateProvider.getInstance().resetForTesting();
        notificationManager.onTokenReceived(PUSH_TOKEN, null);

        assertEquals(WorkInfo.State.ENQUEUED, runDeliveryWorker());
        fake.assertNoRequests();
    }

    // setError clears the SDK task queue, so the worker must notice the
    // state change itself instead of holding a WorkManager thread until the ready timeout.
    @Test
    public void deliveryRetriesWhenSdkErrorsWhileWaiting() throws Exception {
        RegisterDeviceWorker.sdkReadyTimeoutMillis = 10_000L;
        SdkStateProvider.getInstance().resetForTesting();
        notificationManager.onTokenReceived(PUSH_TOKEN, null);
        Thread error = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            SdkStateProvider.getInstance().setError();
        });
        error.start();

        long started = System.currentTimeMillis();
        WorkInfo.State state = runDeliveryWorker();
        long elapsed = System.currentTimeMillis() - started;
        error.join();

        assertEquals(WorkInfo.State.ENQUEUED, state);
        assertTrue("worker waited " + elapsed + " ms for the ready timeout", elapsed < 3000);
        fake.assertNoRequests();
    }

    // Unsubscribe and REPLACE stop a running worker but cannot interrupt a socket, so a worker that was
    // stopped while waiting for the SDK must drop its run instead of opening the request.
    @Test
    public void stoppedWorkerSendsNothing() {
        registrationPrefs.pushToken().set(PUSH_TOKEN);
        WorkerParameters params = Mockito.mock(WorkerParameters.class);
        Mockito.when(params.getInputData()).thenReturn(Data.EMPTY);
        RegisterDeviceWorker worker =
                Mockito.spy(new RegisterDeviceWorker(AndroidPlatformModule.getApplicationContext(), params));
        Mockito.doReturn(true).when(worker).isStopped();

        ListenableWorker.Result result = worker.doWork();

        assertThat(result, is(ListenableWorker.Result.failure()));
        fake.assertNoRequests();
    }

    // A worker replaced on cold start must free its WorkManager thread without waiting for the ready timeout.
    @Test
    public void workerStoppedWhileWaitingForSdkReturnsBeforeReadyTimeout() {
        RegisterDeviceWorker.sdkReadyTimeoutMillis = 10_000L;
        SdkStateProvider.getInstance().resetForTesting();
        registrationPrefs.pushToken().set(PUSH_TOKEN);
        WorkerParameters params = Mockito.mock(WorkerParameters.class);
        Mockito.when(params.getInputData()).thenReturn(Data.EMPTY);
        RegisterDeviceWorker worker =
                Mockito.spy(new RegisterDeviceWorker(AndroidPlatformModule.getApplicationContext(), params));
        Mockito.doReturn(true).when(worker).isStopped();

        long started = System.currentTimeMillis();
        ListenableWorker.Result result = worker.doWork();
        long elapsed = System.currentTimeMillis() - started;

        assertThat(result, is(ListenableWorker.Result.failure()));
        assertTrue("worker waited " + elapsed + " ms for the ready timeout", elapsed < 3000);
        fake.assertNoRequests();
    }

    // No transport provider: error event, error callback, no request.
    @Test
    public void onTokenReceivedWithoutTransportSendsErrorEvent() throws Exception {
        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback =
                CallbackWrapper.spy();
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);
        RegistrationCallbackHolder.setCallback(callback);
        DeviceSpecificProvider previous = nullDeviceSpecificProvider();

        try {
            notificationManager.onTokenReceived(PUSH_TOKEN, null);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

            verify(errorListener, timeout(1000)).onReceive(any());
            verify(callback, timeout(1000)).process(captor.capture());
            assertThat(captor.getValue().isSuccess(), is(false));
            fake.assertNoRequests();
            assertTrue(workManager
                    .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                    .get()
                    .isEmpty());
        } finally {
            restoreDeviceSpecificProvider(previous);
        }
    }

    private static DeviceSpecificProvider nullDeviceSpecificProvider() throws Exception {
        Field f = DeviceSpecificProvider.class.getDeclaredField("instance");
        f.setAccessible(true);
        DeviceSpecificProvider previous = (DeviceSpecificProvider) f.get(null);
        f.set(null, null);
        return previous;
    }

    private static void restoreDeviceSpecificProvider(DeviceSpecificProvider previous) throws Exception {
        Field f = DeviceSpecificProvider.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, previous);
    }

    @Test
    public void secondTokenBeforeDeliveryReplacesFirst() throws Exception {
        fake.respondWith("registerDevice", new JSONObject());
        notificationManager.onTokenReceived(
                "token-1", new JSONObject().put("first", 1).toString());
        notificationManager.onTokenReceived(
                "token-2", new JSONObject().put("second", 2).toString());

        assertEquals(WorkInfo.State.SUCCEEDED, runDeliveryWorker());

        assertEquals(1, fake.count("registerDevice"));
        JSONObject params = fake.last("registerDevice").params;
        assertThat(params.getString("push_token"), is(equalTo("token-2")));
        assertTrue(params.getJSONObject("tags").has("second"));
        fake.assertAllScripted();
    }

    @Test
    public void unregisterCancelsPendingDelivery() throws Exception {
        notificationManager.onTokenReceived(PUSH_TOKEN, null);
        UUID id = pendingDeliveryId();

        notificationManager.unregisterForPushNotifications(null);

        assertEquals(
                WorkInfo.State.CANCELLED, workManager.getWorkInfoById(id).get().getState());
        fake.assertNoRequests();
    }
}
