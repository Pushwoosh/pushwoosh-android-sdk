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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.task;

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.event.ExceptionHandler;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Performs all submitted tasks in one separated thread sequentially.
 */
public final class ScheduledBackgroundTaskExecutor implements TaskExecutor {

	private static final int THREADS_COUNT = 1;
	private static final String THREAD_NAME_PREFIX = "binaryprefs-pool-%s";

	private final ExceptionHandler exceptionHandler;
	private final ExecutorService currentExecutor;

	public ScheduledBackgroundTaskExecutor(String prefName,
	                                       ExceptionHandler exceptionHandler,
	                                       Map<String, ExecutorService> executors) {
		this.exceptionHandler = exceptionHandler;
		this.currentExecutor = putIfAbsentExecutor(prefName, executors);
	}

	private ExecutorService putIfAbsentExecutor(final String prefName, Map<String, ExecutorService> executors) {
		if (executors.containsKey(prefName)) {
			return executors.get(prefName);
		}
		ThreadFactory factory = createThreadFactory(prefName);
		ExecutorService service = Executors.newFixedThreadPool(THREADS_COUNT, factory);
		executors.put(prefName, service);
		return service;
	}

	private ThreadFactory createThreadFactory(final String prefName) {
		return new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return createThread(r, prefName);
			}
		};
	}

	private Thread createThread(final Runnable r, final String prefName) {
		Thread thread = new Thread(r);
		thread.setName(String.format(THREAD_NAME_PREFIX, prefName));
		thread.setPriority(Thread.MAX_PRIORITY);
		return thread;
	}

	@Override
	public FutureBarrier submit(final Runnable runnable) {
		Future<?> submit = currentExecutor.submit(runnable);
		return new FutureBarrier(submit, exceptionHandler);
	}

	@Override
	public FutureBarrier submit(Callable<?> callable) {
		Future<?> submit = currentExecutor.submit(callable);
		return new FutureBarrier(submit, exceptionHandler);
	}
}