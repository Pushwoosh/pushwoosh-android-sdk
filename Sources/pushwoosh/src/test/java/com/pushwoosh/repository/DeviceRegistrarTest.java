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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class DeviceRegistrarTest {
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
        if (subscribe != null)
            subscribe.unsubscribe();
    }

    @Test
    public void registerWithServer() throws JSONException {
        DeviceRegistrar.registerWithServer(TEST_ID);

        checkNormalReg();
    }

    private void checkNormalReg() {
        subscribe = EventBus.subscribe(RegistrationSuccessEvent.class, event -> {
            RegisterForPushNotificationsResultData resultData = (RegisterForPushNotificationsResultData) event.getData();
            Assert.assertEquals(TEST_ID, resultData.getToken());
        });

        verify(requestManager).sendRequest(pushRequestArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        callbackArgumentCaptor.getValue().process(Result.fromData(null));

        Assert.assertEquals("registerDevice", pushRequestArgumentCaptor.getValue().getMethod());

        Assert.assertEquals(true, registrationPrefs.registeredOnServer().get());

        long l = registrationPrefs.lastPushRegistration().get();
        Assert.assertTrue(l > 0);
    }

    @Test
    public void registerWithServerError() {
        DeviceRegistrar.registerWithServer(TEST_ID);
        registrationPrefs.lastPushRegistration().set(1000L);

        checkFeilReg();

    }

    private void checkFeilReg() {
        Subscription<RegistrationErrorEvent> subscribe =
                EventBus.subscribe(RegistrationErrorEvent.class, event -> Assert.assertEquals(TEST_EXCEPTION, event.getData()));
        verify(requestManager).sendRequest(pushRequestArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        Assert.assertEquals("registerDevice", pushRequestArgumentCaptor.getValue().getMethod());
        callbackArgumentCaptor.getValue().process(Result.fromException(EXCEPTION));
        subscribe.unsubscribe();
    }

    @Test
    public void unregisterWithServer() {
        unregisterServerStart();

        Subscription<DeregistrationSuccessEvent> subscribe =
                EventBus.subscribe(DeregistrationSuccessEvent.class, event -> Assert.assertEquals(TEST_ID, event.getData()));

        verify(requestManager).sendRequest(pushRequestArgumentCaptor.capture(), eq(URL), callbackArgumentCaptor.capture());
        callbackArgumentCaptor.getValue().process(Result.fromData(0));

        checkOftenResult();
        Assert.assertEquals(0, registrationPrefs.lastPushRegistration().get());
        subscribe.unsubscribe();
    }

    private void checkOftenResult() {
        Assert.assertEquals(false, registrationPrefs.registeredOnServer().get());
        Assert.assertEquals("unregisterDevice", pushRequestArgumentCaptor.getValue().getMethod());
    }

    private void unregisterServerStart() {
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.lastPushRegistration().set(1000L);

        DeviceRegistrar.unregisterWithServer(TEST_ID, URL);
    }


    @Test
    public void unregisterWithServerError() {
        unregisterServerStart();

        Subscription<DeregistrationErrorEvent> subscribe =
                EventBus.subscribe(DeregistrationErrorEvent.class, event -> Assert.assertEquals(TEST_EXCEPTION, event.getData()));

        verify(requestManager).sendRequest(pushRequestArgumentCaptor.capture(), eq(URL), callbackArgumentCaptor.capture());
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
}