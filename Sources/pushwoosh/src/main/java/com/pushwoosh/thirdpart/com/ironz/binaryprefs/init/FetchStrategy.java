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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.init;

import java.util.Map;

/**
 * Interface which describes contract for object fetching
 */
public interface FetchStrategy {
	/**
	 * Returns value for target key.
	 *
	 * @param key      given key for fetching
	 * @param defValue default value if target value does not exists
	 * @return actual value if exists, default value otherwise
	 */
	Object getValue(String key, Object defValue);

	/**
	 * Returns all value from current cache.
	 *
	 * @return all value from current preferences
	 */
	Map<String, Object> getAll();

	/**
	 * Checks whether the preferences contains a preference.
	 *
	 * @param key The name of the preference to check.
	 * @return Returns true if the preference exists in the preferences,
	 * false otherwise.
	 */
	boolean contains(String key);
}