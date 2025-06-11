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

package com.pushwoosh.notification.handlers.notification;

import android.os.Bundle;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.repository.PushwooshRepository;

public class PushStatNotificationOpenHandler implements PushNotificationOpenHandler {

    @Override
    public void postHandleNotification(Bundle pushBundle) {
        if (PushBundleDataProvider.isLocal(pushBundle) || PushBundleDataProvider.isSilent(pushBundle)) {
            return;
        }

        String pushHash = PushBundleDataProvider.getPushHash(pushBundle);
        String metadata = PushBundleDataProvider.getPushMetadata(pushBundle);

        PWLog.debug("PushStatNotificationOpenHandler", String.format("postHandleNotification, pushHash: %s, metadata: %s", pushHash, metadata));

        PushwooshRepository pushwooshRepository = PushwooshPlatform.getInstance().pushwooshRepository();
        pushwooshRepository.setCurrentSessionHash(pushHash);
        pushwooshRepository.sendPushOpened(pushHash, metadata);
    }
}
