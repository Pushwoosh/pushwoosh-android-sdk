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

import androidx.annotation.NonNull;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.utils.Accumulator;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONObject;

import java.util.List;

public class SendTagsProcessor implements Accumulator.Completion<SendTagsProcessor.SendTagsInvocation> {

	public static class SendTagsInvocation {
		private final JSONObject tags;
		private final Callback<Void, PushwooshException> handler;

		SendTagsInvocation(JSONObject tags, Callback<Void, PushwooshException> handler) {
			this.tags = tags;
			this.handler = handler;
		}

		@NonNull
		public JSONObject getTags() {
			return tags;
		}

		public Callback<Void, PushwooshException> getHandler() {
			return handler;
		}
	}


	private static final int TAG_ACCUMULATION_DELAY = 1000;

	private Accumulator<SendTagsInvocation> accumulator;

	public SendTagsProcessor() {
		accumulator = new Accumulator<>(this, TAG_ACCUMULATION_DELAY);
	}

	public void sendTags(@NonNull JSONObject tags, Callback<Void, PushwooshException> listener) {
		accumulator.accumulate(new SendTagsInvocation(tags, listener));
	}

	@Override
	public void onAccumulated(List<SendTagsProcessor.SendTagsInvocation> data) {
		JSONObject tags = new JSONObject();

		for (SendTagsInvocation sendTagsInvocation : data) {
			JsonUtils.mergeJson(sendTagsInvocation.getTags(), tags);
		}

		RequestManager requestManager = NetworkModule.getRequestManager();
		if(requestManager == null){
			NetworkException exception = new NetworkException("Request manager is null");
			failedAccumulatedData(data, exception);
			return;
		}
		requestManager.sendRequest(new SetTagsRequest(tags), result -> {
			if (result.isSuccess()) {
				for (SendTagsInvocation sendTagsInvocation : data) {
					if (sendTagsInvocation.getHandler() != null) {
						sendTagsInvocation.getHandler().process(Result.fromData(null));
					}
				}
				PWLog.info("Tags successfully sent to Pushwoosh");
			} else {
				failedAccumulatedData(data, result.getException());
			}
		});
	}

	private void failedAccumulatedData(List<SendTagsInvocation> data, NetworkException exception) {
		RepositoryModule.getRegistrationPreferences().setTagsFailed().set(true);

		for (SendTagsInvocation sendTagsInvocation : data) {
			if (sendTagsInvocation.getHandler() != null) {
				sendTagsInvocation.getHandler().process(Result.fromException(exception));
			}
		}
	}
}
