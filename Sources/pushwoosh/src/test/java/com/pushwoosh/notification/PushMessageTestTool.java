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

package com.pushwoosh.notification;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.mockito.Mockito;

/**
 * Created by aevstefeev on 13/03/2018.
 */

public class PushMessageTestTool {

    @NonNull
    public PushMessage getPushMessageMock(boolean isSilent) {
        PushMessage pushMessage = Mockito.mock(PushMessage.class);
        setData(isSilent, pushMessage);
        return pushMessage;
    }


    @NonNull
    public PushMessage getPushMessagePowerMock(boolean isSilent) {
        PushMessage pushMessage = Mockito.mock(PushMessage.class);
        setData(isSilent, pushMessage);
        return pushMessage;
    }

    private void setData(boolean isSilent, PushMessage pushMessage) {
        Mockito.when(pushMessage.getLedOnMS()).thenReturn(2);
        Mockito.when(pushMessage.isSilent()).thenReturn(isSilent);
        Mockito.when(pushMessage.getLedOffMS()).thenReturn(2);
        Mockito.when(pushMessage.getLed()).thenReturn(1);
        Mockito.when(pushMessage.getBigPictureUrl()).thenReturn("url_picture");
        Mockito.when(pushMessage.getMessage()).thenReturn("message text");
        Mockito.when(pushMessage.getTag()).thenReturn("tag");
        Mockito.when(pushMessage.getVibration()).thenReturn(true);
        Mockito.when(pushMessage.getSound()).thenReturn("sound.mp3");
        Mockito.when(pushMessage.getLargeIconUrl()).thenReturn("lagre_icon");
        Mockito.when(pushMessage.getTicker()).thenReturn("ticker_text");
        Mockito.when(pushMessage.getVisibility()).thenReturn(3);
        Mockito.when(pushMessage.toJson()).thenReturn(new JSONObject());
    }
}
