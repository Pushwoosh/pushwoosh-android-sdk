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

package com.pushwoosh.inapp.network;

import com.pushwoosh.internal.network.PushRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by kai on 27.02.2018.
 */

public class TriggerInAppActionRequest extends PushRequest<Void> {
    private String inAppCode;
    private String messageHash;
    private String richMediaCode;

    public TriggerInAppActionRequest(String inAppCode, String messageHash, String richMediaCode) {
        this.inAppCode = inAppCode;
        this.messageHash = messageHash;
        this.richMediaCode = richMediaCode;
    }

    public String getMethod() {
        return "triggerInAppAction";
    }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        params.put("action", "show");
        params.put("code", inAppCode.startsWith("r-") ? "" : inAppCode);
        if (messageHash != null) {
            params.put("messageHash", messageHash);
        }
        if (richMediaCode.startsWith("r-")) {
            params.put("richMediaCode", richMediaCode.substring(2));
        }

        int timezone = Calendar.getInstance().getTimeZone().getRawOffset() / 1000;
        long unixTime = System.currentTimeMillis() / 1000L;
        long localTime = unixTime + timezone;

        params.put("timestampUTC", unixTime);
        params.put("timestampCurrent", localTime);
    }
}
