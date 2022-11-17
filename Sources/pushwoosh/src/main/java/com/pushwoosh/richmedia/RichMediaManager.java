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

package com.pushwoosh.richmedia;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.utils.PWLog;

/**
 * `RichMediaManager` class is a manager responsible for Rich Media presentation.
 */
public class RichMediaManager {
    public static void setDelegate(RichMediaPresentingDelegate delegate) {
        // workaround for PUSH-31718
        // on some Lenovo devices PushwooshPlatform instance is not created when delegate is being set
        PushwooshPlatform pushwooshPlatform = PushwooshPlatform.getInstance();
        if (pushwooshPlatform == null) {
            PWLog.error("RichMediaManager failed to set delegate as PushwooshPlatform is not initialized yet");
            return;
        }
        RichMediaController richMediaController = pushwooshPlatform.getRichMediaController();
        if (richMediaController != null)
            richMediaController.setDelegate(delegate);
    }

    public static void present(RichMedia richMedia) {
        RichMediaController richMediaController = PushwooshPlatform.getInstance().getRichMediaController();
        if (richMediaController != null)
            richMediaController.present(richMedia);
    }

    public static RichMediaStyle getRichMediaStyle() {
        RichMediaController richMediaController = PushwooshPlatform.getInstance().getRichMediaController();
        if (richMediaController != null) {
            return richMediaController.getRichMediaStyle();
        } else {
            return null;
        }
    }
}
