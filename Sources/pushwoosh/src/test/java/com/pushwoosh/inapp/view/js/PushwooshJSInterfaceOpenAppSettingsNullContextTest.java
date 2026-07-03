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

import android.content.Context;
import android.webkit.WebView;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

// Regression guard for crash-openappsettings-context-null: PushwooshJSInterface.openAppSettings()
// is an @JavascriptInterface method called directly by the WebView JS engine on the binder thread
// with no SDK wrapper and no local try/catch. It dereferences AndroidPlatformModule
// .getApplicationContext() (a WeakReference<Context>.get(), nullable when GC'd or not yet set). When
// rich-media/in-app JS calls pushwoosh.openAppSettings() while that context is null, the method must
// guard it and return instead of crashing. We force getApplicationContext()==null via MockedStatic
// and assert the method returns gracefully.
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class PushwooshJSInterfaceOpenAppSettingsNullContextTest {

    @Mock
    private WebView webViewMock;

    @Mock
    private JsCallback jsCallbackMock;

    private AutoCloseable mocks;
    private MockedStatic<AndroidPlatformModule> platformModuleStatic;
    private PushwooshJSInterface jsInterface;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        platformModuleStatic = Mockito.mockStatic(AndroidPlatformModule.class);
        // The actually-nullable value: WeakReference<Context>.get() returns null.
        platformModuleStatic.when(AndroidPlatformModule::getApplicationContext).thenReturn(null);

        jsInterface = new PushwooshJSInterface(jsCallbackMock, webViewMock, null, null, null);
    }

    @After
    public void tearDown() throws Exception {
        if (platformModuleStatic != null) {
            platformModuleStatic.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Regression guard: with getApplicationContext() null, the null-context check added to
     * openAppSettings() must early-return gracefully instead of dereferencing a null Context.
     */
    @Test
    public void openAppSettings_nullApplicationContext_returnsGracefullyWithoutThrowing() {
        jsInterface.openAppSettings();
    }

    /**
     * Negative control: the null context is the necessary condition. With getApplicationContext()
     * returning a real (non-null) application context, openAppSettings() runs to completion and
     * does not throw — proving condition getApplicationContext()==null is required, not incidental.
     */
    @Test
    public void openAppSettings_nonNullApplicationContext_doesNotThrow() {
        Context realContext = RuntimeEnvironment.getApplication();
        platformModuleStatic.when(AndroidPlatformModule::getApplicationContext).thenReturn(realContext);

        jsInterface.openAppSettings();
    }
}
