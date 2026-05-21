/*
 *
 * Copyright (c) 2025. Pushwoosh Inc. (http://www.pushwoosh.com)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class SendTagsProcessorTest {

    @Mock
    private RequestManager requestManager;

    @Mock
    private Callback<Void, PushwooshException> listener;

    @Mock
    private Callback<Void, PushwooshException> listener2;

    private SendTagsProcessor processor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new SendTagsProcessor();
    }

    private static void stubRequestManagerSuccess(RequestManager requestManager) {
        doAnswer(invocation -> {
                    Callback<Void, NetworkException> cb = invocation.getArgument(1);
                    cb.process(Result.fromData(null));
                    return null;
                })
                .when(requestManager)
                .sendRequest(any(SetTagsRequest.class), any());
    }

    private static void stubRequestManagerFailure(RequestManager requestManager, NetworkException error) {
        doAnswer(invocation -> {
                    Callback<Void, NetworkException> cb = invocation.getArgument(1);
                    cb.process(Result.fromException(error));
                    return null;
                })
                .when(requestManager)
                .sendRequest(any(SetTagsRequest.class), any());
    }

    @Test
    public void sendTags_singleInvocation_callsRequestManagerAndDeliversSuccess() throws Exception {
        try (MockedStatic<NetworkModule> netMock = Mockito.mockStatic(NetworkModule.class)) {
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManager);

            stubRequestManagerSuccess(requestManager);

            processor.sendTags(new JSONObject().put("k", "v"), listener);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

            ArgumentCaptor<SetTagsRequest> reqCaptor = ArgumentCaptor.forClass(SetTagsRequest.class);
            verify(requestManager).sendRequest(reqCaptor.capture(), any());

            ArgumentCaptor<Result<Void, PushwooshException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
            verify(listener, times(1)).process(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            assertNull(resultCaptor.getValue().getException());
        }
    }

    @Test
    public void sendTags_multipleInvocationsInWindow_mergesIntoSingleRequestAndFansOut() throws Exception {
        try (MockedStatic<NetworkModule> netMock = Mockito.mockStatic(NetworkModule.class)) {
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManager);

            stubRequestManagerSuccess(requestManager);

            processor.sendTags(new JSONObject().put("a", 1), listener);
            processor.sendTags(new JSONObject().put("b", 2), listener2);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

            ArgumentCaptor<SetTagsRequest> reqCaptor = ArgumentCaptor.forClass(SetTagsRequest.class);
            verify(requestManager, times(1)).sendRequest(reqCaptor.capture(), any());

            Field tagsField = SetTagsRequest.class.getDeclaredField("tags");
            tagsField.setAccessible(true);
            JSONObject mergedTags = (JSONObject) tagsField.get(reqCaptor.getValue());
            assertTrue(mergedTags.has("a"));
            assertTrue(mergedTags.has("b"));
            assertEquals(1, mergedTags.getInt("a"));
            assertEquals(2, mergedTags.getInt("b"));

            ArgumentCaptor<Result<Void, PushwooshException>> r1 = ArgumentCaptor.forClass(Result.class);
            verify(listener, times(1)).process(r1.capture());
            assertTrue(r1.getValue().isSuccess());

            ArgumentCaptor<Result<Void, PushwooshException>> r2 = ArgumentCaptor.forClass(Result.class);
            verify(listener2, times(1)).process(r2.capture());
            assertTrue(r2.getValue().isSuccess());
        }
    }

    @Test
    public void sendTags_requestManagerIsNull_deliversNetworkExceptionToAllListeners() throws Exception {
        try (MockedStatic<NetworkModule> netMock = Mockito.mockStatic(NetworkModule.class)) {
            netMock.when(NetworkModule::getRequestManager).thenReturn(null);

            processor.sendTags(new JSONObject().put("a", 1), listener);
            processor.sendTags(new JSONObject().put("b", 2), listener2);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

            ArgumentCaptor<Result<Void, PushwooshException>> r1 = ArgumentCaptor.forClass(Result.class);
            verify(listener, times(1)).process(r1.capture());
            assertFalse(r1.getValue().isSuccess());
            assertTrue(r1.getValue().getException() instanceof NetworkException);
            assertEquals("Request manager is null", r1.getValue().getException().getMessage());

            ArgumentCaptor<Result<Void, PushwooshException>> r2 = ArgumentCaptor.forClass(Result.class);
            verify(listener2, times(1)).process(r2.capture());
            assertFalse(r2.getValue().isSuccess());
            assertTrue(r2.getValue().getException() instanceof NetworkException);

            verifyNoInteractions(requestManager);
        }
    }

    @Test
    public void sendTags_requestManagerFails_propagatesSameExceptionToAllListeners() throws Exception {
        NetworkException boom = new NetworkException("boom");

        try (MockedStatic<NetworkModule> netMock = Mockito.mockStatic(NetworkModule.class)) {
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManager);

            stubRequestManagerFailure(requestManager, boom);

            processor.sendTags(new JSONObject().put("a", 1), listener);
            processor.sendTags(new JSONObject().put("b", 2), listener2);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

            verify(requestManager, times(1)).sendRequest(any(SetTagsRequest.class), any());

            ArgumentCaptor<Result<Void, PushwooshException>> r1 = ArgumentCaptor.forClass(Result.class);
            verify(listener, times(1)).process(r1.capture());
            assertFalse(r1.getValue().isSuccess());
            assertSame(boom, r1.getValue().getException());

            ArgumentCaptor<Result<Void, PushwooshException>> r2 = ArgumentCaptor.forClass(Result.class);
            verify(listener2, times(1)).process(r2.capture());
            assertFalse(r2.getValue().isSuccess());
            assertSame(boom, r2.getValue().getException());
        }
    }
}
