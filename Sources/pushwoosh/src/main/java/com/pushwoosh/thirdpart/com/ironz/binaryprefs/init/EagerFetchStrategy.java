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

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.candidates.CacheCandidateProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.provider.CacheProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.transaction.FileTransaction;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.transaction.TransactionElement;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.lock.LockFactory;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.SerializerFactory;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.task.FutureBarrier;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.task.TaskExecutor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

public final class EagerFetchStrategy implements FetchStrategy {

	private final Lock readLock;
	private final TaskExecutor taskExecutor;
	private final CacheCandidateProvider candidateProvider;
	private final CacheProvider cacheProvider;
	private final FileTransaction fileTransaction;
	private final SerializerFactory serializerFactory;

	public EagerFetchStrategy(LockFactory lockFactory,
	                          TaskExecutor taskExecutor,
	                          CacheCandidateProvider candidateProvider,
	                          CacheProvider cacheProvider,
	                          FileTransaction fileTransaction,
	                          SerializerFactory serializerFactory) {
		this.readLock = lockFactory.getReadLock();
		this.taskExecutor = taskExecutor;
		this.candidateProvider = candidateProvider;
		this.cacheProvider = cacheProvider;
		this.fileTransaction = fileTransaction;
		this.serializerFactory = serializerFactory;
		fetchCache();
	}

	private void fetchCache() {
		fileTransaction.lock();
		readLock.lock();
		try {
			FutureBarrier barrier = taskExecutor.submit(new Runnable() {
				@Override
				public void run() {
					fetchCacheInternal();
				}
			});
			barrier.completeBlockingUnsafe();
		} finally {
			fileTransaction.unlock();
			readLock.unlock();
		}
	}

	private void fetchCacheInternal() {
		if (!shouldFetch()) {
			return;
		}
		for (TransactionElement element : fileTransaction.fetchAll()) {
			String name = element.getName();
			byte[] bytes = element.getContent();
			Object o = serializerFactory.deserialize(name, bytes);
			cacheProvider.put(name, o);
			candidateProvider.put(name);
		}
	}

	private boolean shouldFetch() {
		Set<String> candidates = candidateProvider.keys();
		Set<String> cacheKeys = cacheProvider.keys();
		return !cacheKeys.containsAll(candidates);
	}

	@Override
	public Object getValue(String key, Object defValue) {
		readLock.lock();
		try {
			Object o = cacheProvider.get(key);
			if (o == null) {
				return defValue;
			}
			return serializerFactory.redefineMutable(o);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public Map<String, Object> getAll() {
		readLock.lock();
		try {
			Map<String, Object> all = cacheProvider.getAll();
			Map<String, Object> clone = new HashMap<>(all.size());
			for (String key : all.keySet()) {
				Object value = all.get(key);
				Object redefinedValue = serializerFactory.redefineMutable(value);
				clone.put(key, redefinedValue);
			}
			return Collections.unmodifiableMap(clone);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public boolean contains(String key) {
		readLock.lock();
		try {
			return cacheProvider.contains(key);
		} finally {
			readLock.unlock();
		}
	}
}