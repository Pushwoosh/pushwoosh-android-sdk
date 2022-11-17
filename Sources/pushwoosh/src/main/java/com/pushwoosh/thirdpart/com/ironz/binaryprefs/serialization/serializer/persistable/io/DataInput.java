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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.io;

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.Persistable;

public interface DataInput {

	/**
	 * Reads one input byte and returns
	 * <code>true</code> if that byte is nonzero,
	 * <code>false</code> if that byte is zero.
	 *
	 * @return the <code>boolean</code> value read.
	 */
	boolean readBoolean();

	/**
	 * Reads and returns one input byte.
	 * The byte is treated as a signed value in
	 * the range <code>-128</code> through <code>127</code>,
	 * inclusive.
	 *
	 * @return the 8-bit value read.
	 */
	byte readByte();

	/**
	 * Reads and returns input byte array.
	 * The byte array is treated as a signed value in
	 * the range <code>-128</code> through <code>127</code>,
	 * inclusive.
	 *
	 * @return byte array read.
	 */
	byte[] readByteArray();

	/**
	 * Reads two input bytes and returns
	 * a <code>short</code> value. Let <code>a</code>
	 * be the first byte read and <code>b</code>
	 * be the second byte.
	 *
	 * @return the 16-bit value read.
	 */
	short readShort();

	/**
	 * Reads two input bytes and returns a <code>char</code> value.
	 * Let <code>a</code>
	 * be the first byte read and <code>b</code>
	 * be the second byte.
	 *
	 * @return the <code>char</code> value read.
	 */
	char readChar();

	/**
	 * Reads four input bytes and returns an
	 * <code>int</code> value. Let <code>a-d</code>
	 * be the first through fourth bytes read.
	 *
	 * @return the <code>int</code> value read.
	 */
	int readInt();

	/**
	 * Reads eight input bytes and returns
	 * a <code>long</code> value. Let <code>a-h</code>
	 * be the first through eighth bytes read.
	 *
	 * @return the <code>long</code> value read.
	 */
	long readLong();

	/**
	 * Reads four input bytes and returns
	 * a <code>float</code> value.
	 *
	 * @return the <code>float</code> value read.
	 */
	float readFloat();

	/**
	 * Reads eight input bytes and returns
	 * a <code>double</code> value.
	 *
	 * @return the <code>double</code> value read.
	 */
	double readDouble();

	/**
	 * Reads input string's length and string bytes and returns
	 * a <code>java.lang.String</code> value.
	 * <p>
	 * Please note that before string will be read it's length will be read by {@link #readInt()} method.
	 * </p>
	 *
	 * @return the <code>java.lang.String</code> value read.
	 */
	String readString();

	/**
	 * Deserializes byte array into given {@link Persistable} object by given key.
	 *
	 * @param key   given preference key for {@link Persistable} object define
	 *              see {@link com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.PersistableRegistry}.
	 * @param bytes given byte array created by specific output scheme
	 * @return deserialized object
	 */
	Persistable deserialize(String key, byte[] bytes);
}