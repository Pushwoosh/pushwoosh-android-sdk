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

package com.pushwoosh.repository;

import androidx.annotation.Nullable;

import com.pushwoosh.internal.network.PushRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@code /messageDeliveryEvent} request. When the push receipt moment is known, it is sent as
 * {@code receivedAt}: an RFC 3339 UTC string with seconds and no fractional part, e.g.
 * {@code 2026-11-23T16:56:00Z}. Without a known receipt moment the field is omitted.
 */
public class MessageDeliveredRequest extends PushRequest<Void> {
    private static final String RECEIVED_AT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private final String hash;
    private final String metaData;

    @Nullable private final Long receivedAtUtcSeconds;

    public MessageDeliveredRequest(String hash, String metaData) {
        this(hash, metaData, null);
    }

    /**
     * @param receivedAtUtcSeconds push receipt moment in unix seconds; {@code null} or non-positive
     *     means unknown and omits {@code receivedAt}
     */
    public MessageDeliveredRequest(String hash, String metaData, @Nullable Long receivedAtUtcSeconds) {
        this.hash = hash;
        this.metaData = metaData;
        this.receivedAtUtcSeconds = receivedAtUtcSeconds;
    }

    public String getMethod() {
        return "messageDeliveryEvent";
    }

    public String getHash() {
        return hash;
    }

    public String getMetaData() {
        return metaData;
    }

    @Nullable public Long getReceivedAtUtcSeconds() {
        return receivedAtUtcSeconds;
    }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        if (hash != null) {
            params.put("hash", hash);
        }
        if (metaData != null) {
            params.put("metaData", metaData);
        }
        if (receivedAtUtcSeconds != null && receivedAtUtcSeconds > 0) {
            params.put("receivedAt", formatReceivedAt(receivedAtUtcSeconds));
        }
    }

    static String formatReceivedAt(long utcSeconds) {
        // Locale.US pins ASCII digits and the Gregorian calendar regardless of the device locale.
        SimpleDateFormat format = new SimpleDateFormat(RECEIVED_AT_PATTERN, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(utcSeconds * 1000L));
    }
}
