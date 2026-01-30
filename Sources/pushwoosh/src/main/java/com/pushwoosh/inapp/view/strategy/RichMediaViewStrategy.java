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

package com.pushwoosh.inapp.view.strategy;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.ModalRichMediaWindow;
import com.pushwoosh.inapp.view.RichMediaWebActivity;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.richmedia.RichMediaType;

/**
 * Display strategy for Rich Media content.
 * Uses MODAL (PopupWindow) or DEFAULT (Activity) based on RichMediaManager setting.
 */
class RichMediaViewStrategy implements ResourceViewStrategy {
    private static final String TAG = "RichMediaViewStrategy";

    private final Context context;
    private final long delay;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    RichMediaViewStrategy(Context context, long delay) {
        this.context = context;
        this.delay = delay;
    }

    @Override
    public void show(Resource resource) {
        PWLog.noise(TAG, "show()");

        if (resource == null) {
            PWLog.warn(TAG, "resource is empty");
            return;
        }

        String richMediaCode = resource.getCode().substring(2);
        PushwooshPlatform.getInstance().pushwooshRepository().setCurrentRichMediaCode(richMediaCode);
        PushwooshPlatform.getInstance().pushwooshRepository().setCurrentInAppCode(null);

        String message = String.format("presenting richMedia with code: %s, url: %s", resource.getCode(), resource.getUrl());
        PWLog.info(TAG, message);

        if (RichMediaManager.getRichMediaType() == RichMediaType.MODAL) {
            ModalRichMediaWindow.showModalRichMediaWindow(resource);
        } else if (RichMediaManager.getRichMediaType() == RichMediaType.DEFAULT) {
            Intent intent = RichMediaWebActivity.createRichMediaIntent(context, resource);
            mainHandler.postDelayed(() -> context.startActivity(intent), delay);
        }
    }
}
