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

package com.pushwoosh.inapp.view.js;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.webkit.WebView;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class PushManagerJSInterfaceTest {

    @Mock
    private WebView webViewMock;

    @Mock
    private JsCallback jsCallbackMock;

    @Mock
    private PushwooshPlatform platformMock;

    @Mock
    private PushwooshInAppImpl inAppMock;

    @Mock
    private Pushwoosh pushwooshMock;

    private PushManagerJSInterface pushManagerJSInterface;
    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        pushManagerJSInterface = new PushManagerJSInterface(webViewMock, jsCallbackMock);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // Verifies that sendTags parses valid JSON and forwards a TagsBundle to Pushwoosh.setTags.
    @Test
    public void sendTags_validJson_callsSetTagsOnPushwoosh() throws Exception {
        try (MockedStatic<Pushwoosh> pushwooshStatic = mockStatic(Pushwoosh.class)) {
            pushwooshStatic.when(Pushwoosh::getInstance).thenReturn(pushwooshMock);

            pushManagerJSInterface.sendTags("{\"IntTag\":42,\"StringTag\":\"s\"}");

            ArgumentCaptor<TagsBundle> bundleCaptor = ArgumentCaptor.forClass(TagsBundle.class);
            verify(pushwooshMock).setTags(bundleCaptor.capture());
            String json = bundleCaptor.getValue().toJson().toString();
            org.junit.Assert.assertTrue("contains IntTag=42, was " + json, json.contains("\"IntTag\":42"));
            org.junit.Assert.assertTrue("contains StringTag=s, was " + json, json.contains("\"StringTag\":\"s\""));
        }
    }

    // Verifies that sendTags swallows invalid JSON, logs an error, and does not call Pushwoosh.setTags.
    @Test
    public void sendTags_invalidJson_logsErrorAndDoesNotCallSetTags() {
        try (MockedStatic<Pushwoosh> pushwooshStatic = mockStatic(Pushwoosh.class);
                MockedStatic<PWLog> pwLog = mockStatic(PWLog.class)) {
            pushwooshStatic.when(Pushwoosh::getInstance).thenReturn(pushwooshMock);

            pushManagerJSInterface.sendTags("not a json");

            verify(pushwooshMock, never()).setTags(any(TagsBundle.class));
            pwLog.verify(() -> PWLog.error(
                    eq("Invalid tags format, expected object with string properties"), any(JSONException.class)));
        }
    }

    // Verifies that postEvent with valid blob without callbacks forwards event name and tags to SDK
    // and that the SDK callback - when invoked with success - does not touch the WebView.
    @Test
    public void postEvent_validBlobWithoutCallbacks_forwardsToSdkAndIgnoresResult() {
        try (MockedStatic<PushwooshPlatform> platformStatic = mockStatic(PushwooshPlatform.class)) {
            platformStatic.when(PushwooshPlatform::getInstance).thenReturn(platformMock);
            when(platformMock.pushwooshInApp()).thenReturn(inAppMock);

            pushManagerJSInterface.postEvent("{\"event\":\"myEvent\",\"attributes\":{\"k\":\"v\"}}");

            ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<TagsBundle> tagsCaptor = ArgumentCaptor.forClass(TagsBundle.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Callback<Void, PostEventException>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
            verify(inAppMock).postEvent(eventCaptor.capture(), tagsCaptor.capture(), callbackCaptor.capture());

            org.junit.Assert.assertEquals("myEvent", eventCaptor.getValue());
            String json = tagsCaptor.getValue().toJson().toString();
            org.junit.Assert.assertTrue("contains k=v, was " + json, json.contains("\"k\":\"v\""));

            callbackCaptor.getValue().process(Result.fromData(null));
            verifyNoInteractions(webViewMock);
        }
    }

    // Verifies that postEvent loads the success JS into the WebView when the SDK callback succeeds.
    @Test
    public void postEvent_successCallback_loadsSuccessJs() {
        try (MockedStatic<PushwooshPlatform> platformStatic = mockStatic(PushwooshPlatform.class)) {
            platformStatic.when(PushwooshPlatform::getInstance).thenReturn(platformMock);
            when(platformMock.pushwooshInApp()).thenReturn(inAppMock);

            pushManagerJSInterface.postEvent("{\"event\":\"e\",\"attributes\":{},\"success\":\"onOk\"}");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Callback<Void, PostEventException>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
            verify(inAppMock).postEvent(any(), any(), callbackCaptor.capture());

            callbackCaptor.getValue().process(Result.fromData(null));

            verify(webViewMock).loadUrl("javascript:onOk();");
        }
    }

    // Verifies that postEvent loads the error JS with the exception message when the SDK callback fails.
    @Test
    public void postEvent_errorCallback_loadsErrorJsWithMessage() {
        try (MockedStatic<PushwooshPlatform> platformStatic = mockStatic(PushwooshPlatform.class)) {
            platformStatic.when(PushwooshPlatform::getInstance).thenReturn(platformMock);
            when(platformMock.pushwooshInApp()).thenReturn(inAppMock);

            pushManagerJSInterface.postEvent(
                    "{\"event\":\"e\",\"attributes\":{},\"success\":\"onOk\",\"error\":\"onErr\"}");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Callback<Void, PostEventException>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
            verify(inAppMock).postEvent(any(), any(), callbackCaptor.capture());

            Result<Void, PushwooshException> failure = Result.fromException(new PostEventException("boom"));
            @SuppressWarnings({"unchecked", "rawtypes"})
            Result<Void, PostEventException> failureCast = (Result) failure;
            callbackCaptor.getValue().process(failureCast);

            verify(webViewMock).loadUrl("javascript:onErr('boom');");
        }
    }

    // Restored from cross-check: covers the WebView WeakReference null-guard. If a refactor removes
    // the `if (webView != null)` check or swaps WeakReference for a strong ref, a callback fired
    // after the in-app dismissal (real-world race) would NPE instead of silently no-op.
    @Test
    public void postEvent_webViewGcCollected_doesNotThrow() {
        try (MockedStatic<PushwooshPlatform> platformStatic = mockStatic(PushwooshPlatform.class)) {
            platformStatic.when(PushwooshPlatform::getInstance).thenReturn(platformMock);
            when(platformMock.pushwooshInApp()).thenReturn(inAppMock);

            PushManagerJSInterface jsInterface = new PushManagerJSInterface(null, jsCallbackMock);

            jsInterface.postEvent("{\"event\":\"e\",\"attributes\":{},\"success\":\"onOk\"}");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Callback<Void, PostEventException>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
            verify(inAppMock).postEvent(any(), any(), callbackCaptor.capture());

            callbackCaptor.getValue().process(Result.fromData(null));

            verifyNoInteractions(webViewMock);
        }
    }

    // Verifies that postEvent with an invalid JSON blob logs an error and does not touch the SDK.
    @Test
    public void postEvent_invalidJson_logsErrorAndDoesNotCallSdk() {
        try (MockedStatic<PushwooshPlatform> platformStatic = mockStatic(PushwooshPlatform.class);
                MockedStatic<PWLog> pwLog = mockStatic(PWLog.class)) {

            pushManagerJSInterface.postEvent("{not json}");

            platformStatic.verifyNoInteractions();
            pwLog.verify(() -> PWLog.error(eq("Invalid arguments"), any(JSONException.class)));
        }
    }
}
