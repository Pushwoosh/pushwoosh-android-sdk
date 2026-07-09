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

package com.pushwoosh.inapp.view.js;

import static org.junit.Assert.assertTrue;

import android.webkit.WebView;

import com.pushwoosh.PushwooshPlatform;

import org.junit.After;
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

// Regression guard for crash-jsinterface-invokecallback-null-arg.
// invokeCallback(String method, String args) used to do `args.replace("\"","\\\"")` (:346) with no
// null-guard. The synchronous catch of @JavascriptInterface postEvent (:303) / richMediaAction (:329)
// calls invokeCallback(errorCb, e.getLocalizedMessage()); on Android <12 that message is null (no
// helpful-NPE), so invokeCallback(cb, null) hit `null.replace(...)` -> a SECOND NPE, born inside the
// catch block with no SDK wrapper above the JavaBridge thread -> process crash. The fix null-guards the
// shared sink (null args -> ""), so the error callback is still routed to the WebView with an empty arg.
//
// Environment note (why the crash needs a stand-in for the null message). Probed on this host: on JDK 17
// (and Android 12+), the NPE from `new JSONObject((String) null)` carries a HELPFUL, non-null message,
// so the pre-12 null-message condition cannot be produced from the real JSONObject NPE here. The two
// catch-path tests inject an equivalent message-less NullPointerException from inside the SAME try-block,
// keeping the catch (:301/:327), the :303/:329 invokeCallback(errorCb, <null>) call, and the guarded
// :346 all real. nonNullMessage_realHelpfulNpe_doesNotCrash is the necessary-condition control: it drives
// the REAL raw trigger postEvent(evt, null, cb, cb) unmocked (non-null helpful message on this host).
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class PushwooshJSInterfaceInvokeCallbackNullArgTest {

    @Mock
    private WebView webViewMock;

    @Mock
    private JsCallback jsCallbackMock;

    private AutoCloseable mocks;
    private PushwooshJSInterface jsInterface;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        jsInterface = new PushwooshJSInterface(jsCallbackMock, webViewMock, null, null, null);
    }

    @After
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * postEvent whose try-block throws a null-message exception reaches the guarded
     * invokeCallback(errorCb, null): instead of an NPE at :346, the null arg is rendered as an empty
     * string and the error callback is routed to the WebView. The null-message exception is injected at
     * the real pushwooshInApp() call inside the same try, so the catch (:301) -> :303 -> :346 path is real.
     */
    @Test
    public void postEvent_nullMessageException_routesEmptyArgToWebView() {
        try (MockedStatic<PushwooshPlatform> platformStatic = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformStatic.when(PushwooshPlatform::getInstance).thenThrow(new NullPointerException());

            jsInterface.postEvent("evt", "{}", "successCb", "errorCb");
            ShadowLooper.idleMainLooper();

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(webViewMock).loadUrl(urlCaptor.capture());
            assertTrue(
                    "null error message must be routed to the WebView as an empty arg, not crash at :346",
                    urlCaptor.getValue().contains("_pwCallbackHelper.invokeCallback(\"errorCb\", \"\")"));
        }
    }

    /**
     * Necessary-condition control: driving the REAL raw trigger postEvent(evt, null, cb, cb) unmocked,
     * the real `new JSONObject((String) null)` throws a helpful-NPE whose message is non-null on this
     * host, so invokeCallback receives a non-null arg. It must complete without crashing either way.
     */
    @Test
    public void nonNullMessage_realHelpfulNpe_doesNotCrash() {
        jsInterface.postEvent("evt", null, "successCb", "errorCb");
    }

    /**
     * Mirror sibling: richMediaAction shares the identical synchronous catch -> invokeCallback(errorCb,
     * message) path (:329). A null-message exception from inside its try reaches the same guarded :346 and
     * is routed to the WebView with an empty arg instead of crashing.
     */
    @Test
    public void richMediaAction_nullMessageException_routesEmptyArgToWebView() {
        try (MockedStatic<PushwooshPlatform> platformStatic = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformStatic.when(PushwooshPlatform::getInstance).thenThrow(new NullPointerException());

            jsInterface.richMediaAction("inapp", "rm", 1, "{}", "successCb", "errorCb");
            ShadowLooper.idleMainLooper();

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(webViewMock).loadUrl(urlCaptor.capture());
            assertTrue(
                    "null error message must be routed to the WebView as an empty arg, not crash at :346",
                    urlCaptor.getValue().contains("_pwCallbackHelper.invokeCallback(\"errorCb\", \"\")"));
        }
    }
}
