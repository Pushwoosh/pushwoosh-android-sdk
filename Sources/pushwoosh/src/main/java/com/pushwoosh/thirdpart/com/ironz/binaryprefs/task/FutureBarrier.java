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
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.exception.FileOperationException;

import java.util.concurrent.Future;

/**
 * Meta object which holds current task state and allows blocking await.
 */
public final class FutureBarrier {

	private final Future<?> future;
	private final ExceptionHandler exceptionHandler;

	FutureBarrier(Future<?> future, ExceptionHandler exceptionHandler) {
		this.future = future;
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Complete task without exception handle and re-throws exception on higher level.
	 */
	public void completeBlockingUnsafe() {
		try {
			future.get();
		} catch (Exception e) {
			throw new FileOperationException(e);
		}
	}

	/**
	 * Complete task with exception handle and returns result or default value for this task.
	 */
	public Object completeBlockingWihResult(Object defValue) {
		try {
			return future.get();
		} catch (Exception e) {
			exceptionHandler.handle(e);
		}
		return defValue;
	}

	/**
	 * Complete task with result returning without exception handle and re-throws exception on higher level.
	 */
	public Object completeBlockingWithResultUnsafe() {
		try {
			return future.get();
		} catch (Exception e) {
			throw new FileOperationException(e);
		}
	}

	/**
	 * Returns task execution result.
	 * Also this method will call exception handle method if task execution fails.
	 *
	 * @return status - {@code true} if task completed successfully {@code false} otherwise
	 */
	public boolean completeBlockingWithStatus() {
		try {
			future.get();
			return true;
		} catch (Exception e) {
			exceptionHandler.handle(e);
		}
		return false;
	}
}