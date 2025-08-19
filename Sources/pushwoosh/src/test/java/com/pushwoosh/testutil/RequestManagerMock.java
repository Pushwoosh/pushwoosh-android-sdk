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

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.PushRequestHelper;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;


public class RequestManagerMock implements RequestManager {
	private final Map<Class, JSONObject> responseMap = new HashMap<>();
	private final Map<Class, NetworkException> exceptionMap = new HashMap<>();
	private final Map<Class, Expectation<JSONObject>> expectations = new HashMap<>();
	private ServerCommunicationManager serverCommunicationManager;

	@Override
	public <Response> void sendRequest(PushRequest<Response> request) {
		sendRequest(request, null);
	}

	@Override
	public <Response> void sendRequest(PushRequest<Response> request, Callback<Response, NetworkException> callback) {
		sendRequest(request, null, callback);
	}

	@Override
	public <Response> void sendRequest(final PushRequest<Response> request, final String baseUrl, final Callback<Response, NetworkException> callback) {
		PWLog.noise("RequestManagerMock", "sendRequest: " + request.getMethod());
		Result<Response, NetworkException> result = sendRequestSync(request);

		if (callback != null) {
			new Handler(Looper.getMainLooper()).post(() -> {
				callback.process(result);
			});
		}
	}

	@Override
	@NonNull
	public <Response> Result<Response, NetworkException> sendRequestSync(PushRequest<Response> request) {
		JSONObject params = null;
		try {
			params = PushRequestHelper.getParams(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (Map.Entry<Class, Expectation<JSONObject>> entry : expectations.entrySet()) {
			Class excpectedClass = entry.getKey();
			Expectation expectation = entry.getValue();

			if (excpectedClass.isAssignableFrom(request.getClass())) {
				expectation.fulfilled(params);
			}
		}

		if (responseMap.containsKey(request.getClass())) {
			try {
				return Result.fromData(request.parseResponse(responseMap.get(request.getClass())));
			} catch (JSONException e) {
				return Result.fromException(new NetworkException(e.getMessage()));
			}
		}

		if (exceptionMap.containsKey(request.getClass())) {
			return Result.fromException(exceptionMap.get(request.getClass()));
		}

		return Result.fromData(null);
	}

	@Override
	public void updateBaseUrl(final String baseUrl) {

	}

	@Override
	public void setReverseProxyUrl(String url) {

	}

	@Override
	public void disableReverseProxy() {

	}

	public void setResponse(JSONObject response, Class<? extends PushRequest> requestClass) {
		responseMap.put(requestClass, response);
	}

	public void setException(NetworkException exception, Class<? extends PushRequest> requestClass) {
		exceptionMap.put(requestClass, exception);
	}

	public void clear(){
		exceptionMap.clear();
		responseMap.clear();
	}

	public Expectation<JSONObject> expect(Class<? extends PushRequest> requestClass) {
		Expectation expectation = Mockito.mock(Expectation.class);
		expectations.put(requestClass, expectation);
		return expectation;
	}
}
