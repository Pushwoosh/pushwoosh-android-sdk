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

package com.pushwoosh.function;

import androidx.annotation.NonNull;

import com.pushwoosh.PushwooshWorkManagerHelper;
import com.pushwoosh.SendCachedRequestWorker;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.RequestStorage;
import com.pushwoosh.repository.RepositoryModule;

import java.util.concurrent.TimeUnit;

import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

/**
 * Wrapper callback which is added request to store if it was failed with ConnectionException
 */
public class CacheFailedRequestCallback<Response> implements Callback<Response, NetworkException> {

	private Callback<Response, NetworkException> callback;
	private PushRequest<Response> request;
	private RequestStorage requestStorage;

	public CacheFailedRequestCallback(PushRequest<Response> request, RequestStorage requestStorage) {
		this(null, request, requestStorage);
	}

	public CacheFailedRequestCallback(Callback<Response, NetworkException> callback, PushRequest<Response> request, RequestStorage requestStorage) {
		this.callback = callback;
		this.request = request;
		this.requestStorage = requestStorage;
	}

	@Override
	public void process(@NonNull Result<Response, NetworkException> result) {
		if (!result.isSuccess() && result.getException() instanceof ConnectionException) {
			if (needToRetry((ConnectionException) result.getException())) {
				scheduleSendCachedRequestWorker(request);
			}
		}

		if (callback != null) {
			callback.process(result);
		}
	}

	public static void scheduleSendCachedRequestWorker(PushRequest request) {
		long rowId = RepositoryModule.getRequestStorage().add(request);
		if (rowId >= 0) {
			Data inputData = new Data.Builder()
					.putLong(SendCachedRequestWorker.DATA_CACHED_REQUEST_ID, rowId)
					.build();
			OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SendCachedRequestWorker.class)
					.setInputData(inputData)
					.setConstraints(PushwooshWorkManagerHelper.getNetworkAvailableConstraints())
					.setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
					.build();
			PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(workRequest,
					SendCachedRequestWorker.TAG,
					ExistingWorkPolicy.APPEND);
		}
	}

	boolean needToRetry(ConnectionException exception) {
		int pushwooshStatus = exception.getPushwooshStatusCode();
		int networkStatus = exception.getStatusCode();
		// statuses are 0 by default and changed after processing request. If they are both still 0
		// then request failed due to connection errors
		return pushwooshStatus == 0 && networkStatus == 0;
	}
}
