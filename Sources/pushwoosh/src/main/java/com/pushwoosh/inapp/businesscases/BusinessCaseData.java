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

package com.pushwoosh.inapp.businesscases;

import com.pushwoosh.inapp.network.model.Resource;

import org.json.JSONObject;

/**
 * Created by kai on 06.02.2018.
 */


public class BusinessCaseData {
	private String inAppCode;
	private long updated;

	public String getInAppCode() {
		return inAppCode;
	}

	public long getUpdated() {
		return updated;
	}

	public static BusinessCaseData fromResource(Resource resource) {
		BusinessCaseData data = new BusinessCaseData();
		data.inAppCode = resource.getCode();
		data.updated = resource.getUpdated();
		return data;
	}

	public static BusinessCaseData fromJSON(JSONObject caseData) {
		BusinessCaseData eventData = new BusinessCaseData();
		eventData.inAppCode = caseData.optString("code");
		eventData.updated = caseData.optLong("updated");
		return eventData;
	}

	public static BusinessCaseData create(String inAppCode, long updated) {
		BusinessCaseData eventData = new BusinessCaseData();
		eventData.inAppCode = inAppCode;
		eventData.updated = updated;
		return eventData;
	}
}