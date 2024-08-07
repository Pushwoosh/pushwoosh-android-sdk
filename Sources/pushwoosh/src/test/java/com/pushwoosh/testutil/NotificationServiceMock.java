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

package com.pushwoosh.testutil;

import android.os.Bundle;

import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;

import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


public class NotificationServiceMock extends NotificationServiceExtension {

    public interface Callback {
        boolean onMessageReceived(PushMessage data);

        void startActivityForPushMessage(PushMessage message);

        void handleMessage(Bundle pushBundle);
    }

    private static NotificationServiceMock INSTANCE;
    private static Callback MOCK;

    public static Callback callbackMock() {
        return MOCK;
    }


    public NotificationServiceMock() {
        INSTANCE = this;
        MOCK = Mockito.mock(Callback.class);

        when(MOCK.onMessageReceived(any())).thenReturn(false);
    }


    @Override
    protected boolean onMessageReceived(PushMessage data) {
        super.onMessageReceived(data);
        return MOCK.onMessageReceived(data);
    }


    @Override
    protected void startActivityForPushMessage(PushMessage message) {
        super.startActivityForPushMessage(message);
        MOCK.startActivityForPushMessage(message);
    }

}
