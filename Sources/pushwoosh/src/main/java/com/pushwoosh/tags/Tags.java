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

import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Static utility class for tags creation
 */
public final class Tags {

	private static class Holder {
		private static final TagsBundle EMPTY_BUNDLE = new TagsBundle.Builder().build();
	}

	/**
	 * @param key   tag name
	 * @param value tag value
	 * @return TagsBundle with single tag of int type
	 */
	public static TagsBundle intTag(String key, int value) {
		return new TagsBundle.Builder()
				.putInt(key, value)
				.build();
	}

	/**
	 * @param key   tag name
	 * @param value tag value
	 * @return TagsBundle with single tag of long type
	 */
	public static TagsBundle longTag(String key, long value) {
		return new TagsBundle.Builder()
				.putLong(key, value)
				.build();
	}

	/**
	 * @param key   tag name
	 * @param value tag value
	 * @return TagsBundle with single tag of boolean type
	 */
	public static TagsBundle booleanTag(String key, boolean value) {
		return new TagsBundle.Builder()
				.putBoolean(key, value)
				.build();
	}

	/**
	 * @param key   tag name
	 * @param value tag value
	 * @return TagsBundle with single tag of string type
	 */
	public static TagsBundle stringTag(String key, String value) {
		return new TagsBundle.Builder()
				.putString(key, value)
				.build();
	}

	/**
	 * @param key   tag name
	 * @param value tag value
	 * @return TagsBundle with single tag of list type
	 */
	public static TagsBundle listTag(String key, List<String> value) {
		return new TagsBundle.Builder()
				.putList(key, value)
				.build();
	}

	/**
	 * @param key   tag name
	 * @param value tag value
	 * @return TagsBundle with single tag of Date type
	 */
	public static TagsBundle dateTag(String key, Date value) {
		return new TagsBundle.Builder()
				.putDate(key, value)
				.build();
	}

	/**
	 * @param key tag name
	 * @return TagsBundle for tag removal
	 */
	public static TagsBundle removeTag(String key) {
		return new TagsBundle.Builder()
				.remove(key)
				.build();
	}

	/**
	 * @param json json object with tag name-value pairs
	 * @return converted tags
	 */
	public static TagsBundle fromJson(JSONObject json) {
		return new TagsBundle.Builder()
				.putAll(json)
				.build();
	}

	/**
	 * @param key   tag name
	 * @param delta incremental value
	 * @return TagsBundle for tag increment operation
	 */
	public static TagsBundle incrementInt(String key, int delta) {
		return new TagsBundle.Builder()
				.incrementInt(key, delta)
				.build();
	}

	/**
	 * @param key   tag name
	 * @param list append value
	 * @return TagsBundle for list tag append operation
	 */
	public static TagsBundle appendList(String key, List<String> list) {
		return new TagsBundle.Builder()
				.appendList(key, list)
				.build();
	}


	/**
	 * @return empty TagsBundle
	 */
	public static TagsBundle empty() {
		return Holder.EMPTY_BUNDLE;
	}

	private Tags() { /* do nothing */ }
}
