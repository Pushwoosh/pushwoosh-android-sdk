/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;

import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.PushRequestHelper;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.registrar.RegisterDeviceWorker;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.notification.event.DeregistrationErrorEvent;
import com.pushwoosh.notification.event.DeregistrationSuccessEvent;
import com.pushwoosh.notification.event.RegistrationErrorEvent;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;
import com.pushwoosh.testutil.EventListenerWrapper;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class DeviceRegistrarTest {
    public static final String TAG = "DeviceRegistrarTest";
    public static final String TEST_ID = "testId";
    private static final String NEW_TOKEN = "testId-2";
    public static final String URL = "url";
    public static final String TEST_EXCEPTION = "test_exception";
    public static final PushwooshException EXCEPTION = new NetworkException(TEST_EXCEPTION);
    private PlatformTestManager platformTestManager;

    @Mock
    private RequestManager requestManager;

    @Captor
    ArgumentCaptor<PushRequest> pushRequestArgumentCaptor;

    @Captor
    ArgumentCaptor<Callback> callbackArgumentCaptor;

    RegistrationPrefs registrationPrefs;
    DeviceRegistrar deviceRegistrar;
    WorkManager workManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();

        NetworkModule.setRequestManager(requestManager);
        registrationPrefs = platformTestManager.getRegistrationPrefs();
        deviceRegistrar = new DeviceRegistrar();

        Context context = AndroidPlatformModule.getApplicationContext();
        Configuration workConfig = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .setTaskExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, workConfig);
        workManager = WorkManager.getInstance(context);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    private static void awaitNetworkExecutor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        BackgroundExecutor.network(latch::countDown);
        Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    private void stubSyncSuccess() {
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenReturn(Result.fromData(null));
    }

    @Test
    public void unregisterWithServer() {
        unregisterServerStart();

        Subscription<DeregistrationSuccessEvent> subscribe = EventBus.subscribe(
                DeregistrationSuccessEvent.class, event -> Assert.assertEquals(TEST_ID, event.getData()));

        verify(requestManager)
                .sendRequest(pushRequestArgumentCaptor.capture(), eq(URL), callbackArgumentCaptor.capture());
        callbackArgumentCaptor.getValue().process(Result.fromData(0));

        checkOftenResult();
        Assert.assertEquals(0, registrationPrefs.lastPushRegistration().get());
        subscribe.unsubscribe();
    }

    private void checkOftenResult() {
        Assert.assertEquals(true, registrationPrefs.registeredOnServer().get());
        Assert.assertEquals(
                "unregisterDevice", pushRequestArgumentCaptor.getValue().getMethod());
    }

    private void unregisterServerStart() {
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.lastPushRegistration().set(1000L);

        deviceRegistrar.unregisterWithServer(TEST_ID, URL, "APP_CODE");
    }

    @Test
    public void unregisterWithServerError() {
        unregisterServerStart();

        Subscription<DeregistrationErrorEvent> subscribe = EventBus.subscribe(
                DeregistrationErrorEvent.class, event -> Assert.assertEquals(TEST_EXCEPTION, event.getData()));

        verify(requestManager)
                .sendRequest(pushRequestArgumentCaptor.capture(), eq(URL), callbackArgumentCaptor.capture());
        callbackArgumentCaptor.getValue().process(Result.fromException(EXCEPTION));

        checkOftenResult();
        Assert.assertEquals(1000L, registrationPrefs.lastPushRegistration().get());
        subscribe.unsubscribe();
    }

    // Verifies that unregisterWithServer publishes a DeregistrationErrorEvent when the SDK is not initialized.
    @Test
    public void unregisterWithServerSdkNotInitializedSendsErrorEvent() {
        registrationPrefs.registeredOnServer().set(true);
        NetworkModule.setRequestManager(null);

        final String[] received = new String[1];
        Subscription<DeregistrationErrorEvent> sub =
                EventBus.subscribe(DeregistrationErrorEvent.class, event -> received[0] = event.getData());

        deviceRegistrar.unregisterWithServer(TEST_ID, URL, "APP_CODE");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertEquals("SDK is not initialized", received[0]);
        Assert.assertTrue(registrationPrefs.registeredOnServer().get());
        sub.unsubscribe();
    }

    // Verifies that the unregister request carries the explicitly passed app code and not the prefs
    // value: by the time the change cycle calls unregister, prefs already hold the NEW code, and the
    // request must still leave with the old one.
    @Test
    public void unregisterWithServerRequestCarriesExplicitAppCode() {
        registrationPrefs.applicationId().set("NEW_CODE");
        registrationPrefs.userId().set("user-1");

        deviceRegistrar.unregisterWithServer(TEST_ID, URL, "OLD_CODE");

        verify(requestManager)
                .sendRequest(pushRequestArgumentCaptor.capture(), eq(URL), callbackArgumentCaptor.capture());
        UnregisterDeviceRequest request = (UnregisterDeviceRequest) pushRequestArgumentCaptor.getValue();
        Assert.assertEquals("OLD_CODE", request.getApplicationId());
        Assert.assertEquals("user-1", request.getUserId());
    }

    // Verifies that single-arg unregisterWithServer sends synchronously with the stored app code,
    // clears registeredOnServer and publishes the outcome itself.
    @Test
    public void unregisterWithServerSingleArgSendsSyncWithStoredAppCode() {
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.lastPushRegistration().set(1000L);
        registrationPrefs.applicationId().set("STORED_CODE");
        stubSyncSuccess();
        final String[] received = new String[1];
        Subscription<DeregistrationSuccessEvent> sub =
                EventBus.subscribe(DeregistrationSuccessEvent.class, event -> received[0] = event.getData());

        deviceRegistrar.unregisterWithServer(TEST_ID);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertFalse(registrationPrefs.registeredOnServer().get());
        Assert.assertEquals(0, registrationPrefs.lastPushRegistration().get());
        Assert.assertEquals(TEST_ID, received[0]);
        verify(requestManager).sendRequestSync(pushRequestArgumentCaptor.capture());
        UnregisterDeviceRequest request = (UnregisterDeviceRequest) pushRequestArgumentCaptor.getValue();
        Assert.assertEquals("STORED_CODE", request.getApplicationId());
        sub.unsubscribe();
    }

    // An unsubscribe that lands while the device's token is being delivered must reach the server after
    // that delivery, never before it: a registerDevice landing last would re-subscribe the device while the
    // SDK already shows it unsubscribed.
    @Test
    public void unregisterWaitsForInFlightDelivery() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.registeredOnServer().set(true);
        CountDownLatch inFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<String> sentMethods = Collections.synchronizedList(new ArrayList<>());
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            PushRequest<?> request = invocation.getArgument(0);
            sentMethods.add(request.getMethod());
            if ("registerDevice".equals(request.getMethod())) {
                inFlight.countDown();
                release.await(5, TimeUnit.SECONDS);
            }
            return Result.fromData(null);
        });
        Thread delivery = new Thread(() -> deviceRegistrar.deliverFromWorker(null));
        Thread unregister = new Thread(() -> deviceRegistrar.unregisterWithServer(TEST_ID));

        try {
            delivery.start();
            Assert.assertTrue(inFlight.await(5, TimeUnit.SECONDS));

            unregister.start();
            unregister.join(500);
            Assert.assertTrue(unregister.isAlive());
            Assert.assertEquals(Collections.singletonList("registerDevice"), sentMethods);

            release.countDown();
            delivery.join(5000);
            unregister.join(5000);

            Assert.assertEquals(Arrays.asList("registerDevice", "unregisterDevice"), sentMethods);
            Assert.assertFalse(registrationPrefs.registeredOnServer().get());
        } finally {
            release.countDown();
            delivery.join(5000);
            unregister.join(5000);
        }
    }

    // Verifies that unregisterWithServer error with empty message falls back to default error description.
    @Test
    public void unregisterWithServerEmptyErrorMessageUsesDefault() {
        registrationPrefs.registeredOnServer().set(true);

        final String[] received = new String[1];
        Subscription<DeregistrationErrorEvent> sub =
                EventBus.subscribe(DeregistrationErrorEvent.class, event -> received[0] = event.getData());

        deviceRegistrar.unregisterWithServer(TEST_ID, URL, "APP_CODE");

        verify(requestManager)
                .sendRequest(pushRequestArgumentCaptor.capture(), eq(URL), callbackArgumentCaptor.capture());
        callbackArgumentCaptor.getValue().process(Result.fromException(new NetworkException("")));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertEquals("Pushwoosh unregistration error", received[0]);
        sub.unsubscribe();
    }

    // Verifies that onAppOpened skips request when pushToken is empty.
    @Test
    public void onAppOpenedEmptyTokenSkipsRequest() throws Exception {
        registrationPrefs.pushToken().set("");

        deviceRegistrar.onAppOpened();
        awaitNetworkExecutor();

        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
    }

    // Verifies that onAppOpened skips request when last registration is within the cooldown window.
    @Test
    public void onAppOpenedWithinCooldownSkipsRequest() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 60_000L);

        deviceRegistrar.onAppOpened();
        awaitNetworkExecutor();

        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
        Assert.assertTrue(workManager
                .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                .get()
                .isEmpty());
    }

    // Verifies that onAppOpened sends registerDevice again once the cooldown window has elapsed.
    @Test
    public void onAppOpenedAfterCooldownSendsAgain() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(11));
        stubSyncSuccess();

        deviceRegistrar.onAppOpened();
        awaitNetworkExecutor();

        verify(requestManager, times(1)).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
    }

    // Verifies that a delivery that went out right away leaves no RegisterDeviceWorker behind:
    // a scheduled one would send a duplicate registerDevice.
    @Test
    public void onAppOpenedDeliveredRightAwaySchedulesNoWorker() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        stubSyncSuccess();

        deviceRegistrar.onAppOpened();
        awaitNetworkExecutor();

        verify(requestManager, times(1)).sendRequestSync(any(PushRequest.class));
        Assert.assertTrue(workManager
                .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                .get()
                .isEmpty());
    }

    // Verifies that onAppCodeChanged sends registerDevice even inside the cooldown window: an
    // application code change is not subject to the cooldown at all.
    @Test
    public void onAppCodeChangedSendsWithinCooldown() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 60_000L);
        stubSyncSuccess();

        deviceRegistrar.onAppCodeChanged();
        awaitNetworkExecutor();

        verify(requestManager, times(1)).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
    }

    // The in-flight request was built under the old application code.
    @Test
    public void onAppCodeChangedSchedulesDeliveryWhenOneIsInFlight() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        CountDownLatch inFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            inFlight.countDown();
            release.await(5, TimeUnit.SECONDS);
            return Result.fromData(null);
        });
        Thread delivery = new Thread(() -> deviceRegistrar.deliverFromWorker(null));

        try {
            delivery.start();
            Assert.assertTrue(inFlight.await(5, TimeUnit.SECONDS));

            deviceRegistrar.onAppCodeChanged();
            awaitNetworkExecutor();

            List<WorkInfo> infos = workManager
                    .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                    .get();
            Assert.assertEquals(1, infos.size());
            Assert.assertEquals(WorkInfo.State.ENQUEUED, infos.get(0).getState());
            verify(requestManager, times(1)).sendRequestSync(any(PushRequest.class));
        } finally {
            release.countDown();
            delivery.join(5000);
        }
    }

    // A code change re-delivers the saved token whether or not registerForPushNotifications was called.
    @Test
    public void onAppCodeChangedSendsEvenWhenNotRegisteredForPush() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.isRegisteredForPush().set(false);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis());
        stubSyncSuccess();

        deviceRegistrar.onAppCodeChanged();
        awaitNetworkExecutor();

        verify(requestManager, times(1)).sendRequestSync(any(PushRequest.class));
    }

    // After the worker delivers under a new app code, the next app open stays inside the cooldown.
    @Test
    public void appOpenAfterWorkerDeliveryUnderNewCodeSendsNothingExtra() throws Exception {
        final String oldCode = "OLD_CODE";
        final String newCode = "NEW_CODE";
        registrationPrefs.applicationId().set(oldCode);
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.isRegisteredForPush().set(true);
        stubSyncSuccess();

        new AppCodeApplier(registrationPrefs).apply(newCode, null);
        deviceRegistrar.deliverFromWorker(null);
        // Back-date the delivery the worker just recorded: the exact millisecond it used is a race
        // against Calendar's strict before() in the cooldown check below.
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 1000L);
        deviceRegistrar.onAppOpened();
        awaitNetworkExecutor();

        verify(requestManager, times(1)).sendRequestSync(any(PushRequest.class));
    }

    // App code change with a process restart: the pref zeroed by removeAppId outlives the process.
    @Test
    public void appCodeChangedThenNewProcessAppOpenSendsUnderNewCode() throws Exception {
        final String oldCode = "OLD_CODE";
        final String newCode = "NEW_CODE";
        registrationPrefs.applicationId().set(oldCode);
        registrationPrefs.pushToken().set(TEST_ID);
        stubSyncSuccess();

        deviceRegistrar.deliverFromWorker(null);
        new AppCodeApplier(registrationPrefs).apply(newCode, null);
        new DeviceRegistrar().onAppOpened();
        awaitNetworkExecutor();

        verify(requestManager, times(2)).sendRequestSync(pushRequestArgumentCaptor.capture());
        JSONObject params = PushRequestHelper.getParams(pushRequestArgumentCaptor.getValue());
        Assert.assertEquals(newCode, params.getString("application"));
    }

    private static void notificationsEnabled(boolean enabled) {
        Application application = RuntimeEnvironment.getApplication();
        shadowOf((NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE))
                .setNotificationsEnabled(enabled);
    }

    // Verifies that onPermissionStatusChanged sends registerDevice inside the cooldown where onAppOpened
    // stays silent, carrying the tracker's status, which is remembered once the server took it.
    //
    // The mock request manager never calls PushRequest#getParams() the way the real one does (it just returns
    // the stubbed Result), so the stub builds params itself here to populate getSentPushPermissionStatus()
    // before deliverFromWorker reads it — otherwise the permission status would never reach the request.
    @Test
    public void onPermissionStatusChangedSendsWithinCooldown() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 60_000L);
        registrationPrefs.pushPermissionLastSent().set("not_determined");
        registrationPrefs.pushPermissionGrantedViaSdkPrompt().set(true);
        notificationsEnabled(true);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            PushRequestHelper.getParams(invocation.getArgument(0));
            return Result.fromData(null);
        });

        deviceRegistrar.onPermissionStatusChanged(() -> {});
        awaitNetworkExecutor();

        verify(requestManager).sendRequestSync(pushRequestArgumentCaptor.capture());
        PushRequest<?> request = pushRequestArgumentCaptor.getValue();
        Assert.assertEquals("registerDevice", request.getMethod());
        Assert.assertEquals(
                "granted_explicit", PushRequestHelper.getParams(request).getString("push_permission_status"));
        Assert.assertEquals(
                "granted_explicit", registrationPrefs.pushPermissionLastSent().get());
        Assert.assertTrue(registrationPrefs.registeredOnServer().get());
    }

    // Verifies that onDone runs once the request completes, also when it failed.
    @Test
    public void onPermissionStatusChangedRunsOnDoneAfterFailedRequest() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenReturn(Result.fromException(EXCEPTION));
        boolean[] done = {false};

        deviceRegistrar.onPermissionStatusChanged(() -> done[0] = true);
        awaitNetworkExecutor();

        verify(requestManager).sendRequestSync(any(PushRequest.class));
        Assert.assertTrue(done[0]);
    }

    // Verifies that without a push token nothing is sent and onDone runs right away.
    @Test
    public void onPermissionStatusChangedWithoutTokenRunsOnDoneAndSendsNothing() {
        registrationPrefs.pushToken().set("");
        boolean[] done = {false};

        deviceRegistrar.onPermissionStatusChanged(() -> done[0] = true);

        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
        Assert.assertTrue(done[0]);
    }

    // Behaviour change: before sendNow, an exception inside the delivery left statusInFlight stuck
    // until the process restarted, because onDone never ran.
    @Test
    public void onPermissionStatusChangedRunsOnDoneWhenDeliveryThrows() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenThrow(new RuntimeException("boom"));
        boolean[] done = {false};

        deviceRegistrar.onPermissionStatusChanged(() -> done[0] = true);
        awaitNetworkExecutor();

        Assert.assertTrue(done[0]);
    }

    // A changed push permission status that lands while a delivery is in flight must still reach the server:
    // the in-flight request carries the old status, so onPermissionStatusChanged hands the send over to the
    // worker instead of waiting on the shared network thread.
    @Test
    public void onPermissionStatusChangedSchedulesDeliveryWhenOneIsInFlight() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        CountDownLatch inFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            inFlight.countDown();
            release.await(5, TimeUnit.SECONDS);
            return Result.fromData(null);
        });
        Thread delivery = new Thread(() -> deviceRegistrar.deliverFromWorker(null));

        try {
            delivery.start();
            Assert.assertTrue(inFlight.await(5, TimeUnit.SECONDS));
            boolean[] done = {false};

            deviceRegistrar.onPermissionStatusChanged(() -> done[0] = true);
            awaitNetworkExecutor();

            Assert.assertTrue(done[0]);
            List<WorkInfo> infos = workManager
                    .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                    .get();
            Assert.assertEquals(1, infos.size());
            Assert.assertEquals(WorkInfo.State.ENQUEUED, infos.get(0).getState());
            verify(requestManager, times(1)).sendRequestSync(any(PushRequest.class));
        } finally {
            release.countDown();
            delivery.join(5000);
        }
    }

    // An app code or language change that lands while a delivery is in flight must not be lost: the in-flight
    // request carries the old prefs, so onAppOpened hands the send over to the worker instead of skipping.
    @Test
    public void onAppOpenedSchedulesDeliveryWhenOneIsInFlight() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        CountDownLatch inFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            inFlight.countDown();
            release.await(5, TimeUnit.SECONDS);
            return Result.fromData(null);
        });
        Thread delivery = new Thread(() -> deviceRegistrar.deliverFromWorker(null));

        try {
            delivery.start();
            Assert.assertTrue(inFlight.await(5, TimeUnit.SECONDS));

            deviceRegistrar.onAppOpened();
            awaitNetworkExecutor();

            List<WorkInfo> infos = workManager
                    .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                    .get();
            Assert.assertEquals(1, infos.size());
            Assert.assertEquals(WorkInfo.State.ENQUEUED, infos.get(0).getState());
            verify(requestManager, times(1)).sendRequestSync(any(PushRequest.class));
        } finally {
            release.countDown();
            delivery.join(5000);
        }
    }

    // Verifies that onServerCommunicationStarted enqueues a delivery worker instead of sending a
    // synchronous request, when there is a token to deliver.
    @Test
    public void onServerCommunicationStartedEnqueuesDeliveryWorker() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);

        deviceRegistrar.onServerCommunicationStarted();

        List<WorkInfo> infos =
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get();
        Assert.assertEquals(1, infos.size());
        Assert.assertEquals(WorkInfo.State.ENQUEUED, infos.get(0).getState());
        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
    }

    // Verifies that onServerCommunicationStarted does nothing without a push token to deliver.
    @Test
    public void onServerCommunicationStartedWithoutTokenEnqueuesNothing() throws Exception {
        registrationPrefs.pushToken().set("");

        deviceRegistrar.onServerCommunicationStarted();

        List<WorkInfo> infos =
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get();
        Assert.assertTrue(infos.isEmpty());
        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
    }

    // Verifies that onLanguageChanged sends registerDevice even inside the cooldown window: a
    // language change is not subject to the cooldown at all.
    @Test
    public void onLanguageChangedSendsWithinCooldown() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 60_000L);
        stubSyncSuccess();

        deviceRegistrar.onLanguageChanged();
        awaitNetworkExecutor();

        verify(requestManager, times(1)).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
    }

    // The 14-day freshness check in registerForPushesInternal reads lastPushRegistration
    // even on the no-token path, so it must still be zeroed when there is nothing to send.
    @Test
    public void onLanguageChangedWithoutTokenStillZeroesLastRegistration() throws Exception {
        registrationPrefs.pushToken().set("");
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis());

        deviceRegistrar.onLanguageChanged();
        awaitNetworkExecutor();

        Assert.assertEquals(0, registrationPrefs.lastPushRegistration().get());
        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
    }

    // onTokenReceived() part

    @Test
    public void onTokenReceivedWritesTokenThenEnqueuesWorker() throws Exception {
        deviceRegistrar.onTokenReceived(NEW_TOKEN, null);

        Assert.assertEquals(NEW_TOKEN, registrationPrefs.pushToken().get());
        List<WorkInfo> infos =
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get();
        Assert.assertEquals(1, infos.size());
        Assert.assertEquals(WorkInfo.State.ENQUEUED, infos.get(0).getState());
        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
    }

    // A process started by WorkManager can reach this before RepositoryModule has prefs; the waiting
    // registerExistingToken callback must still get an answer.
    @Test
    public void onTokenReceivedWithoutPrefsSendsErrorEvent() throws Exception {
        RepositoryModule.setRegistrationPreferences(null);
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);

        deviceRegistrar.onTokenReceived(NEW_TOKEN, null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(errorListener).onReceive(any());
        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
        Assert.assertTrue(workManager
                .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                .get()
                .isEmpty());
    }

    @Test
    public void onTokenReceivedCarriesTagsIntoWorkerInput() {
        String tagsJson = "{\"k\":\"v\"}";
        ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        WorkManager mockWorkManager = Mockito.mock(WorkManager.class);
        try (MockedStatic<WorkManager> workManagerMock = Mockito.mockStatic(WorkManager.class)) {
            workManagerMock
                    .when(() -> WorkManager.getInstance(any(Context.class)))
                    .thenReturn(mockWorkManager);

            deviceRegistrar.onTokenReceived(NEW_TOKEN, tagsJson);

            verify(mockWorkManager)
                    .enqueueUniqueWork(
                            eq(RegisterDeviceWorker.TAG), eq(ExistingWorkPolicy.REPLACE), requestCaptor.capture());
        }
        Data input = requestCaptor.getValue().getWorkSpec().input;
        Assert.assertEquals(tagsJson, input.getString(RegisterDeviceWorker.DATA_TAGS));
    }

    @Test
    public void onTokenReceivedWithoutTransportSendsErrorEventAndKeepsToken() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);
        Field instance = DeviceSpecificProvider.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previous = instance.get(null);
        instance.set(null, null);

        try {
            deviceRegistrar.onTokenReceived(NEW_TOKEN, null);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

            Assert.assertEquals(TEST_ID, registrationPrefs.pushToken().get());
            verify(errorListener).onReceive(any());
            Assert.assertTrue(workManager
                    .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                    .get()
                    .isEmpty());
        } finally {
            instance.set(null, previous);
        }
    }

    // Dedup lives in the FCM/HMS helpers, not here.
    @Test
    public void onTokenReceivedWithTheSameTokenStillEnqueuesWorker() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);

        deviceRegistrar.onTokenReceived(TEST_ID, null);

        List<WorkInfo> infos =
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get();
        Assert.assertEquals(1, infos.size());
        Assert.assertEquals(WorkInfo.State.ENQUEUED, infos.get(0).getState());
    }

    @Test
    public void deliverFromWorkerSuccessAppliesOutcome() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        stubSyncSuccess();
        EventListener<RegistrationSuccessEvent> successListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationSuccessEvent.class, successListener);

        Result<Void, NetworkException> result = deviceRegistrar.deliverFromWorker("{\"k\":\"v\"}");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertTrue(result.isSuccess());
        verify(requestManager).sendRequestSync(pushRequestArgumentCaptor.capture());
        JSONObject params = PushRequestHelper.getParams(pushRequestArgumentCaptor.getValue());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
        Assert.assertEquals(TEST_ID, params.getString("push_token"));
        Assert.assertEquals("v", params.getJSONObject("tags").getString("k"));
        Assert.assertTrue(registrationPrefs.registeredOnServer().get());
        Assert.assertTrue(registrationPrefs.lastPushRegistration().get() > 0);
        ArgumentCaptor<RegistrationSuccessEvent> event = ArgumentCaptor.forClass(RegistrationSuccessEvent.class);
        verify(successListener).onReceive(event.capture());
        Assert.assertEquals(TEST_ID, event.getValue().getData().getToken());
    }

    @Test
    public void deliverySuccessRecordsDeliveredToken() {
        registrationPrefs.pushToken().set(TEST_ID);
        stubSyncSuccess();

        deviceRegistrar.deliverFromWorker(null);

        Assert.assertEquals(TEST_ID, registrationPrefs.deliveredPushToken().get());
    }

    @Test
    public void isDeliveredFalseForEmptyTokenOnFreshInstall() {
        Assert.assertFalse(deviceRegistrar.isDelivered(""));
    }

    @Test
    public void isDeliveredFalseAfterTokenRotation() {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.deliveredPushToken().set(TEST_ID);

        registrationPrefs.pushToken().set(NEW_TOKEN);

        Assert.assertFalse(deviceRegistrar.isDelivered(NEW_TOKEN));
    }

    @Test
    public void isDeliveredFalseAfterPushTokenWiped() {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.deliveredPushToken().set(TEST_ID);

        registrationPrefs.pushToken().set("");

        Assert.assertFalse(deviceRegistrar.isDelivered(TEST_ID));
    }

    @Test
    public void unregisterWithServerClearsDeliveredToken() {
        registrationPrefs.deliveredPushToken().set(TEST_ID);
        stubSyncSuccess();

        deviceRegistrar.unregisterWithServer(TEST_ID);

        Assert.assertEquals("", registrationPrefs.deliveredPushToken().get());
    }

    @Test
    public void unregisterWithExplicitAppCodeKeepsDeliveredToken() {
        registrationPrefs.deliveredPushToken().set(TEST_ID);

        deviceRegistrar.unregisterWithServer(TEST_ID, URL, "APP_CODE");

        Assert.assertEquals(TEST_ID, registrationPrefs.deliveredPushToken().get());
    }

    @Test
    public void deliverFromWorkerFailureSendsErrorEventAndKeepsPrefs() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenReturn(Result.fromException(EXCEPTION));
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);

        Result<Void, NetworkException> result = deviceRegistrar.deliverFromWorker(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertFalse(result.isSuccess());
        Assert.assertSame(EXCEPTION, result.getException());
        verify(errorListener).onReceive(any());
        Assert.assertFalse(registrationPrefs.registeredOnServer().get());
        Assert.assertEquals(0L, registrationPrefs.lastPushRegistration().get());
    }

    @Test
    public void deliverFromWorkerWithoutTokenReturnsFailureResult() {
        registrationPrefs.pushToken().set("");

        Result<Void, NetworkException> result = deviceRegistrar.deliverFromWorker(null);

        Assert.assertFalse(result.isSuccess());
        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
    }

    // A token that changed while the request was in flight is not applied;
    // the next delivery sends the new token.
    @Test
    public void deliverFromWorkerStaleTokenOutcomeNotApplied() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            registrationPrefs.pushToken().set(NEW_TOKEN);
            return Result.fromData(null);
        });
        EventListener<RegistrationSuccessEvent> successListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationSuccessEvent.class, successListener);

        deviceRegistrar.deliverFromWorker(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertFalse(registrationPrefs.registeredOnServer().get());
        Assert.assertEquals(0L, registrationPrefs.lastPushRegistration().get());
        verify(successListener, never()).onReceive(any());

        stubSyncSuccess();
        deviceRegistrar.deliverFromWorker(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(requestManager, times(2)).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                NEW_TOKEN,
                PushRequestHelper.getParams(pushRequestArgumentCaptor.getValue())
                        .getString("push_token"));
        Assert.assertTrue(registrationPrefs.registeredOnServer().get());
    }

    // The old app code's answer must not stamp lastPushRegistration under the new one: with the
    // re-registration still queued in memory, a process death would leave the cooldown to hide it.
    @Test
    public void deliverFromWorkerStaleAppCodeOutcomeNotApplied() throws Exception {
        registrationPrefs.applicationId().set("OLD_CODE");
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            new AppCodeApplier(registrationPrefs).apply("NEW_CODE", null);
            return Result.fromData(null);
        });
        EventListener<RegistrationSuccessEvent> successListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationSuccessEvent.class, successListener);

        Result<Void, NetworkException> result = deviceRegistrar.deliverFromWorker(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(0L, registrationPrefs.lastPushRegistration().get());
        Assert.assertEquals("", registrationPrefs.deliveredPushToken().get());
        Assert.assertFalse(registrationPrefs.registeredOnServer().get());
        verify(successListener, never()).onReceive(any());

        stubSyncSuccess();
        deviceRegistrar.onAppOpened();
        awaitNetworkExecutor();

        verify(requestManager, times(2)).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                "NEW_CODE",
                PushRequestHelper.getParams(pushRequestArgumentCaptor.getValue())
                        .getString("application"));
        Assert.assertTrue(registrationPrefs.registeredOnServer().get());
    }

    // A first app code set mid-flight is not a change: nothing re-delivers after it.
    @Test
    public void deliverFromWorkerFirstAppCodeSetMidFlightKeepsOutcome() {
        registrationPrefs.applicationId().set("");
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            new AppCodeApplier(registrationPrefs).apply("FIRST_CODE", null);
            return Result.fromData(null);
        });
        EventListener<RegistrationSuccessEvent> successListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationSuccessEvent.class, successListener);

        Result<Void, NetworkException> result = deviceRegistrar.deliverFromWorker(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(registrationPrefs.registeredOnServer().get());
        Assert.assertEquals(TEST_ID, registrationPrefs.deliveredPushToken().get());
        Assert.assertTrue(registrationPrefs.lastPushRegistration().get() > 0);
        verify(successListener).onReceive(any());
    }

    // Verifies that a delivery overtaken by a token rotation reports success and swallows the
    // request's own outcome: RegisterDeviceWorker reads isSuccess() on it, the newer delivery reports.
    @Test
    public void deliverFromWorkerRotatedMidFlightReportsSuccess() {
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            registrationPrefs.pushToken().set(NEW_TOKEN);
            return Result.fromException(EXCEPTION);
        });
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);

        Result<Void, NetworkException> result = deviceRegistrar.deliverFromWorker(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isSuccess());
        verify(errorListener, never()).onReceive(any());
    }

    // Verifies that without a push transport module nothing is sent and the waiting side gets
    // a RegistrationErrorEvent instead of hanging.
    @Test
    public void deliverFromWorkerWithoutTransportSendsErrorEventAndNoRequest() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);
        Field instance = DeviceSpecificProvider.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previous = instance.get(null);
        instance.set(null, null);

        try {
            Result<Void, NetworkException> result = deviceRegistrar.deliverFromWorker(null);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

            Assert.assertFalse(result.isSuccess());
            Assert.assertEquals(
                    "can't register device: no push transport module",
                    result.getException().getMessage());
            verify(errorListener).onReceive(any());
            verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
        } finally {
            instance.set(null, previous);
        }
    }

    @Test
    public void cancelScheduledRegistrationCancelsDeliveryWork() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        deviceRegistrar.onServerCommunicationStarted();
        UUID id = workManager
                .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                .get()
                .get(0)
                .getId();

        deviceRegistrar.cancelScheduledRegistration();

        Assert.assertEquals(
                WorkInfo.State.CANCELLED, workManager.getWorkInfoById(id).get().getState());
    }

    // A WorkManager failure at enqueue time does not leave a waiting callback hanging.
    @Test
    public void onServerCommunicationStartedScheduleFailureSendsErrorEvent() {
        registrationPrefs.pushToken().set(TEST_ID);
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);
        try (MockedStatic<WorkManager> workManagerMock = Mockito.mockStatic(WorkManager.class)) {
            workManagerMock
                    .when(() -> WorkManager.getInstance(any(Context.class)))
                    .thenThrow(new IllegalStateException("WorkManager is not initialized"));

            deviceRegistrar.onServerCommunicationStarted();
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
        verify(errorListener).onReceive(any());
    }

    // A tags payload that doesn't fit in a WorkManager Data (Data.MAX_DATA_BYTES) must fail
    // scheduling the same way a WorkManager enqueue failure does, not throw out of onTokenReceived.
    @Test
    public void onTokenReceivedWithOversizedTagsSendsErrorEvent() throws Exception {
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);
        String oversizedTags = repeat("a", 11_000);

        deviceRegistrar.onTokenReceived(NEW_TOKEN, oversizedTags);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(errorListener).onReceive(any());
        List<WorkInfo> infos =
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get();
        Assert.assertTrue(infos.isEmpty());
    }

    // Verifies that registerDevice carries the values read when the request was created, not the ones
    // found in prefs and in the transport at serialization time.
    @Test
    public void registerDeviceCarriesValuesCapturedWhenRequestWasCreated() throws Exception {
        RepositoryModule.getNotificationPreferences()
                .isCollectingDeviceLocaleAllowed()
                .set(true);
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.applicationId().set("APP-1");
        registrationPrefs.hwid().set("HW-1");
        registrationPrefs.setLanguage("es");
        registrationPrefs.setLocale("es-MX");
        int deviceType = DeviceSpecificProvider.getInstance().deviceType();
        JSONObject[] sent = new JSONObject[1];
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            registrationPrefs.pushToken().set(NEW_TOKEN);
            registrationPrefs.applicationId().set("APP-2");
            registrationPrefs.hwid().set("HW-2");
            registrationPrefs.setLanguage("fr");
            registrationPrefs.setLocale("fr-CA");
            DeviceSpecificProvider otherTransport = Mockito.mock(DeviceSpecificProvider.class);
            when(otherTransport.deviceType()).thenReturn(deviceType + 1);
            try (MockedStatic<DeviceSpecificProvider> provider =
                    Mockito.mockStatic(DeviceSpecificProvider.class, Mockito.CALLS_REAL_METHODS)) {
                provider.when(DeviceSpecificProvider::getInstance).thenReturn(otherTransport);
                sent[0] = PushRequestHelper.getParams(invocation.getArgument(0));
            }
            return Result.fromData(null);
        });

        deviceRegistrar.deliverFromWorker(null);

        Assert.assertEquals("APP-1", sent[0].getString("application"));
        Assert.assertEquals("HW-1", sent[0].getString("hwid"));
        Assert.assertEquals("es", sent[0].getString("language"));
        Assert.assertEquals("es-MX", sent[0].getString("locale"));
        Assert.assertEquals(TEST_ID, sent[0].getString("push_token"));
        Assert.assertEquals(deviceType, sent[0].getInt("device_type"));
    }

    // Verifies that a token cleared between the empty-token guard and the snapshot is not sent as an empty one.
    @Test
    public void registerDeviceIsNotSentWhenTokenIsClearedBeforeSnapshotIsTaken() {
        PreferenceStringValue clearedAfterFirstRead = Mockito.mock(PreferenceStringValue.class);
        when(clearedAfterFirstRead.get()).thenReturn(TEST_ID, "");
        RegistrationPrefs prefs = Mockito.spy(registrationPrefs);
        when(prefs.pushToken()).thenReturn(clearedAfterFirstRead);
        RepositoryModule.setRegistrationPreferences(prefs);
        try {
            deviceRegistrar.deliverFromWorker(null);
        } finally {
            RepositoryModule.setRegistrationPreferences(registrationPrefs);
        }

        verify(requestManager, never()).sendRequestSync(any());
        Assert.assertFalse(registrationPrefs.registeredOnServer().get());
    }

    // Verifies that an empty HWID does not stop the delivery: the snapshot stores values as read, unvalidated.
    @Test
    public void registerDeviceIsSentWhenHwidIsEmpty() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.hwid().set("");
        stubSyncSuccess();

        deviceRegistrar.deliverFromWorker(null);

        verify(requestManager).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
        Assert.assertEquals(
                "",
                PushRequestHelper.getParams(pushRequestArgumentCaptor.getValue())
                        .getString("hwid"));
        Assert.assertTrue(registrationPrefs.registeredOnServer().get());
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
