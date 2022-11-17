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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.candidates;

import java.util.Set;

/**
 * Describes contract which store, fetch and remove cache candidates.
 * Cache candidates - this is a key names which is candidates for file fetching.
 */
public interface CacheCandidateProvider {
	/**
	 * Returns true if value is exists false otherwise
	 *
	 * @param key target key
	 * @return exists condition
	 */
	boolean contains(String key);

	/**
	 * Puts file to cache, value not might be null
	 *
	 * @param key target key
	 */
	void put(String key);

	/**
	 * Returns all keys inside cache
	 *
	 * @return keys array
	 */
	Set<String> keys();

	/**
	 * Removes specific value from cache by given key
	 *
	 * @param key target key for remove
	 */
	void remove(String key);
}