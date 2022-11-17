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

public interface DataOutput {

	/**
	 * Writes a <code>boolean</code> value to this output stream.
	 *
	 * @param v the boolean to be written.
	 */
	void writeBoolean(boolean v);

	/**
	 * Writes to the output stream the eight low-
	 * order bits of the argument <code>v</code>.
	 *
	 * @param v the <code>byte</code> value to be written.
	 */
	void writeByte(byte v);

	/**
	 * Writes to the output stream the eight low-
	 * order bits array of the argument <code>v</code>.
	 *
	 * @param v the <code>byte[]</code> value to be written.
	 */
	void writeByteArray(byte[] v);

	/**
	 * Writes two bytes to the output
	 * stream to represent the value of the argument.
	 *
	 * @param v the <code>short</code> value to be written.
	 */
	void writeShort(short v);

	/**
	 * Writes a <code>char</code> value, which
	 * is comprised of two bytes, to the
	 * output stream.
	 *
	 * @param v the <code>char</code> value to be written.
	 */
	void writeChar(char v);

	/**
	 * Writes an <code>int</code> value, which is
	 * comprised of four bytes, to the output stream.
	 *
	 * @param v the <code>int</code> value to be written.
	 */
	void writeInt(int v);

	/**
	 * Writes a <code>long</code> value, which is
	 * comprised of eight bytes, to the output stream.
	 *
	 * @param v the <code>long</code> value to be written.
	 */
	void writeLong(long v);

	/**
	 * Writes a <code>float</code> value,
	 * which is comprised of four bytes, to the output stream.
	 *
	 * @param v the <code>float</code> value to be written.
	 */
	void writeFloat(float v);

	/**
	 * Writes a <code>double</code> value,
	 * which is comprised of eight bytes, to the output stream.
	 *
	 * @param v the <code>double</code> value to be written.
	 */
	void writeDouble(double v);

	/**
	 * Writes a <code>String</code> value,
	 * which is comprised of n bytes, to the output stream.
	 *
	 * @param s the <code>String</code> not null value to be written.
	 */
	void writeString(String s);

	/**
	 * Serializes all input object data into byte array with specific scheme
	 *
	 * @param value given object
	 * @return byte array
	 */
	byte[] serialize(Persistable value);
}
