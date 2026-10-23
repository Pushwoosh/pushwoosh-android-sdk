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

import static com.pushwoosh.repository.DeviceRegistrar.areNotificationsEnabled;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;

import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.pushwoosh.RegisterForPushNotificationsResultData;
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
import com.pushwoosh.internal.registrar.RegisterDeviceWorker;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.event.DeregistrationErrorEvent;
import com.pushwoosh.notification.event.DeregistrationSuccessEvent;
import com.pushwoosh.notification.event.RegistrationErrorEvent;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;
import com.pushwoosh.testutil.EventListenerWrapper;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONException;
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
import java.util.Date;
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
    Subscription<RegistrationSuccessEvent> subscribe;

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
        if (subscribe != null) subscribe.unsubscribe();
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
    public void registerWithServer() throws JSONException {
        deviceRegistrar.registerWithServer(
                TEST_ID, null, DeviceSpecificProvider.getInstance().deviceType(), result -> {
                    if (result.isSuccess()) {
                        registrationPrefs.registeredOnServer().set(true);

                        EventBus.sendEvent(new RegistrationSuccessEvent(
                                new RegisterForPushNotificationsResultData(TEST_ID, areNotificationsEnabled())));
                        registrationPrefs.lastPushRegistration().set(new Date().getTime());
                        PWLog.info(TAG, "Registered for push notifications: " + TEST_ID);
                    } else {
                        String errorDescription = result.getException() == null
                                ? ""
                                : result.getException().getMessage();
                        if (TextUtils.isEmpty(errorDescription)) {
                            errorDescription = "Pushwoosh registration error";
                        }

                        PWLog.error(TAG, "Registration error: " + errorDescription);
                        EventBus.sendEvent(new RegistrationErrorEvent(errorDescription));
                    }
                });

        checkNormalReg();
    }

    private void checkNormalReg() {
        subscribe = EventBus.subscribe(RegistrationSuccessEvent.class, event -> {
            RegisterForPushNotificationsResultData resultData =
                    (RegisterForPushNotificationsResultData) event.getData();
            Assert.assertEquals(TEST_ID, resultData.getToken());
        });

        verify(requestManager).sendRequest(pushRequestArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        callbackArgumentCaptor.getValue().process(Result.fromData(null));

        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());

        Assert.assertEquals(true, registrationPrefs.registeredOnServer().get());

        long l = registrationPrefs.lastPushRegistration().get();
        Assert.assertTrue(l > 0);
    }

    @Test
    public void registerWithServerError() {
        deviceRegistrar.registerWithServer(
                TEST_ID, null, DeviceSpecificProvider.getInstance().deviceType(), result -> {
                    if (result.isSuccess()) {
                        registrationPrefs.registeredOnServer().set(true);

                        EventBus.sendEvent(new RegistrationSuccessEvent(
                                new RegisterForPushNotificationsResultData(TEST_ID, areNotificationsEnabled())));
                        registrationPrefs.lastPushRegistration().set(new Date().getTime());
                        PWLog.info(TAG, "Registered for push notifications: " + TEST_ID);
                    } else {
                        String errorDescription = result.getException() == null
                                ? ""
                                : result.getException().getMessage();
                        if (TextUtils.isEmpty(errorDescription)) {
                            errorDescription = "Pushwoosh registration error";
                        }

                        PWLog.error(TAG, "Registration error: " + errorDescription);
                        EventBus.sendEvent(new RegistrationErrorEvent(errorDescription));
                    }
                });
        registrationPrefs.lastPushRegistration().set(1000L);

        checkFeilReg();
    }

    private void checkFeilReg() {
        Subscription<RegistrationErrorEvent> subscribe = EventBus.subscribe(
                RegistrationErrorEvent.class, event -> Assert.assertEquals(TEST_EXCEPTION, event.getData()));
        verify(requestManager).sendRequest(pushRequestArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
        callbackArgumentCaptor.getValue().process(Result.fromException(EXCEPTION));
        subscribe.unsubscribe();
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

    @Test
    public void updateRegistration() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(false);
        stubSyncSuccess();

        deviceRegistrar.updateRegistration();
        awaitNetworkExecutor();

        verify(requestManager).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
        Assert.assertTrue(registrationPrefs.registeredOnServer().get());
        Assert.assertTrue(registrationPrefs.lastPushRegistration().get() > 0);
    }

    @Test
    public void updateRegistrationForceUpdate() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(true);
        stubSyncSuccess();

        deviceRegistrar.updateRegistration();
        awaitNetworkExecutor();

        verify(requestManager).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
        Assert.assertTrue(registrationPrefs.registeredOnServer().get());
        Assert.assertTrue(registrationPrefs.lastPushRegistration().get() > 0);
    }

    @Test
    public void updateRegistrationError() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(false);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenReturn(Result.fromException(EXCEPTION));

        deviceRegistrar.updateRegistration();
        awaitNetworkExecutor();

        verify(requestManager).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
        Assert.assertFalse(registrationPrefs.registeredOnServer().get());
    }

    // Verifies that registerWithServer delivers a terminal NetworkException to the callback when the
    // SDK is not initialized — the seam's Null Object manager answers instead of a null check.
    @Test
    public void registerWithServerSdkNotInitializedDeliversErrorToCallback() {
        NetworkModule.setRequestManager(null);

        List<Result<Void, NetworkException>> delivered = new ArrayList<>();

        deviceRegistrar.registerWithServer(
                TEST_ID, null, DeviceSpecificProvider.getInstance().deviceType(), delivered::add);

        Assert.assertEquals(1, delivered.size());
        Assert.assertFalse(delivered.get(0).isSuccess());
        Assert.assertEquals(
                "SDK is not initialized", delivered.get(0).getException().getMessage());
        verifyNoInteractions(requestManager);
    }

    // The never-built-platform state has no RegistrationPrefs either (RepositoryModule.init precedes
    // NetworkModule.init), so registerDevice must still answer with the seam's terminal error — building
    // the request must not dereference the prefs.
    @Test
    public void registerWithServerWithoutPrefsDeliversErrorInsteadOfNpe() {
        RegistrationPrefs saved = RepositoryModule.getRegistrationPreferences();
        NetworkModule.setRequestManager(null);
        RepositoryModule.setRegistrationPreferences(null);

        try {
            List<Result<Void, NetworkException>> delivered = new ArrayList<>();

            deviceRegistrar.registerWithServer(
                    TEST_ID, null, DeviceSpecificProvider.getInstance().deviceType(), delivered::add);

            Assert.assertEquals(1, delivered.size());
            Assert.assertFalse(delivered.get(0).isSuccess());
            Assert.assertEquals(
                    "SDK is not initialized", delivered.get(0).getException().getMessage());
        } finally {
            RepositoryModule.setRegistrationPreferences(saved);
        }
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
        Thread delivery = new Thread(() -> deviceRegistrar.registerSavedToken(null));
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

    // Verifies that updateRegistration skips request when pushToken is empty.
    @Test
    public void updateRegistrationEmptyTokenSkipsRequest() throws Exception {
        registrationPrefs.pushToken().set("");
        registrationPrefs.forceRegister().set(false);

        deviceRegistrar.updateRegistration();
        awaitNetworkExecutor();

        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
    }

    // Verifies that updateRegistration skips request when last registration is within the cooldown window.
    @Test
    public void updateRegistrationWithinCooldownSkipsRequest() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(false);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 60_000L);

        deviceRegistrar.updateRegistration();
        awaitNetworkExecutor();

        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
        Assert.assertTrue(workManager
                .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                .get()
                .isEmpty());
    }

    // Verifies that a pending forceRegister (app code or language change) sends registerDevice
    // even inside the cooldown window.
    @Test
    public void updateRegistrationForceRegisterBypassesCooldown() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(true);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 60_000L);
        stubSyncSuccess();

        deviceRegistrar.updateRegistration();
        awaitNetworkExecutor();

        verify(requestManager, times(1)).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
    }

    // Verifies that a delivery that went out right away leaves no RegisterDeviceWorker behind:
    // a scheduled one would send a duplicate registerDevice.
    @Test
    public void updateRegistrationDeliveredRightAwaySchedulesNoWorker() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(true);
        stubSyncSuccess();

        deviceRegistrar.updateRegistration();
        awaitNetworkExecutor();

        verify(requestManager, times(1)).sendRequestSync(any(PushRequest.class));
        Assert.assertTrue(workManager
                .getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG)
                .get()
                .isEmpty());
    }

    // Verifies that updateRegistration consumes the forceRegister flag (resets it to false after read).
    @Test
    public void updateRegistrationConsumesForceRegisterFlag() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(true);
        stubSyncSuccess();

        deviceRegistrar.updateRegistration();
        awaitNetworkExecutor();

        Assert.assertFalse(registrationPrefs.forceRegister().get());
    }

    private static void notificationsEnabled(boolean enabled) {
        Application application = RuntimeEnvironment.getApplication();
        shadowOf((NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE))
                .setNotificationsEnabled(enabled);
    }

    // Verifies that updateRegistrationNow sends registerDevice inside the cooldown where updateRegistration stays
    // silent, carrying the tracker's status, which is remembered once the server took it.
    //
    // The mock request manager never calls PushRequest#getParams() the way the real one does (it just returns
    // the stubbed Result), so the stub builds params itself here to populate getSentPushPermissionStatus()
    // before registerSavedToken reads it — otherwise the permission status would never reach the request.
    @Test
    public void updateRegistrationNowSendsWithinCooldown() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(false);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 60_000L);
        registrationPrefs.pushPermissionLastSent().set("not_determined");
        registrationPrefs.pushPermissionGrantedViaSdkPrompt().set(true);
        notificationsEnabled(true);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            PushRequestHelper.getParams(invocation.getArgument(0));
            return Result.fromData(null);
        });

        deviceRegistrar.updateRegistrationNow(() -> {});
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
    public void updateRegistrationNowRunsOnDoneAfterFailedRequest() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenReturn(Result.fromException(EXCEPTION));
        boolean[] done = {false};

        deviceRegistrar.updateRegistrationNow(() -> done[0] = true);
        awaitNetworkExecutor();

        verify(requestManager).sendRequestSync(any(PushRequest.class));
        Assert.assertTrue(done[0]);
    }

    // Verifies that without a push token nothing is sent and onDone runs right away.
    @Test
    public void updateRegistrationNowWithoutTokenSendsNothing() {
        registrationPrefs.pushToken().set("");
        boolean[] done = {false};

        deviceRegistrar.updateRegistrationNow(() -> done[0] = true);

        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
        Assert.assertTrue(done[0]);
    }

    // Verifies that updateRegistrationNow leaves a pending forceRegister for the scheduled updateRegistration.
    @Test
    public void updateRegistrationNowKeepsForceRegisterFlag() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(true);
        stubSyncSuccess();

        deviceRegistrar.updateRegistrationNow(() -> {});
        awaitNetworkExecutor();

        Assert.assertTrue(registrationPrefs.forceRegister().get());
    }

    // A changed push permission status that lands while a delivery is in flight must still reach the server:
    // the in-flight request carries the old status, so updateRegistrationNow hands the send over to the worker
    // instead of waiting on the shared network thread.
    @Test
    public void updateRegistrationNowSchedulesDeliveryWhenOneIsInFlight() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        CountDownLatch inFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            inFlight.countDown();
            release.await(5, TimeUnit.SECONDS);
            return Result.fromData(null);
        });
        Thread delivery = new Thread(() -> deviceRegistrar.registerSavedToken(null));

        try {
            delivery.start();
            Assert.assertTrue(inFlight.await(5, TimeUnit.SECONDS));
            boolean[] done = {false};

            deviceRegistrar.updateRegistrationNow(() -> done[0] = true);
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
    // request carries the old prefs, so updateRegistration hands the send over to the worker instead of skipping.
    @Test
    public void updateRegistrationSchedulesDeliveryWhenOneIsInFlight() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(true);
        CountDownLatch inFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            inFlight.countDown();
            release.await(5, TimeUnit.SECONDS);
            return Result.fromData(null);
        });
        Thread delivery = new Thread(() -> deviceRegistrar.registerSavedToken(null));

        try {
            delivery.start();
            Assert.assertTrue(inFlight.await(5, TimeUnit.SECONDS));

            deviceRegistrar.updateRegistration();
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

    @Test
    public void registerSavedTokenSuccessAppliesOutcome() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        stubSyncSuccess();
        EventListener<RegistrationSuccessEvent> successListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationSuccessEvent.class, successListener);

        Result<Void, NetworkException> result = deviceRegistrar.registerSavedToken("{\"k\":\"v\"}");
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
    public void registerSavedTokenFailureSendsErrorEventAndKeepsPrefs() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenReturn(Result.fromException(EXCEPTION));
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);

        Result<Void, NetworkException> result = deviceRegistrar.registerSavedToken(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertFalse(result.isSuccess());
        Assert.assertSame(EXCEPTION, result.getException());
        verify(errorListener).onReceive(any());
        Assert.assertFalse(registrationPrefs.registeredOnServer().get());
        Assert.assertEquals(0L, registrationPrefs.lastPushRegistration().get());
    }

    @Test
    public void registerSavedTokenWithoutTokenSendsNothing() {
        registrationPrefs.pushToken().set("");

        Result<Void, NetworkException> result = deviceRegistrar.registerSavedToken(null);

        Assert.assertFalse(result.isSuccess());
        verify(requestManager, never()).sendRequestSync(any(PushRequest.class));
    }

    // A token that changed while the request was in flight is not applied;
    // the next delivery sends the new token.
    @Test
    public void registerSavedTokenStaleTokenOutcomeNotApplied() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            registrationPrefs.pushToken().set(NEW_TOKEN);
            return Result.fromData(null);
        });
        EventListener<RegistrationSuccessEvent> successListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationSuccessEvent.class, successListener);

        deviceRegistrar.registerSavedToken(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertFalse(registrationPrefs.registeredOnServer().get());
        Assert.assertEquals(0L, registrationPrefs.lastPushRegistration().get());
        verify(successListener, never()).onReceive(any());

        stubSyncSuccess();
        deviceRegistrar.registerSavedToken(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(requestManager, times(2)).sendRequestSync(pushRequestArgumentCaptor.capture());
        Assert.assertEquals(
                NEW_TOKEN,
                PushRequestHelper.getParams(pushRequestArgumentCaptor.getValue())
                        .getString("push_token"));
        Assert.assertTrue(registrationPrefs.registeredOnServer().get());
    }

    // Verifies that a delivery overtaken by a token rotation reports success and swallows the
    // request's own outcome: RegisterDeviceWorker reads isSuccess() on it, the newer delivery reports.
    @Test
    public void registerSavedTokenRotatedMidFlightReportsSuccess() {
        registrationPrefs.pushToken().set(TEST_ID);
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenAnswer(invocation -> {
            registrationPrefs.pushToken().set(NEW_TOKEN);
            return Result.fromException(EXCEPTION);
        });
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);

        Result<Void, NetworkException> result = deviceRegistrar.registerSavedToken(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isSuccess());
        verify(errorListener, never()).onReceive(any());
    }

    // Verifies that without a push transport module nothing is sent and the waiting side gets
    // a RegistrationErrorEvent instead of hanging.
    @Test
    public void registerSavedTokenWithoutTransportSendsErrorEventAndNoRequest() throws Exception {
        registrationPrefs.pushToken().set(TEST_ID);
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);
        Field instance = DeviceSpecificProvider.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previous = instance.get(null);
        instance.set(null, null);

        try {
            Result<Void, NetworkException> result = deviceRegistrar.registerSavedToken(null);
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
    public void scheduleRegistrationEnqueuesUniqueDeliveryWork() throws Exception {
        deviceRegistrar.scheduleRegistration("{\"k\":\"v\"}");

        List<WorkInfo> infos =
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get();
        Assert.assertEquals(1, infos.size());
        Assert.assertEquals(WorkInfo.State.ENQUEUED, infos.get(0).getState());
    }

    @Test
    public void cancelScheduledRegistrationCancelsDeliveryWork() throws Exception {
        deviceRegistrar.scheduleRegistration(null);
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
    public void scheduleRegistrationFailureSendsErrorEvent() {
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);
        try (MockedStatic<WorkManager> workManagerMock = Mockito.mockStatic(WorkManager.class)) {
            workManagerMock
                    .when(() -> WorkManager.getInstance(any(Context.class)))
                    .thenThrow(new IllegalStateException("WorkManager is not initialized"));

            deviceRegistrar.scheduleRegistration(null);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
        verify(errorListener).onReceive(any());
    }

    // A tags payload that doesn't fit in a WorkManager Data (Data.MAX_DATA_BYTES) must fail
    // scheduling the same way a WorkManager enqueue failure does, not throw out of scheduleRegistration.
    @Test
    public void scheduleRegistrationWithOversizedTagsSendsErrorEvent() throws Exception {
        EventListener<RegistrationErrorEvent> errorListener = EventListenerWrapper.spy();
        EventBus.subscribe(RegistrationErrorEvent.class, errorListener);
        String oversizedTags = repeat("a", 11_000);

        deviceRegistrar.scheduleRegistration(oversizedTags);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(errorListener).onReceive(any());
        List<WorkInfo> infos =
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get();
        Assert.assertTrue(infos.isEmpty());
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
