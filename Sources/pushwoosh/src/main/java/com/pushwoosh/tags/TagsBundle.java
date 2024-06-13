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

package com.pushwoosh.tags;


import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable collection of tags specific for current device. Tags are used to target different audience selectively when sending push notification.
 *
 * @see <a href="http://docs.pushwoosh.com/docs/segmentation-tags-and-filters">Segmentation guide</a>
 */
public class TagsBundle {
	/**
	 * TagsBundle.Builder class is used to generate TagsBundle instances
	 */
	public static class Builder {
		private final Map<String, Object> tags = new ConcurrentHashMap<>();

		/**
		 * Adds tag with integer value
		 *
		 * @param key   tag name
		 * @param value tag value
		 * @return builder
		 */
		public Builder putInt(String key, int value) {
			tags.put(key, value);
			return this;
		}

		/**
		 * Adds tag with long value
		 *
		 * @param key   tag name
		 * @param value tag value
		 * @return builder
		 */
		public Builder putLong(String key, long value) {
			tags.put(key, value);
			return this;
		}

		/**
		 * Adds increment operation for given tag
		 *
		 * @param key   tag name
		 * @param value incremental value
		 * @return builder
		 */
		public Builder incrementInt(String key, int value) {
			Map<String, Object> inc = new HashMap<>();
			inc.put("operation", "increment");
			inc.put("value", value);
			tags.put(key, inc);
			return this;
		}

		/**
		 * Adds append operation for given list tag
		 *
		 * @param key   tag name
		 * @param value list to append
		 * @return builder
		 */
		public Builder appendList(String key, List<String> value) {
			Map<String, Object> append = new HashMap<>();
			append.put("operation", "append");
			append.put("value", value);
			tags.put(key, append);
			return this;
		}

		/**
		 * Adds remove operation for given list tag
		 *
		 * @param key   tag name
		 * @param value list to remove
		 * @return builder
		 */
		public Builder removeFromList(String key, List<String> value) {
			Map<String, Object> remove  = new HashMap<>();
			remove.put("operation", "remove");
			remove.put("value", value);
			tags.put(key, remove);
			return this;
		}

		/**
		 * Adds tag with boolean value
		 *
		 * @param key   tag name
		 * @param value tag value
		 * @return builder
		 */
		public Builder putBoolean(String key, boolean value) {
			tags.put(key, value);
			return this;
		}

		/**
		 * Adds tag with string value
		 *
		 * @param key   tag name
		 * @param value tag value
		 * @return builder
		 */
		public Builder putString(String key, String value) {
			tags.put(key, value);
			return this;
		}

		public Builder putStringIfNotEmpty(String key, String value) {
			if (!TextUtils.isEmpty(value)) {
				tags.put(key, value);
			}
			return this;
		}

		/**
		 * Adds tag with list value
		 *
		 * @param key   tag name
		 * @param value tag value
		 * @return builder
		 */
		public Builder putList(String key, List<String> value) {
			tags.put(key, value);
			return this;
		}

		/**
		 * Adds tag with date value
		 *
		 * @param key   tag name
		 * @param value tag value
		 * @return builder
		 */
		public Builder putDate(String key, Date value) {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			tags.put(key, dateFormat.format(value));
			return this;
		}

		/**
		 * Removes tag
		 *
		 * @param key tag name
		 * @return builder
		 */
		public Builder remove(String key) {
			tags.put(key, null);
			return this;
		}

		/**
		 * Adds all tags from key-value pairs of given json
		 *
		 * @param json json object with tag name-value pairs
		 * @return
		 */
		public Builder putAll(JSONObject json) {
			Iterator<String> keys = json.keys();
			//use synchronized keyword in attempt to fix rare ConcurrentModificationException in
			// java.util.LinkedHashMap$LinkedKeyIterator.next on devices with Samsung chips (https://kanban.corp.pushwoosh.com/issue/SDK-306/)
			synchronized (keys) {
				while (keys.hasNext()) {
					String key = keys.next();
					this.tags.put(key, json.opt(key));
				}
			}

			return this;
		}

		/**
		 * Builds and returns TagsBundle.
		 *
		 * @return TagsBundle
		 */
		public TagsBundle build() {
			return new TagsBundle(this);
		}
	}

	private final Map<String, Object> tags;

	private TagsBundle(Builder builder) {
		tags = builder.tags;
	}

	/**
	 * @param key          tag name
	 * @param defaultValue default tag value
	 * @return tag value for given name or defaultValue if tag with given name does not exist
	 */
	public int getInt(String key, int defaultValue) {
		Object value = tags.get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}

		return defaultValue;
	}

	/**
	 * @param key          tag name
	 * @param defaultValue default tag value
	 * @return tag value for given name or defaultValue if tag with given name does not exist
	 */
	public long getLong(String key, long defaultValue) {
		Object value = tags.get(key);
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}

		return defaultValue;
	}

	/**
	 * @param key          tag name
	 * @param defaultValue default tag value
	 * @return tag value for given name or defaultValue if tag with given name does not exist
	 */
	public boolean getBoolean(String key, boolean defaultValue) {
		Object value = tags.get(key);
		if (value instanceof Boolean) {
			return (Boolean) value;
		}

		return defaultValue;
	}

	/**
	 * @param key tag name
	 * @return tag value for given name or null if tag with given name does not exist
	 */
	@Nullable
	public String getString(String key) {
		Object value = tags.get(key);
		if (value instanceof String) {
			return (String) value;
		}

		return null;
	}

	/**
	 * @param key tag name
	 * @return tag value for given name or null if tag with given name does not exist
	 */
	@Nullable
	public List<String> getList(String key) {
		Object value = tags.get(key);
		if (value instanceof List) {
			return (List<String>) value;
		}

		List<String> result = new ArrayList<>();
		if (value instanceof JSONArray) {
			for (int i = 0; i < ((JSONArray) value).length(); i++) {
				try {
					result.add(((JSONArray) value).getString(i));
				} catch (JSONException ignore) {
					//getList return only Strings so that we can ignore all other elements
				}
			}

			return result;
		}

		return null;
	}

	/**
	 * @return JSON representation of TagsBundle
	 */
	@NonNull
	public JSONObject toJson() {
		return JsonUtils.mapToJson(tags);
	}

	public Map<String, Object> getMap(){
		return tags;
	}
}
