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
import android.test.suitebuilder.annotation.SmallTest;

import com.pushwoosh.exception.GetTagsException;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.exception.UnregisterForPushNotificationException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.tags.TagsBundle;
import com.pushwoosh.testutil.CallbackWrapper;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by aevstefeev on 27/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@SmallTest
public class GDPRManagerTest {
    public static final String TEST_EXCEPTION_STRING = "test exception";
    public static final PostEventException TEST_EXCEPTION = new PostEventException(TEST_EXCEPTION_STRING);
    private GDPRManager gdprManager;

    @Mock
    private PushwooshRepository pushwooshRepositoryMock;
    @Mock
    private PushwooshNotificationManager pushwooshNotificationManagerMock;
    @Mock
    private PushwooshInAppImpl pushwooshInAppImpl;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(pushwooshRepositoryMock.isGdprEnable()).thenReturn(true);

        gdprManager = new GDPRManager(
                pushwooshRepositoryMock,
                pushwooshNotificationManagerMock,
                pushwooshInAppImpl);
    }

    @Test
    public void gdpNotAvailable() {
        when(pushwooshRepositoryMock.isGdprEnable()).thenReturn(false);

        gdprManager.setCommunicationEnabled(true, null);
        gdprManager.setCommunicationEnabled(false, null);
        gdprManager.removeAllDeviceData(null);

        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.setCommunicationEnabled(true, callback);
        gdprManager.setCommunicationEnabled(false, callback);
        gdprManager.removeAllDeviceData(callback);

        verify(pushwooshInAppImpl, never()).postEvent(anyString(), any(), any());

        ArgumentCaptor<Result<Void, PushwooshException>> callbackArgumentCaptor =
                ArgumentCaptor.forClass(Result.class);

        verify(callback, times(3)).process(callbackArgumentCaptor.capture());
        for (Result result : callbackArgumentCaptor.getAllValues()) {
            PushwooshException exception = result.getException();
            Assert.assertEquals("The GDPR solution isn’t available for this account", exception.getMessage());
        }
    }

    @Test
    public void enableCommunication() {
        gdprManager.setCommunicationEnabled(true, null);

        emulatePostEvenComumnication(true, Result.fromData(null));
        emulateSuccessRegister();

        verify(pushwooshRepositoryMock).communicationEnabled(true);
    }

    @NonNull
    private ArgumentCaptor<Callback<Void, PostEventException>> assertAndReturnCallbackArgumentCaptor(boolean enable) {
        ArgumentCaptor<TagsBundle> tagsBundleArgumentCaptor = ArgumentCaptor.forClass(TagsBundle.class);
        ArgumentCaptor<Callback<Void, PostEventException>> callbackArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshInAppImpl).postEvent(eq("GDPRConsent"), tagsBundleArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        TagsBundle tagsBundle = tagsBundleArgumentCaptor.getValue();
        Assert.assertEquals(enable, tagsBundle.getBoolean("channel", !enable));
        return callbackArgumentCaptor;
    }

    private ArgumentCaptor<Callback<Void, PostEventException>> assertAndReturnCallbackArgumentCaptor() {
        return assertAndReturnCallbackArgumentCaptor(false);
    }

    @Test
    public void enableCommunicationCallBack() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();

        gdprManager.setCommunicationEnabled(true, callback);

        emulatePostEvenComumnication(true, Result.fromData(null));
        emulateSuccessRegister();

        verify(pushwooshRepositoryMock).communicationEnabled(true);

        checckSuccess(callback);
    }

    private void emulateSuccessRegister() {
        ArgumentCaptor<Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> argumentCaptorReg =
                ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshNotificationManagerMock).registerForPushNotifications(argumentCaptorReg.capture(), eq(true));
        argumentCaptorReg.getValue().process(Result.fromData(null));
    }

    private void checckSuccess(Callback<Void, PushwooshException> callback) {
        ArgumentCaptor<Result<Void, PushwooshException>> argumentCaptorResult = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(argumentCaptorResult.capture());
        Assert.assertTrue(argumentCaptorResult.getValue().isSuccess());
    }

    @Test
    public void enableCommunicationCallBackErrorRegister() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.setCommunicationEnabled(true, callback);

        emulatePostEvenComumnication(true, Result.fromData(null));
        emulateFailRegister();

        verify(pushwooshRepositoryMock).communicationEnabled(true);
        assertFail(callback);
    }

    @Test
    public void enableCommunicationCallBackErrorPostEvent() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.setCommunicationEnabled(true, callback);

        emulatePostEvenComumnication(true, Result.fromException(new RegisterForPushNotificationsException(TEST_EXCEPTION_STRING)));
        verify(pushwooshNotificationManagerMock, never()).registerForPushNotifications(any(), eq(true));

        verify(pushwooshRepositoryMock, never()).communicationEnabled(anyBoolean());
        assertFail(callback);
    }

    private void emulateFailRegister() {
        ArgumentCaptor<Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> argumentCaptorReg =
                ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshNotificationManagerMock).registerForPushNotifications(argumentCaptorReg.capture(), eq(true));
        RegisterForPushNotificationsException exception = new RegisterForPushNotificationsException(TEST_EXCEPTION_STRING);
        argumentCaptorReg.getValue().process(Result.fromException(exception));
    }

    private void emulatePostEvenComumnication(boolean enable, Result result) {
        ArgumentCaptor<Callback<Void, PostEventException>> callbackPostEventArgumentCaptor
                = assertAndReturnCallbackArgumentCaptor(enable);
        Callback callbackPostEvent = callbackPostEventArgumentCaptor.getValue();
        callbackPostEvent.process(result);
    }


    @Test
    public void disableCommunication() {
        gdprManager.setCommunicationEnabled(false, null);

        emulatePostEventCommunicationDisable();
        emulateSuccessUnregister();

        verify(pushwooshRepositoryMock).communicationEnabled(false);
    }

    @Test
    public void disableCommunicationCallBackErrorRegister() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.setCommunicationEnabled(false, callback);

        emulatePostEvenComumnication(false, Result.fromData(null));
        emulateFailUnregister();

        verify(pushwooshRepositoryMock).communicationEnabled(false);
        assertFail(callback);
    }

    @Test
    public void disableCommunicationCallBackErrorPostEvent() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.setCommunicationEnabled(false, callback);

        emulatePostEvenComumnication(false, Result.fromException(new UnregisterForPushNotificationException(TEST_EXCEPTION_STRING)));
        verify(pushwooshNotificationManagerMock, never()).unregisterForPushNotifications(any());
        verify(pushwooshNotificationManagerMock, never()).registerForPushNotifications(any(), eq(true));

        verify(pushwooshRepositoryMock, never()).communicationEnabled(anyBoolean());
        assertFail(callback);
    }

    private void emulatePostEventCommunicationDisable() {
        ArgumentCaptor<Callback<Void, PostEventException>> callbackArgumentCaptor = assertAndReturnCallbackArgumentCaptor();
        Callback callback = callbackArgumentCaptor.getValue();
        callback.process(Result.fromData(null));
    }


    @Test
    public void disableCommunicationServerError() {
        gdprManager.setCommunicationEnabled(false, null);

        ArgumentCaptor<Callback<Void, PostEventException>> callbackArgumentCaptor = assertAndReturnCallbackArgumentCaptor();

        processWithException(callbackArgumentCaptor);

        verify(pushwooshNotificationManagerMock, never()).unregisterForPushNotifications(null);
        verify(pushwooshRepositoryMock, never()).communicationEnabled(false);
    }

    private void processWithException(ArgumentCaptor<Callback<Void, PostEventException>> callbackArgumentCaptor) {
        Callback<Void, PostEventException> callback = callbackArgumentCaptor.getValue();
        PostEventException exception = new PostEventException("some expection");
        Result<Void, PostEventException> result = Result.fromException(exception);
        callback.process(result);
    }

    @Test
    public void removeAllDeviceData() {
        gdprManager.removeAllDeviceData(null);

        emulateSuccessPostEvent();
        emulateSuccessGetTags();
        emulateSuccessSendTags();
        emulateSuccessUnregister();

        verify(pushwooshNotificationManagerMock).unregisterForPushNotifications(any());
        verify(pushwooshRepositoryMock).removeAllDeviceData();
    }

    private void emulateSuccessSendTags() {
        ArgumentCaptor<Callback<Void, PushwooshException>> callbackSendTagArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshRepositoryMock).sendTags(any(TagsBundle.class), callbackSendTagArgumentCaptor.capture());
        callbackSendTagArgumentCaptor.getValue().process(Result.fromData(null));
    }

    private void emulateSuccessGetTags() {
        ArgumentCaptor<Callback<TagsBundle, GetTagsException>> callbackSendTagArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshRepositoryMock).getTags(callbackSendTagArgumentCaptor.capture());

        TagsBundle tagsBundle =
                new TagsBundle.Builder()
                .putString("1", "212")
                .putInt("2", 2)
                .build();

        callbackSendTagArgumentCaptor.getValue().process(Result.fromData(tagsBundle));
    }

    private void emulateSuccessPostEvent() {
        ArgumentCaptor<Callback<Void, PostEventException>> callbackPostEventArgumentCaptor = assertAndReturnCallbackArgumentCaptorRemoveDeviceData();
        Callback callbackPostEvent = callbackPostEventArgumentCaptor.getValue();
        callbackPostEvent.process(Result.fromData(null));
    }

    @Test
    public void removeAllDeviceDataCallBack() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.removeAllDeviceData(callback);

        emulateSuccessPostEvent();
        emulateSuccessGetTags();
        emulateSuccessSendTags();
        emulateSuccessUnregister();

        verify(pushwooshRepositoryMock).removeAllDeviceData();

        checckSuccess(callback);
    }

    private void emulateSuccessUnregister() {
        ArgumentCaptor<Callback<String, UnregisterForPushNotificationException>> registrationArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshNotificationManagerMock).unregisterForPushNotifications(registrationArgumentCaptor.capture());
        registrationArgumentCaptor.getValue().process(Result.fromData(null));
    }

    @Test
    public void removeAllDeviceDataCallBackErrorUnregister() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.removeAllDeviceData(callback);

        emulateSuccessPostEvent();
        emulateSuccessGetTags();
        emulateSuccessSendTags();
        emulateFailUnregister();

        verify(pushwooshRepositoryMock, never()).removeAllDeviceData();

        assertFail(callback);
    }

    private void emulateFailUnregister() {
        ArgumentCaptor<Callback<String, UnregisterForPushNotificationException>> registrationArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshNotificationManagerMock).unregisterForPushNotifications(registrationArgumentCaptor.capture());
        registrationArgumentCaptor.getValue().process(Result.fromException(new UnregisterForPushNotificationException(TEST_EXCEPTION_STRING)));
    }

    @Test
    public void removeAllDeviceDataCallBackErrorSendTag() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
        gdprManager.removeAllDeviceData(callback);

        emulateSuccessPostEvent();
        emulateSuccessGetTags();
        emulateFailSendTag();

        verify(pushwooshNotificationManagerMock, never()).unregisterForPushNotifications(any());
        verify(pushwooshRepositoryMock, never()).removeAllDeviceData();

        assertFail(callback);
    }

    private void emulateFailSendTag() {
        ArgumentCaptor<Callback<Void, PushwooshException>> callbackSendTagArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshRepositoryMock).sendTags(any(TagsBundle.class), callbackSendTagArgumentCaptor.capture());
        callbackSendTagArgumentCaptor.getValue().process(Result.fromException(TEST_EXCEPTION));
    }

    @Test
    public void removeAllDeviceDataCallBackErrorGetTag() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();

        gdprManager.removeAllDeviceData(callback);

        emulateSuccessPostEvent();
        emulateFailGetTags();

        verify(pushwooshRepositoryMock, never()).sendTags(any(TagsBundle.class), any());
        verify(pushwooshNotificationManagerMock, never()).unregisterForPushNotifications(any());
        verify(pushwooshRepositoryMock, never()).removeAllDeviceData();

        assertFail(callback);
    }

    private void emulateFailGetTags() {
        ArgumentCaptor<Callback<TagsBundle, GetTagsException>> callbackSendTagArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshRepositoryMock).getTags(callbackSendTagArgumentCaptor.capture());
        callbackSendTagArgumentCaptor.getValue().process(Result.fromException(new GetTagsException(TEST_EXCEPTION_STRING)));
    }

    @Test
    public void removeAllDeviceDataCallBackErrorPostEvent() {
        Callback<Void, PushwooshException> callback = CallbackWrapper.spy();

        gdprManager.removeAllDeviceData(callback);

        emulateFailPostEvent();

        verify(pushwooshRepositoryMock, never()).sendTags(any(TagsBundle.class), any());
        verify(pushwooshNotificationManagerMock, never()).unregisterForPushNotifications(any());
        verify(pushwooshRepositoryMock, never()).removeAllDeviceData();

        assertFail(callback);
    }

    private void emulateFailPostEvent() {
        ArgumentCaptor<Callback<Void, PostEventException>> callbackPostEventArgumentCaptor = assertAndReturnCallbackArgumentCaptorRemoveDeviceData();
        Callback callbackPostEvent = callbackPostEventArgumentCaptor.getValue();
        callbackPostEvent.process(Result.fromException(TEST_EXCEPTION));
    }

    private void assertFail(Callback<Void, PushwooshException> callback) {
        ArgumentCaptor<Result<Void, PushwooshException>> argumentCaptorResult = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(argumentCaptorResult.capture());
        Result<Void, PushwooshException> result = argumentCaptorResult.getValue();
        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(TEST_EXCEPTION_STRING, result.getException().getMessage());
    }

    @Test
    public void removeAllDeviceDataServerError() {
        gdprManager.removeAllDeviceData(null);

        ArgumentCaptor<Callback<Void, PostEventException>> callbackArgumentCaptor = assertAndReturnCallbackArgumentCaptorRemoveDeviceData();
        processWithException(callbackArgumentCaptor);

        verify(pushwooshRepositoryMock, never()).removeAllDeviceData();
    }

    @NonNull
    private ArgumentCaptor<Callback<Void, PostEventException>> assertAndReturnCallbackArgumentCaptorRemoveDeviceData() {
        ArgumentCaptor<TagsBundle> tagsBundleArgumentCaptor = ArgumentCaptor.forClass(TagsBundle.class);
        ArgumentCaptor<Callback<Void, PostEventException>> callbackArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(pushwooshInAppImpl).postEvent(eq("GDPRDelete"), tagsBundleArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        TagsBundle tagsBundle = tagsBundleArgumentCaptor.getValue();
        Assert.assertEquals(true, tagsBundle.getBoolean("status", false));
        return callbackArgumentCaptor;
    }
}