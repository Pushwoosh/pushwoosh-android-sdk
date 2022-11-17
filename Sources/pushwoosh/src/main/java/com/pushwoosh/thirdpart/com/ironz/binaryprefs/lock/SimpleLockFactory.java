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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.lock;

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.directory.DirectoryProvider;

import java.io.File;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Simple lock factory for providing lock by instance and global lock by preference name.
 */
public final class SimpleLockFactory implements LockFactory {

	private static final String LOCK_EXTENSION = ".lock";

	private final File lockDirectory;

	private final ReadWriteLock readWriteLock;
	private final Lock processLock;

	public SimpleLockFactory(String prefName,
	                         DirectoryProvider provider,
	                         Map<String, ReadWriteLock> locks,
	                         Map<String, Lock> processLocks) {
		this.lockDirectory = provider.getLockDirectory();
		this.readWriteLock = putIfAbsentLocalLock(prefName, locks);
		this.processLock = putIfAbsentProcessLock(prefName, processLocks);
	}

	private ReadWriteLock putIfAbsentLocalLock(String name, Map<String, ReadWriteLock> locks) {
		if (locks.containsKey(name)) {
			return locks.get(name);
		}
		ReadWriteLock lock = new ReentrantReadWriteLock(true);
		locks.put(name, lock);
		return lock;
	}

	private Lock putIfAbsentProcessLock(String name, Map<String, Lock> processLocks) {
		if (processLocks.containsKey(name)) {
			return processLocks.get(name);
		}
		File file = new File(lockDirectory, name + LOCK_EXTENSION);
		Lock lock = new ProcessFileLock(file);
		processLocks.put(name, lock);
		return lock;
	}

	@Override
	public Lock getReadLock() {
		return readWriteLock.readLock();
	}

	@Override
	public Lock getWriteLock() {
		return readWriteLock.writeLock();
	}

	@Override
	public Lock getProcessLock() {
		return processLock;
	}
}