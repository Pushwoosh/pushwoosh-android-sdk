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

package com.pushwoosh.inapp.network.model;

import androidx.annotation.NonNull;

import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_BUSINESS_CASE;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_CODE;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_GDPR;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_HASH;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_PRIORITY;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_REQUIRED;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_UPDATED;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_URL;
import static com.pushwoosh.inapp.network.model.ResourceParseUtils.convertTags;

public class Resource implements Serializable, Comparable<Resource> {

	private static final long serialVersionUID = 0L;
	private static final InAppLayout defaultLayout = InAppLayout.TOP;

	@NonNull
	public static Resource parseRichMedia(final String richMedia) throws ResourceParseException {
		return ResourceParseUtils.parseRichMedia(richMedia);
	}

	static class Column {
		static final String KEY_CODE = "code";
		static final String KEY_URL = "url";
		static final String KEY_HASH = "hash";
		static final String KEY_UPDATED = "updated";
		static final String KEY_TAGS = "tags";
		static final String KEY_TIME_STAMP = "ts";
		static final String KEY_PRIORITY = "priority";
		static final String KEY_REQUIRED = "required";
		static final String KEY_BUSINESS_CASE = "businessCase";
		static final String KEY_GDPR = "gdpr";
	}

	private final String mCode;
	private final String mUrl;
	private final String mHash;   // Hash is not stored in database!
	private final long mUpdated;
	private final InAppLayout mLayout;
	private final boolean required;
	private final int priority;
	private final String businessCase;
	private final String gdpr;

	private ModalRichmediaConfig resourceModalConfig = null;

	private Map<String, String> mTags;

	public Resource(JSONObject json) throws JSONException {
		this(json.getString(KEY_CODE),
			 json.getString(KEY_URL),
			 json.optString(KEY_HASH, ""),
			 json.getLong(KEY_UPDATED),
			 defaultLayout,
			 Collections.emptyMap(),
			 json.optBoolean(KEY_REQUIRED, false),
			 json.optInt(KEY_PRIORITY, 0),
				json.optString(KEY_BUSINESS_CASE, null),
				json.optString(KEY_GDPR));

	}

	public Resource(String url) {
		this("", url, null, 0, InAppLayout.FULLSCREEN, null, false, -1, null, null);
	}

	public Resource(String code, boolean isRequired) {
		this(code, null, null, 0, InAppLayout.FULLSCREEN, null, isRequired, -1, null, null);
	}

	public Resource(String code, String url, String hash, long updated, InAppLayout layout, Map<String, Object> tags, boolean required, int priority) {
		this(code, url, hash, updated, layout, tags, required, priority, null, null);
	}

	public Resource(String code, String url, String hash, long updated, InAppLayout layout, Map<String, Object> tags, boolean required, int priority, String businessCase, String gdpr) {
        if (businessCase != null && !businessCase.isEmpty()) {
            this.required = true;
        } else {
            this.required = required;
        }
	    mCode = code;
		mUrl = url;
		mHash = hash;
		mUpdated = updated;
		mLayout = layout;
		mTags = convertTags(tags);
		this.priority = priority;
		this.businessCase = businessCase;
		this.gdpr = gdpr;
	}

	public void setTags(Map<String, Object> tags) {
		mTags = convertTags(tags);
	}

	public String getCode() {
		return mCode;
	}

	public String getUrl() {
		return mUrl;
	}

	public String getHash() {
		return mHash;
	}

	public long getUpdated() {
		return mUpdated;
	}

	public InAppLayout getLayout() {
		return mLayout;
	}

	public boolean isRequired() {
		return required;
	}

	public int getPriority() {
		return priority;
	}

	public Map<String, String> getTags() {
		return new HashMap<>(mTags);
	}

	public boolean isNotDownload(){
		return mUrl  == null;
	}

	public String getBusinessCase() {
		return businessCase;
	}
	public String getGdpr() {
		return gdpr;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Resource resource = (Resource) o;

		if (mUpdated != resource.mUpdated) {
			return false;
		}
		if (required != resource.required) {
			return false;
		}
		if (priority != resource.priority) {
			return false;
		}
		if (mCode != null ? !mCode.equals(resource.mCode) : resource.mCode != null) {
			return false;
		}
		//noinspection SimplifiableIfStatement
		if (mUrl != null ? !mUrl.equals(resource.mUrl) : resource.mUrl != null) {
			return false;
		}
		if (businessCase != null ? !businessCase.equals(resource.businessCase) : resource.businessCase != null){
			return false;
		}
		if (gdpr != null ? !gdpr.equals(resource.gdpr) : resource.gdpr != null) {
			return false;
		}
		return mLayout == resource.mLayout;

	}

	@Override
	public int hashCode() {
		int result = mCode != null ? mCode.hashCode() : 0;
		result = 31 * result + (mUrl != null ? mUrl.hashCode() : 0);
		result = 31 * result + (gdpr != null ? gdpr.hashCode() : 0);
		result = 31 * result + (int) (mUpdated ^ (mUpdated >>> 32));
		result = 31 * result + (mLayout != null ? mLayout.hashCode() : 0);
		result = 31 * result + (required ? 1 : 0);
		result = 31 * result + priority;
		return result;
	}

	@Override
	public int compareTo(@NonNull Resource resource) {
		if (required && resource.required || !required && !resource.required) {
			int compare = resource.priority - priority;
			if (compare == 0) {
				compare = mCode == null ? 1 : mCode.compareTo(resource.getCode());
			}
			return compare;
		}

		if (required) {
			return -1;
		}

		return 1;
	}

	public boolean isInApp() {
		return mCode.length() > 0 && !mCode.startsWith("r-");
	}

	public ModalRichmediaConfig getResourceModalConfig() {
		return resourceModalConfig;
	}

	public void setResourceModalConfig(ModalRichmediaConfig config) {
		this.resourceModalConfig = config;
	}

	public boolean hasResourceModalConfig() {
		return resourceModalConfig != null;
	}
}
