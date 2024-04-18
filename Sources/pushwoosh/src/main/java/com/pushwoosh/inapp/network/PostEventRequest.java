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

package com.pushwoosh.inapp.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

class PostEventRequest extends PushRequest<PostEventResponse> {
	private String currentSessionHash;
	private TagsBundle attributes;
	private String event;
	private String richMediaCode;
	private String inAppCode;

	PostEventRequest(String event, String currentSessionHash, @Nullable TagsBundle attributes) {
		this.attributes = attributes != null ? attributes : Tags.empty();
		this.currentSessionHash = currentSessionHash;
		this.event = event;
		this.richMediaCode = PushwooshPlatform.getInstance().pushwooshRepository().getCurrentRichMediaCode();
		this.inAppCode = PushwooshPlatform.getInstance().pushwooshRepository().getCurrentInAppCode();
	}

	@Override
	public String getMethod() {
		return "postEvent";
	}

	@Override
	public boolean shouldUseJitter(){ return false; }

	@Override
	protected void buildParams(JSONObject params) throws JSONException {
		attributes = new TagsBundle.Builder()
				.putAll(attributes.toJson())
				.putStringIfNotEmpty("msgHash", currentSessionHash)
				.putStringIfNotEmpty("richMediaCode", richMediaCode)
				.putStringIfNotEmpty("inAppCode", inAppCode)
				.build();


		params.put("attributes", attributes.toJson());
		params.put("event", event);

		int timezone = Calendar.getInstance().getTimeZone().getOffset(new Date().getTime()) / 1000;
		long unixTime = System.currentTimeMillis() / 1000L;
		long localTime = unixTime + timezone;

		params.put("timestampUTC", unixTime);
		params.put("timestampCurrent", localTime);
	}

	@Override
	public PostEventResponse parseResponse(@NonNull JSONObject response) throws JSONException {
		return new PostEventResponse(response);
	}
}
