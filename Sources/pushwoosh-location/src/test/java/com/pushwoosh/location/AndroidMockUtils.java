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

package com.pushwoosh.location;

import android.os.Handler;
import android.os.Looper;

import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class AndroidMockUtils {

	public static void mockMainThreadHandler() throws Exception {
		mockStatic(Looper.class);

		Looper mockMainThreadLooper = mock(Looper.class);
		when(Looper.getMainLooper()).thenReturn(mockMainThreadLooper);
		whenNew(Handler.class).withArguments(mockMainThreadLooper).thenReturn(mockHandler());
	}

	public static Handler mockHandler() {
		Handler mockMainThreadHandler = mock(Handler.class);
		Answer<Boolean> handlerPostAnswer = invocation -> {
			Runnable runnable = invocation.getArgumentAt(0, Runnable.class);

			if (runnable != null) {
				runnable.run();
			}
			return true;
		};
		doAnswer(handlerPostAnswer).when(mockMainThreadHandler).post(any(Runnable.class));
		doAnswer(handlerPostAnswer).when(mockMainThreadHandler).postDelayed(any(Runnable.class), anyLong());
		return mockMainThreadHandler;
	}
}
