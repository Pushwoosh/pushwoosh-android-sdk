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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs;

import android.content.SharedPreferences;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

final class ParametersProvider {

	private static final Map<String, ReadWriteLock> locks = new ConcurrentHashMap<>();
	private static final Map<String, Lock> processLocks = new ConcurrentHashMap<>();
	private static final Map<String, Map<String, Object>> caches = new ConcurrentHashMap<>();
	private static final Map<String, Set<String>> cacheCandidates = new ConcurrentHashMap<>();
	private static final Map<String, List<SharedPreferences.OnSharedPreferenceChangeListener>> allListeners = new ConcurrentHashMap<>();
	private static final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();

	Map<String, ReadWriteLock> getLocks() {
		return locks;
	}

	Map<String, Lock> getProcessLocks() {
		return processLocks;
	}

	Map<String, Map<String, Object>> getCaches() {
		return caches;
	}

	Map<String, List<SharedPreferences.OnSharedPreferenceChangeListener>> getAllListeners() {
		return allListeners;
	}

	Map<String, ExecutorService> getExecutors() {
		return executors;
	}

	Map<String, Set<String>> getCacheCandidates() {
		return cacheCandidates;
	}
}