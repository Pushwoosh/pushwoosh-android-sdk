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

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.candidates.CacheCandidateProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.provider.CacheProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.event.EventBridge;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.exception.TransactionInvalidatedException;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.transaction.FileTransaction;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.transaction.TransactionElement;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.SerializerFactory;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.Persistable;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.strategy.SerializationStrategy;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.strategy.impl.*;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.task.FutureBarrier;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.task.TaskExecutor;

import java.util.*;
import java.util.concurrent.locks.Lock;

final class BinaryPreferencesEditor implements PreferencesEditor {

	private static final String TRANSACTED_TWICE_MESSAGE = "Transaction should be applied or committed only once!";

	private final Map<String, SerializationStrategy> strategyMap = new HashMap<>();
	private final Set<String> removeSet = new HashSet<>();

	private final FileTransaction fileTransaction;
	private final EventBridge bridge;
	private final TaskExecutor taskExecutor;
	private final SerializerFactory serializerFactory;
	private final CacheProvider cacheProvider;
	private final CacheCandidateProvider candidateProvider;
	private final Lock writeLock;

	private boolean invalidated;

	BinaryPreferencesEditor(FileTransaction fileTransaction,
	                        EventBridge bridge,
	                        TaskExecutor taskExecutor,
	                        SerializerFactory serializerFactory,
	                        CacheProvider cacheProvider,
	                        CacheCandidateProvider candidateProvider,
	                        Lock writeLock) {
		this.fileTransaction = fileTransaction;
		this.bridge = bridge;
		this.taskExecutor = taskExecutor;
		this.serializerFactory = serializerFactory;
		this.cacheProvider = cacheProvider;
		this.candidateProvider = candidateProvider;
		this.writeLock = writeLock;
	}

	@Override
	public PreferencesEditor putString(String key, String value) {
		if (value == null) {
			return remove(key);
		}
		writeLock.lock();
		try {
			SerializationStrategy strategy = new StringSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PreferencesEditor putStringSet(String key, Set<String> value) {
		if (value == null) {
			return remove(key);
		}
		writeLock.lock();
		try {
			SerializationStrategy strategy = new StringSetSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PreferencesEditor putInt(String key, int value) {
		writeLock.lock();
		try {
			SerializationStrategy strategy = new IntegerSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PreferencesEditor putLong(String key, long value) {
		writeLock.lock();
		try {
			SerializationStrategy strategy = new LongSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PreferencesEditor putFloat(String key, float value) {
		writeLock.lock();
		try {
			SerializationStrategy strategy = new FloatSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PreferencesEditor putBoolean(String key, boolean value) {
		writeLock.lock();
		try {
			SerializationStrategy strategy = new BooleanSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public <T extends Persistable> PreferencesEditor putPersistable(String key, T value) {
		if (value == null) {
			return remove(key);
		}
		writeLock.lock();
		try {
			SerializationStrategy strategy = new PersistableSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PreferencesEditor putByte(String key, byte value) {
		writeLock.lock();
		try {
			SerializationStrategy strategy = new ByteSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PreferencesEditor putShort(String key, short value) {
		writeLock.lock();
		try {
			SerializationStrategy strategy = new ShortSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PreferencesEditor putChar(String key, char value) {
		writeLock.lock();
		try {
			SerializationStrategy strategy = new CharSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PreferencesEditor putDouble(String key, double value) {
		writeLock.lock();
		try {
			SerializationStrategy strategy = new DoubleSerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.lock();
		}
	}

	@Override
	public PreferencesEditor putByteArray(String key, byte[] value) {
		writeLock.lock();
		try {
			SerializationStrategy strategy = new ByteArraySerializationStrategy(value, serializerFactory);
			strategyMap.put(key, strategy);
			return this;
		} finally {
			writeLock.lock();
		}
	}

	@Override
	public PreferencesEditor remove(String key) {
		writeLock.lock();
		try {
			removeSet.add(key);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PreferencesEditor clear() {
		writeLock.lock();
		try {
			Set<String> all = candidateProvider.keys();
			removeSet.addAll(all);
			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void apply() {
		writeLock.lock();
		try {
			performTransaction();
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean commit() {
		writeLock.lock();
		try {
			FutureBarrier barrier = performTransaction();
			return barrier.completeBlockingWithStatus();
		} finally {
			writeLock.unlock();
		}
	}

	private FutureBarrier performTransaction() {
		removeCache();
		storeCache();
		invalidate();
		return taskExecutor.submit(new Runnable() {
			@Override
			public void run() {
				commitTransaction();
			}
		});
	}

	private void removeCache() {
		for (String name : removeSet) {
			candidateProvider.remove(name);
			cacheProvider.remove(name);
		}
	}

	private void storeCache() {
		for (String name : strategyMap.keySet()) {
			SerializationStrategy strategy = strategyMap.get(name);
			Object value = strategy.getValue();
			candidateProvider.put(name);
			cacheProvider.put(name, value);
		}
	}

	private void invalidate() {
		if (invalidated) {
			throw new TransactionInvalidatedException(TRANSACTED_TWICE_MESSAGE);
		}
		invalidated = true;
	}

	private void commitTransaction() {
		List<TransactionElement> transaction = createTransaction();
		fileTransaction.commit(transaction);
		notifyListeners(transaction);
	}

	private List<TransactionElement> createTransaction() {
		List<TransactionElement> elements = new LinkedList<>();
		elements.addAll(removePersistence());
		elements.addAll(storePersistence());
		return elements;
	}

	private List<TransactionElement> removePersistence() {
		List<TransactionElement> elements = new LinkedList<>();
		for (String name : removeSet) {
			TransactionElement e = TransactionElement.createRemovalElement(name);
			elements.add(e);
		}
		return elements;
	}

	private List<TransactionElement> storePersistence() {
		Set<String> strings = strategyMap.keySet();
		List<TransactionElement> elements = new LinkedList<>();
		for (String name : strings) {
			SerializationStrategy strategy = strategyMap.get(name);
			byte[] bytes = strategy.serialize();
			TransactionElement e = TransactionElement.createUpdateElement(name, bytes);
			elements.add(e);
		}
		return elements;
	}

	private void notifyListeners(List<TransactionElement> transaction) {
		for (TransactionElement element : transaction) {
			String name = element.getName();
			byte[] bytes = element.getContent();
			if (element.getAction() == TransactionElement.ACTION_REMOVE) {
				bridge.notifyListenersRemove(name);
			}
			if (element.getAction() == TransactionElement.ACTION_UPDATE) {
				bridge.notifyListenersUpdate(name, bytes);
			}
		}
	}
}