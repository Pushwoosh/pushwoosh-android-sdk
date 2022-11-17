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

package com.pushwoosh.function;

import androidx.annotation.Nullable;

import com.pushwoosh.exception.PushwooshException;

/**
 * Result class encapsulates result of an asynchronous operation
 *
 * @param <T> Data Class
 * @param <E> Exception Class
 */
public class Result<T, E extends PushwooshException> {
	private final T data;
	private final E exception;

	private Result(T data, E exception) {
		this.data = data;
		this.exception = exception;
	}

	/**
	 * Factory method that constructs successful result with given data
	 *
	 * @param data result data
	 * @param <T>  result data class
	 * @param <E>  result exception class
	 * @return result for given data
	 */
	public static <T, E extends PushwooshException> Result<T, E> fromData(T data) {
		return new Result<>(data, null);
	}

	/**
	 * Factory method that constructs unsuccessful result with given exception
	 *
	 * @param exception result exception
	 * @param <T>       result data class
	 * @param <E>       result exception class
	 * @return result for given exception
	 */
	public static <T, E extends PushwooshException> Result<T, E> fromException(E exception) {
		return new Result<>(null, exception);
	}

	public static <T, E extends PushwooshException> Result<T, E> from(T data, E exception) {
		return new Result<>(data, exception);
	}

	/**
	 * @return true if operation was successful
	 */
	public boolean isSuccess() {
		return exception == null;
	}

	/**
	 * @return result data
	 */
	@Nullable
	public T getData() {
		return data;
	}

	/**
	 * @return result exception
	 */
	@Nullable
	public E getException() {
		return exception;
	}
}
