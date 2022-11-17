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

package com.pushwoosh;

import androidx.annotation.NonNull;
import okhttp3.Response;

import android.test.suitebuilder.annotation.MediumTest;

import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceJsonObjectValue;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.SendTagsProcessor;
import com.pushwoosh.tags.TagsBundle;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.PlatformTestManager;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by aevstefeev on 27/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@MediumTest
public class GDPRManagerIntegretionTest {
    public static final String TEST_EXPECTION_STRING = "test_expection";
    public static final PostEventException EXCEPTION = new PostEventException(TEST_EXPECTION_STRING);
    private GDPRManager gdprManager;

    private PushwooshRepository pushwooshRepository;
    private PushwooshNotificationManager pushwooshNotificationManager;
    private PlatformTestManager platformTestManager;

    @Mock
    private PushwooshInAppImpl pushwooshInAppImplMock;

    @Mock
    private RegistrationPrefs registrationPrefs;
    @Mock
    private NotificationPrefs notificationPrefs;
    @Mock
    private PushRegistrar pushRegistrarMock;
    @Mock
    private SendTagsProcessor sendTagsProcessorMock;
    @Mock
    private RequestManager requestManager;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);
        Config config = MockConfig.createMock();

        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();

        pushwooshNotificationManager = new PushwooshNotificationManager(pushRegistrarMock, config);

        when(registrationPrefs.setTagsFailed()).thenReturn(mock(PreferenceBooleanValue.class));
        when(registrationPrefs.communicationEnable()).thenReturn(mock(PreferenceBooleanValue.class));
        when(registrationPrefs.removeAllDeviceData()).thenReturn(mock(PreferenceBooleanValue.class));
        when(registrationPrefs.gdprEnable()).thenReturn(mock(PreferenceBooleanValue.class));
        when(registrationPrefs.gdprEnable().get()).thenReturn(true);

        when(notificationPrefs.tags()).thenReturn(mock(PreferenceJsonObjectValue.class));
        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        pushwooshRepository = new PushwooshRepository(requestManager, sendTagsProcessorMock, registrationPrefs, notificationPrefs, null, null, serverCommunicationManager);

        gdprManager = new GDPRManager(
                pushwooshRepository,
                pushwooshNotificationManager,
                pushwooshInAppImplMock);
    }

    @After
    public void tearDown() {
        platformTestManager.tearDown();
    }

    @Test
    public void gdprDisable() throws Exception {
        when(registrationPrefs.gdprEnable().get()).thenReturn(false);

        gdprManager.setCommunicationEnabled(true, null);
        gdprManager.setCommunicationEnabled(false, null);
        gdprManager.removeAllDeviceData(null);

        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.setCommunicationEnabled(true, callback);
        gdprManager.setCommunicationEnabled(false, callback);
        gdprManager.removeAllDeviceData(callback);

        ArgumentCaptor<Result> callbackArgumentCaptor =
                ArgumentCaptor.forClass(Result.class);

        verify(callback, times(3)).process(callbackArgumentCaptor.capture());
        for(Result result : callbackArgumentCaptor.getAllValues()){
            PushwooshException exception = result.getException();
            Assert.assertEquals("The GDPR solution isn’t available for this account", exception.getMessage());
        }
        verify(pushwooshInAppImplMock, never()).postEvent(anyString(), any(), any());
        verify(registrationPrefs.communicationEnable(), never()).set(anyBoolean());
        verify(registrationPrefs.removeAllDeviceData(), never()).set(anyBoolean());
    }

    @Test
    public void enableCommunication() throws Exception {

        gdprManager.setCommunicationEnabled(true, null);

        emulatePostEventCommunication(true, Result.fromData(null));

        verify(registrationPrefs.communicationEnable()).set(true);
        verify(pushRegistrarMock).checkDevice(eq("testAppId"));
        verify(pushRegistrarMock).registerPW();
    }

    @Test
    public void enableCommunicationServerError() throws Exception {
        gdprManager.setCommunicationEnabled(true, null);

        emulatePostEventCommunication(true, Result.fromException(EXCEPTION));

        assertNothingHappensForCommunicationCase();
    }

    @Test
    public void enableCommunicationServerErrorPostEventCallback() throws Exception {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.setCommunicationEnabled(true, callback);

        emulatePostEventCommunication(true, Result.fromException(EXCEPTION));

        assertNothingHappensForCommunicationCase();
        assertFail(callback);
    }

    @Test
    public void disableCommunication() throws Exception {
        gdprManager.setCommunicationEnabled(false, null);

        emulatePostEventCommunication(false, Result.fromData(null));

        verify(registrationPrefs.communicationEnable()).set(false);
        verify(pushRegistrarMock).unregisterPW();
    }

    @Test
    public void disableCommunicationServerError() throws Exception {
        gdprManager.setCommunicationEnabled(false, null);

        emulatePostEventCommunication(false, Result.fromException(EXCEPTION));

        assertNothingHappensForCommunicationCase();
    }

    private void assertNothingHappensForCommunicationCase() throws Exception {
        verify(pushRegistrarMock, never()).unregisterPW();
        verify(registrationPrefs.communicationEnable(), never()).set(anyBoolean());
        verify(pushRegistrarMock, never()).checkDevice(any());
    }

    @Test
    public void disableCommunicationServerErrorPostEventCallback() throws Exception {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.setCommunicationEnabled(false, callback);

        emulatePostEventCommunication(false, Result.fromException(EXCEPTION));

        assertNothingHappensForCommunicationCase();
        assertFail(callback);
    }


    @NonNull
    private void emulatePostEventCommunication(boolean enable, Result<Void, PostEventException> result) {
        ArgumentCaptor<TagsBundle> tagsBundleArgumentCaptor = ArgumentCaptor.forClass(TagsBundle.class);
        ArgumentCaptor<Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(Callback.class);

        verify(pushwooshInAppImplMock).postEvent(eq("GDPRConsent"), tagsBundleArgumentCaptor.capture(), callbackArgumentCaptor.capture());

        TagsBundle tagsBundle = tagsBundleArgumentCaptor.getValue();
        Assert.assertEquals(enable, tagsBundle.getBoolean("channel", !enable));

        Callback callback = callbackArgumentCaptor.getValue();
        callback.process(result);
    }

    @Test
    public void removeAllDeviceData() throws Exception {
        gdprManager.removeAllDeviceData(null);

        emulatePostEventRemoveDate(Result.fromData(null));
        TagsBundle build =
                new TagsBundle
                .Builder()
                .putString("1","1")
                .putString("2","22")
                .build();
        emulateGetTag(Result.fromData(build));
        emulateSendTag(Result.fromData(null));

        verify(pushRegistrarMock).unregisterPW();
        ArgumentCaptor<JSONObject> jsonObjectArgumentCaptor1 = ArgumentCaptor.forClass(JSONObject.class);
        verify(notificationPrefs.tags()).set(jsonObjectArgumentCaptor1.capture());
        List<JSONObject> values = jsonObjectArgumentCaptor1.getAllValues();
        Assert.assertEquals("{\"1\":\"1\",\"2\":\"22\"}", values.get(0).toString());
//        verify(notificationPrefs.tags()).set(null);
        ArgumentCaptor<JSONObject> jsonObjectArgumentCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(notificationPrefs.tags()).merge(jsonObjectArgumentCaptor.capture());
        Assert.assertEquals("{\"1\":null,\"2\":null}", jsonObjectArgumentCaptor.getValue().toString());
//        verify(registrationPrefs.removeAllDeviceData()).set(true);
    }

    @NonNull
    private void emulateSendTag(Result result) {
        ArgumentCaptor<Callback> callbackSandTagArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        ArgumentCaptor<JSONObject> jsonObjectArgumentCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(sendTagsProcessorMock).sendTags(jsonObjectArgumentCaptor.capture(), callbackSandTagArgumentCaptor.capture());
        Assert.assertEquals("{\"1\":null,\"2\":null}", jsonObjectArgumentCaptor.getValue().toString());
        callbackSandTagArgumentCaptor.getValue().process(result);
    }

    @NonNull
    private void emulateGetTag(Result result) throws Exception {
        ArgumentCaptor<Callback> callbackSandTagArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        ArgumentCaptor<PushRequest> jsonObjectArgumentCaptor = ArgumentCaptor.forClass(PushRequest.class);
        verify(requestManager).sendRequest(jsonObjectArgumentCaptor.capture(), callbackSandTagArgumentCaptor.capture());
        PushRequest<Response> request = jsonObjectArgumentCaptor.getValue();
        JSONObject jsonObject = new JSONObject();

        Assert.assertEquals("getTags", request.getMethod());
        callbackSandTagArgumentCaptor.getValue().process(result);
    }

    @Test
    public void removeAllDeviceDataServerErrorPostEvent() {
        gdprManager.removeAllDeviceData(null);

        emulatePostEventRemoveDate(Result.fromException(EXCEPTION));

        assertNotHappensRemoveDevice();
    }

    private void assertNotHappensRemoveDevice() {
        verify(notificationPrefs.tags(), never()).set(any());
        verify(registrationPrefs.removeAllDeviceData(), never()).set(anyBoolean());
        verify(pushRegistrarMock, never()).unregisterPW();
    }

    @Test
    public void removeAllDeviceDataServerErrorPostEventCallBack() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.removeAllDeviceData(callback);

        emulatePostEventRemoveDate(Result.fromException(EXCEPTION));

        assertNotHappensRemoveDevice();
        assertFail(callback);
    }

    @Test
    public void removeAllDeviceDataServerErrorSendTagCallBack() throws Exception {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.removeAllDeviceData(callback);

        emulatePostEventRemoveDate(Result.fromData(null));
        TagsBundle build =
                new TagsBundle
                        .Builder()
                        .putString("1","1")
                        .putString("2","22")
                        .build();
        emulateGetTag(Result.fromData(build));
        emulateSendTag(Result.fromException(EXCEPTION));

        ArgumentCaptor<JSONObject> jsonObjectArgumentCaptor1 = ArgumentCaptor.forClass(JSONObject.class);
        verify(notificationPrefs.tags()).set(jsonObjectArgumentCaptor1.capture());
        List<JSONObject> values = jsonObjectArgumentCaptor1.getAllValues();
        Assert.assertEquals("{\"1\":\"1\",\"2\":\"22\"}", values.get(0).toString());
      //  verify(registrationPrefs.removeAllDeviceData()).set(true);
        verify(pushRegistrarMock, never()).unregisterPW();

        assertFail(callback);
    }

    @Test
    public void removeAllDeviceDataCallBack() throws Exception {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.removeAllDeviceData(callback);

        emulatePostEventRemoveDate(Result.fromData(null));
        TagsBundle build =
                new TagsBundle
                        .Builder()
                        .putString("1","1")
                        .putString("2","22")
                        .build();
        emulateGetTag(Result.fromData(build));
        emulateSendTag(Result.fromData(null));

        ArgumentCaptor<JSONObject> jsonObjectArgumentCaptor1 = ArgumentCaptor.forClass(JSONObject.class);
        verify(notificationPrefs.tags()).set(jsonObjectArgumentCaptor1.capture());
        List<JSONObject> values = jsonObjectArgumentCaptor1.getAllValues();
        Assert.assertEquals("{\"1\":\"1\",\"2\":\"22\"}", values.get(0).toString());
       // verify(registrationPrefs.removeAllDeviceData()).set(true);
        verify(pushRegistrarMock).unregisterPW();
        //todo assert callback result
        // assertSuccess(callback);
    }

    private void assertSuccess(Callback<Void, PushwooshException> callback) {
        ArgumentCaptor<Result> argumentCaptorResult
                = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(argumentCaptorResult.capture());
        Result<Void, PushwooshException> result = argumentCaptorResult.getValue();
        Assert.assertTrue(result.isSuccess());
    }

    private void assertFail(Callback<Void, PushwooshException> callback) {
        ArgumentCaptor<Result> argumentCaptorResult
                = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(argumentCaptorResult.capture());
        Result<Void, PushwooshException> result = argumentCaptorResult.getValue();
        Assert.assertEquals(TEST_EXPECTION_STRING, result.getException().getMessage());
    }

    @NonNull
    private void emulatePostEventRemoveDate(Result<Void, PostEventException> result) {
        ArgumentCaptor<TagsBundle> tagsBundleArgumentCaptor = ArgumentCaptor.forClass(TagsBundle.class);
        ArgumentCaptor<Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshInAppImplMock).postEvent(eq("GDPRDelete"), tagsBundleArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        TagsBundle tagsBundle = tagsBundleArgumentCaptor.getValue();
        Assert.assertEquals(true, tagsBundle.getBoolean("status", false));
        Callback<Void, PostEventException> value = callbackArgumentCaptor.getValue();
        value.process(result);
    }


}