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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.text.TextUtils;

import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.event.DeregistrationErrorEvent;
import com.pushwoosh.notification.event.DeregistrationSuccessEvent;
import com.pushwoosh.notification.event.RegistrationErrorEvent;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class DeviceRegistrarTest {
    public static final String TAG = "DeviceRegistrarTest";
    public static final String TEST_ID = "testId";
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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();

        NetworkModule.setRequestManager(requestManager);
        registrationPrefs = platformTestManager.getRegistrationPrefs();
        deviceRegistrar = new DeviceRegistrar();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
        if (subscribe != null) subscribe.unsubscribe();
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
    public void updateRegistration() {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(false);

        deviceRegistrar.updateRegistration();

        checkNormalReg();
    }

    @Test
    public void updateRegistrationForceUpdate() {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(true);

        deviceRegistrar.updateRegistration();

        checkNormalReg();
    }

    @Test
    public void updateRegistrationError() {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(false);

        deviceRegistrar.updateRegistration();

        checkFeilReg();
    }

    // Verifies that registerWithServerWithRetries sends a registerDevice request via RequestManager.
    @Test
    public void registerWithServerWithRetriesSendsRequest() {
        deviceRegistrar.registerWithServerWithRetries(
                TEST_ID, null, DeviceSpecificProvider.getInstance().deviceType(), result -> {});

        verify(requestManager).sendRequest(pushRequestArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        Assert.assertEquals(
                "registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
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

    // Verifies that single-arg unregisterWithServer clears registeredOnServer and delegates
    // with null baseUrl and the stored app code.
    @Test
    public void unregisterWithServerSingleArgUsesNullBaseUrl() {
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.applicationId().set("STORED_CODE");

        deviceRegistrar.unregisterWithServer(TEST_ID);

        Assert.assertFalse(registrationPrefs.registeredOnServer().get());
        verify(requestManager)
                .sendRequest(
                        pushRequestArgumentCaptor.capture(), isNull(String.class), callbackArgumentCaptor.capture());
        UnregisterDeviceRequest request = (UnregisterDeviceRequest) pushRequestArgumentCaptor.getValue();
        Assert.assertEquals("STORED_CODE", request.getApplicationId());
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
    public void updateRegistrationEmptyTokenSkipsRequest() {
        registrationPrefs.pushToken().set("");
        registrationPrefs.forceRegister().set(false);

        deviceRegistrar.updateRegistration();

        verify(requestManager, never()).sendRequest(any(PushRequest.class), any(Callback.class));
    }

    // Verifies that updateRegistration skips request when last registration is within the cooldown window.
    @Test
    public void updateRegistrationWithinCooldownSkipsRequest() {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(false);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 60_000L);

        deviceRegistrar.updateRegistration();

        verify(requestManager, never()).sendRequest(any(PushRequest.class), any(Callback.class));
    }

    // Verifies that updateRegistration consumes the forceRegister flag (resets it to false after read).
    @Test
    public void updateRegistrationConsumesForceRegisterFlag() {
        registrationPrefs.pushToken().set(TEST_ID);
        registrationPrefs.forceRegister().set(true);

        deviceRegistrar.updateRegistration();

        Assert.assertFalse(registrationPrefs.forceRegister().get());
    }
}
