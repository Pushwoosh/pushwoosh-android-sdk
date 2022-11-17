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

public final class TransactionElement {

	private static final byte[] EMPTY_CONTENT = {};

	private static final int ACTION_FETCH = 1;
	public static final int ACTION_UPDATE = 2;
	public static final int ACTION_REMOVE = 3;

	private final int action;
	private final String name;
	private final byte[] content;

	static TransactionElement createFetchElement(String name, byte[] content) {
		return new TransactionElement(ACTION_FETCH, name, content);
	}

	public static TransactionElement createUpdateElement(String name, byte[] content) {
		return new TransactionElement(ACTION_UPDATE, name, content);
	}

	public static TransactionElement createRemovalElement(String name) {
		return new TransactionElement(ACTION_REMOVE, name, EMPTY_CONTENT);
	}

	private TransactionElement(int action, String name, byte[] content) {
		this.action = action;
		this.name = name;
		this.content = content;
	}

	public int getAction() {
		return action;
	}

	public String getName() {
		return name;
	}

	public byte[] getContent() {
		return content;
	}
}