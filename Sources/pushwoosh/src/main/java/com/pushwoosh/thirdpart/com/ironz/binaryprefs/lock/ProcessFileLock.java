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

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.exception.LockOperationException;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

final class ProcessFileLock implements Lock {

	private static final String RWD_MODE = "rwd";

	private final File lockFile;

	private RandomAccessFile randomAccessFile;
	private FileChannel channel;
	private FileLock lock;

	ProcessFileLock(File lockFile) {
		this.lockFile = lockFile;
	}

	@Override
	public void lock() {
		try {
			randomAccessFile = new RandomAccessFile(lockFile, RWD_MODE);
			channel = randomAccessFile.getChannel();
			lock = channel.lock();
		} catch (Exception e) {
			try {
				if (randomAccessFile != null) {
					randomAccessFile.close();
				}
				if (channel != null) {
					channel.close();
				}
			} catch (Exception ignored) {
			}
			throw new LockOperationException(e);
		}
	}

	@Override
	public void unlock() {
		try {
			if (lock != null && lock.isValid()) {
				lock.release();
			}
		} catch (Exception e) {
			throw new LockOperationException(e);
		} finally {
			try {
				if (randomAccessFile != null) {
					randomAccessFile.close();
				}
				if (channel != null) {
					channel.close();
				}
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public boolean tryLock() {
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public Condition newCondition() {
		throw new UnsupportedOperationException("Not implemented!");
	}
}