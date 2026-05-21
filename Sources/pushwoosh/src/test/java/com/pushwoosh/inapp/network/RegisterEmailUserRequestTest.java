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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class RegisterEmailUserRequestTest {

    @Test
    public void getMethod_always_returnsRegisterEmailUser() {
        RegisterEmailUserRequest request = new RegisterEmailUserRequest("user-1", "user@example.com");

        assertEquals("registerEmailUser", request.getMethod());
    }

    @Test
    public void buildParams_withUserIdAndEmail_writesEmailUserIdAndNumericTzOffset() throws JSONException {
        RegisterEmailUserRequest request = new RegisterEmailUserRequest("user-1", "user@example.com");
        JSONObject params = new JSONObject();

        request.buildParams(params);

        assertEquals("user@example.com", params.getString("email"));
        assertEquals("user-1", params.getString("userId"));
        assertTrue(params.has("tz_offset"));
        assertTrue(params.get("tz_offset") instanceof Number);

        long expectedTzOffset =
                TimeUnit.SECONDS.convert(TimeZone.getDefault().getOffset(new Date().getTime()), TimeUnit.MILLISECONDS);
        assertEquals(expectedTzOffset, ((Number) params.get("tz_offset")).longValue());
    }
}
