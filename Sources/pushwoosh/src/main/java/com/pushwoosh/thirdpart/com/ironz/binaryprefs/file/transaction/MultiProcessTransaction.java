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

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.encryption.KeyEncryption;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.encryption.ValueEncryption;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.adapter.FileAdapter;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.lock.LockFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

public final class MultiProcessTransaction implements FileTransaction {

	private final FileAdapter fileAdapter;
	private final Lock lock;
	private final KeyEncryption keyEncryption;
	private final ValueEncryption valueEncryption;

	public MultiProcessTransaction(FileAdapter fileAdapter,
	                               LockFactory lockFactory,
	                               KeyEncryption keyEncryption,
	                               ValueEncryption valueEncryption) {
		this.fileAdapter = fileAdapter;
		this.lock = lockFactory.getProcessLock();
		this.valueEncryption = valueEncryption;
		this.keyEncryption = keyEncryption;
	}

	@Override
	public void lock() {
		lock.lock();
	}

	@Override
	public void unlock() {
		lock.unlock();
	}

	@Override
	public List<TransactionElement> fetchAll() {
		return fetchAllInternal();
	}

	@Override
	public Set<String> fetchNames() {
		return fetchNamesInternal();
	}

	@Override
	public TransactionElement fetchOne(String name) {
		return fetchOneInternal(name);
	}

	@Override
	public void commit(List<TransactionElement> elements) {
		commitInternal(elements);
	}

	private List<TransactionElement> fetchAllInternal() {
		String[] names = fileAdapter.names();
		List<TransactionElement> elements = new ArrayList<>(names.length);
		for (String name : names) {
			TransactionElement element = fetchOneInternal(name);
			elements.add(element);
		}
		return elements;
	}

	private Set<String> fetchNamesInternal() {
		String[] names = fileAdapter.names();
		Set<String> temp = new HashSet<>();
		for (String name : names) {
			String decrypt = keyEncryption.decrypt(name);
			temp.add(decrypt);
		}
		return temp;
	}

	private TransactionElement fetchOneInternal(String name) {
		String encryptName = keyEncryption.encrypt(name);
		byte[] content = fileAdapter.fetch(encryptName);
		byte[] decryptValue = valueEncryption.decrypt(content);
		return TransactionElement.createFetchElement(name, decryptValue);
	}

	private void commitInternal(List<TransactionElement> elements) {
		for (TransactionElement element : elements) {
			int action = element.getAction();
			String name = element.getName();
			String encryptedName = keyEncryption.encrypt(name);
			if (action == TransactionElement.ACTION_UPDATE) {
				byte[] value = element.getContent();
				byte[] encryptedValue = valueEncryption.encrypt(value);
				fileAdapter.save(encryptedName, encryptedValue);
			}
			if (action == TransactionElement.ACTION_REMOVE) {
				fileAdapter.remove(encryptedName);
			}
		}
	}
}