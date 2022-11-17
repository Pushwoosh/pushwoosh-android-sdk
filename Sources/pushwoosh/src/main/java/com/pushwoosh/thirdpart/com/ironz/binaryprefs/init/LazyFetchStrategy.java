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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.candidates.CacheCandidateProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.provider.CacheProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.transaction.FileTransaction;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.transaction.TransactionElement;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.lock.LockFactory;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.SerializerFactory;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.task.FutureBarrier;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.task.TaskExecutor;

public final class LazyFetchStrategy implements FetchStrategy {

	private final Lock readLock;
	private final TaskExecutor taskExecutor;
	private final CacheCandidateProvider candidateProvider;
	private final CacheProvider cacheProvider;
	private final FileTransaction fileTransaction;
	private final SerializerFactory serializerFactory;

	public LazyFetchStrategy(LockFactory lockFactory,
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
		fetchCacheCandidates();
	}

	private void fetchCacheCandidates() {
		readLock.lock();
		try {
			for (String name : fileTransaction.fetchNames()) {
				candidateProvider.put(name);
			}
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public Object getValue(String key, Object defValue) {
		readLock.lock();
		try {
			Object o = getInternal(key, defValue);
			return serializerFactory.redefineMutable(o);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public Map<String, Object> getAll() {
		readLock.lock();
		try {
			Set<String> names = candidateProvider.keys();
			HashMap<String, Object> clone = new HashMap<>(names.size());
			for (String name : names) {
				Object o = getInternal(name);
				Object redefinedValue = serializerFactory.redefineMutable(o);
				clone.put(name, redefinedValue);
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
			Set<String> names = candidateProvider.keys();
			return names.contains(key) && cacheProvider.contains(key);
		} finally {
			readLock.unlock();
		}
	}

	private Object getInternal(final String key) {
		Object cached = cacheProvider.get(key);
		if (cached != null) {
			return cached;
		}
		fileTransaction.lock();
		try {
			FutureBarrier barrier = taskExecutor.submit(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					return fetchObject(key);
				}
			});
			return barrier.completeBlockingWithResultUnsafe();
		} finally {
			fileTransaction.unlock();
		}
	}

	private Object getInternal(final String key, Object defValue) {
		Object cached = cacheProvider.get(key);
		if (cached != null) {
			return cached;
		}
		Set<String> names = candidateProvider.keys();
		if (!names.contains(key)) {
			return defValue;
		}
		fileTransaction.lock();
		try {
			FutureBarrier barrier = taskExecutor.submit(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					return fetchObject(key);
				}
			});
			return barrier.completeBlockingWihResult(defValue);
		} finally {
			fileTransaction.unlock();
		}
	}

	private Object fetchObject(String key) {
		TransactionElement element = fileTransaction.fetchOne(key);
		byte[] bytes = element.getContent();
		Object deserialize = serializerFactory.deserialize(key, bytes);
		cacheProvider.put(key, deserialize);
		return deserialize;
	}
}