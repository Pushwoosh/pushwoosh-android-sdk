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

package com.pushwoosh.internal.network;

public class ConnectionException extends NetworkException {
    private final int statusCode;
    private final int pushwooshStatusCode;

    public ConnectionException(String description, int statusCode, int pushwooshStatusCode) {
        super(description);
        this.statusCode = statusCode;
        this.pushwooshStatusCode = pushwooshStatusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getPushwooshStatusCode() {
        return pushwooshStatusCode;
    }

    /**
     * Answers whether this failure looks temporary — a dropped connection or a server-side hiccup that
     * an identical later request may survive. The verdict is read off the HTTP status alone;
     * {@code pushwooshStatusCode} only takes part in the "no status at all" sentinel, so an HTTP 200
     * carrying an envelope-level error counts as terminal. Whether to actually retry, and how often,
     * stays the caller's policy.
     */
    public boolean isTransient() {
        // statuses are 0 by default and changed after processing request. If they are both still 0
        // then request failed due to connection errors
        boolean noStatusReceived = pushwooshStatusCode == 0 && statusCode == 0;

        return noStatusReceived || isRetriableStatus(statusCode);
    }

    private static boolean isRetriableStatus(int code) {
        switch (code) {
            case 408: // Request Timeout
            case 429: // Too Many Requests
            case 500: // Internal Server Error
            case 502: // Bad Gateway
            case 503: // Service Unavailable
            case 504: // Gateway Timeout
                return true;
            default:
                return false;
        }
    }
}
