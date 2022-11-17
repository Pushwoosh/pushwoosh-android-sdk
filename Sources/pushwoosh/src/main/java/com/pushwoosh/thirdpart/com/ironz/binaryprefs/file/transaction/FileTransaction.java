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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.transaction;

import java.util.List;
import java.util.Set;

/**
 * Transaction contract which describes one file event mechanism.
 * Contract guarantees what disk changes will be performed
 * successful or rolled back to old values.
 */
public interface FileTransaction {

	/**
	 * Acquires global lock for current transaction. {@link #unlock()} - unlocks the current lock.
	 */
	void lock();

	/**
	 * Release global lock for current transaction which is acquired by {@link #lock()} method.
	 */
	void unlock();

	/**
	 * Retrieves all file adapter elements and creates {@code byte[]} elements by unique name.
	 *
	 * @return unique transaction elements.
	 */
	List<TransactionElement> fetchAll();

	/**
	 * Retrieves all file adapter element names without creating an {@code byte[]}.
	 *
	 * @return unique transaction elements.
	 */
	Set<String> fetchNames();

	/**
	 * Retrieves one file adapter element and creates {@code byte[]} element by unique name.
	 *
	 * @param name file name
	 * @return unique transaction element.
	 */
	TransactionElement fetchOne(String name);

	/**
	 * Performs disk write for all transaction values sequentially.
	 *
	 * @param elements target elements for transaction.
	 */
	void commit(List<TransactionElement> elements);
}