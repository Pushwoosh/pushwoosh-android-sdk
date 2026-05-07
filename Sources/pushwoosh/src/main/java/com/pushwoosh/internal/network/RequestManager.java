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

import androidx.annotation.NonNull;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;

import java.util.Map;

/**
 * Interface which associated with communication with pushwoosh service
 */
public interface RequestManager {
    /**
     * @see #sendRequest(PushRequest, String, Callback)
     */
    <Response> void sendRequest(PushRequest<Response> request);

    /**
     * @see #sendRequest(PushRequest, String, Callback)
     */
    <Response> void sendRequest(PushRequest<Response> request, Callback<Response, NetworkException> callback);

    /**
     * Send request async with {@param request} to {@param baseUrl}. Result will be sending to {@param callback}
     * @param request - request which should be sending
     * @param baseUrl - url of service
     * @param callback - result callback
     * @param <Response> - response class associated with request
     */
    <Response> void sendRequest(
            PushRequest<Response> request, String baseUrl, Callback<Response, NetworkException> callback);

    /**
     * Send request sync with {@param request}
     * @param request - request which should be sending
     * @param <Response> - response class associated with request
     * @return - {@link com.pushwoosh.function.Result} of this request
     */
    @NonNull <Response> Result<Response, NetworkException> sendRequestSync(PushRequest<Response> request);

    /**
     * change default base url
     * @param baseUrl - new base url
     */
    boolean updateBaseUrl(String baseUrl);

    /**
     * Set or disable reverse proxy URL with optional custom HTTP headers.
     * Pass null as url to disable reverse proxy and restore default server URL.
     * @param url - reverse proxy url, or null to disable
     * @param headers - optional map of custom HTTP headers (may be null)
     */
    void setReverseProxyUrl(String url, Map<String, String> headers);
}
