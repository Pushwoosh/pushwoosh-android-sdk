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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.provider;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concurrent cache provider which locks on concrete key.
 */
public final class ConcurrentCacheProvider implements CacheProvider {

	private final Map<String, Object> currentCache;

	public ConcurrentCacheProvider(String prefName, Map<String, Map<String, Object>> allCaches) {
		this.currentCache = putIfAbsentCache(prefName, allCaches);
	}

	private Map<String, Object> putIfAbsentCache(String prefName, Map<String, Map<String, Object>> allCaches) {
		if (allCaches.containsKey(prefName)) {
			return allCaches.get(prefName);
		}
		Map<String, Object> map = new ConcurrentHashMap<>();
		allCaches.put(prefName, map);
		return map;
	}

	@Override
	public boolean contains(String key) {
		return currentCache.containsKey(key);
	}

	@Override
	public void put(String key, Object value) {
		currentCache.put(key, value);
	}

	@Override
	public Set<String> keys() {
		Set<String> s = currentCache.keySet();
		return Collections.unmodifiableSet(s);
	}

	@Override
	public Object get(String key) {
		return currentCache.get(key);
	}

	@Override
	public void remove(String key) {
		currentCache.remove(key);
	}

	@Override
	public Map<String, Object> getAll() {
		return currentCache;
	}
}