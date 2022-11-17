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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer;

/**
 * {@code String} to byte array implementation and backwards
 */
public final class StringSerializer {

	/**
	 * Uses for detecting byte array type of {@link String}
	 */
	private static final byte FLAG = -2;
	/**
	 * Minimum size primitive type of {@link String}
	 */
	private static final int SIZE = 1;

	/**
	 * Describes flag offset size
	 */
	private static final int FLAG_OFFSET = 1;

	/**
	 * Serialize {@code String} into byte array with following scheme:
	 * [{@link #FLAG}] + [string_byte_array].
	 *
	 * @param s target String to serialize.
	 * @return specific byte array with scheme.
	 */
	public byte[] serialize(String s) {
		byte[] stringBytes = s.getBytes();
		byte[] b = new byte[stringBytes.length + FLAG_OFFSET];
		b[0] = FLAG;
		System.arraycopy(stringBytes, 0, b, FLAG_OFFSET, stringBytes.length);
		return b;
	}

	/**
	 * Deserialize {@link String} by {@link #serialize(String)} convention
	 *
	 * @param bytes target byte array for deserialization
	 * @return deserialized String
	 */
	public String deserialize(byte[] bytes) {
		return deserialize(bytes, 0, bytes.length - 1);
	}

	/**
	 * Deserialize {@link String} by {@link #serialize(String)} convention
	 *
	 * @param bytes  target byte array for deserialization
	 * @param offset bytes array offset
	 * @param length bytes array length
	 * @return deserialized String
	 */
	public String deserialize(byte[] bytes, int offset, int length) {
		return new String(bytes, FLAG_OFFSET + offset, length);
	}

	public boolean isMatches(byte flag) {
		return flag == FLAG;
	}

	public int bytesLength() {
		return SIZE;
	}
}