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

import android.webkit.WebView;

import com.pushwoosh.inapp.view.WebActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

public class PushManagerJSInterfaceTest {
    @Mock
    private WebView webViewMock;
    @Mock
    private JsCallback jsCallbackMock;

    private PushManagerJSInterface pushManagerJSInterface;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        pushManagerJSInterface = new PushManagerJSInterface(webViewMock,jsCallbackMock);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void postEvent() {

    }

    @Test
    public void closeInApp() {
    }

    @Test
    public void registerForPushNotifications() {
    }

    @Test
    public void sendTags() {
    }

    @Test
    public void log() {
    }
}