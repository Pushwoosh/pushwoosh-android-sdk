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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.FakeRequestManager;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
    private Callback<Void, PushwooshException> listener;

    @Mock
    private Callback<Void, PushwooshException> listener2;

    private AutoCloseable mocks;
    private FakeRequestManager fake;
    private SendTagsProcessor processor;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        fake = FakeRequestManager.install();
        processor = new SendTagsProcessor();
    }

    @After
    public void tearDown() throws Exception {
        NetworkModule.setRequestManager(null);
        mocks.close();
    }

    @Test
    public void sendTags_singleInvocation_callsRequestManagerAndDeliversSuccess() throws Exception {
        fake.respondWith("setTags", new JSONObject());

        processor.sendTags(new JSONObject().put("k", "v"), listener);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, fake.count("setTags"));
        fake.assertAllScripted();

        ArgumentCaptor<Result<Void, PushwooshException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(listener, times(1)).process(resultCaptor.capture());
        assertTrue(resultCaptor.getValue().isSuccess());
        assertNull(resultCaptor.getValue().getException());
    }

    @Test
    public void sendTags_multipleInvocationsInWindow_mergesIntoSingleRequestAndFansOut() throws Exception {
        fake.respondWith("setTags", new JSONObject());

        processor.sendTags(new JSONObject().put("a", 1), listener);
        processor.sendTags(new JSONObject().put("b", 2), listener2);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, fake.count("setTags"));
        fake.assertAllScripted();

        // Reflection, not Sent.params: no platform fixture here, so getParams() throws on getHwid()
        // and the fake records params as null. SetTagsRequest.tags is private with no accessor.
        Field tagsField = SetTagsRequest.class.getDeclaredField("tags");
        tagsField.setAccessible(true);
        JSONObject mergedTags = (JSONObject) tagsField.get(fake.last("setTags").request);
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

    @Test
    public void sendTags_sdkNotInitialized_deliversNetworkExceptionToAllListeners() throws Exception {
        NetworkModule.setRequestManager(null);

        processor.sendTags(new JSONObject().put("a", 1), listener);
        processor.sendTags(new JSONObject().put("b", 2), listener2);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        ArgumentCaptor<Result<Void, PushwooshException>> r1 = ArgumentCaptor.forClass(Result.class);
        verify(listener, times(1)).process(r1.capture());
        assertFalse(r1.getValue().isSuccess());
        assertTrue(r1.getValue().getException() instanceof NetworkException);
        assertEquals("SDK is not initialized", r1.getValue().getException().getMessage());

        ArgumentCaptor<Result<Void, PushwooshException>> r2 = ArgumentCaptor.forClass(Result.class);
        verify(listener2, times(1)).process(r2.capture());
        assertFalse(r2.getValue().isSuccess());
        assertTrue(r2.getValue().getException() instanceof NetworkException);
    }

    @Test
    public void sendTags_requestManagerFails_propagatesSameExceptionToAllListeners() throws Exception {
        NetworkException boom = new NetworkException("boom");

        fake.failWith("setTags", boom);

        processor.sendTags(new JSONObject().put("a", 1), listener);
        processor.sendTags(new JSONObject().put("b", 2), listener2);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, fake.count("setTags"));
        fake.assertAllScripted();

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
