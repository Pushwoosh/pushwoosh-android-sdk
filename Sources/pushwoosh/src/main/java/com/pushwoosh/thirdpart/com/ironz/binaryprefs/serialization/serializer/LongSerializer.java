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
 * Long to byte array implementation and backwards
 */
public final class LongSerializer {

	/**
	 * Uses for detecting byte array primitive type of {@link Long}
	 */
	private static final byte FLAG = -4;

	/**
	 * Minimum size primitive type of {@link Long}
	 */
	private static final int SIZE = 9;

	/**
	 * Serialize {@code long} into byte array with following scheme:
	 * [{@link #FLAG}] + [long_bytes].
	 *
	 * @param value target long to serialize.
	 * @return specific byte array with scheme.
	 */
	public byte[] serialize(long value) {
		return new byte[]{
				FLAG,
				(byte) (value >>> 56),
				(byte) (value >>> 48),
				(byte) (value >>> 40),
				(byte) (value >>> 32),
				(byte) (value >>> 24),
				(byte) (value >>> 16),
				(byte) (value >>> 8),
				(byte) (((long) value))
		};
	}

	/**
	 * Deserialize {@code long} by {@link #serialize(long)} convention
	 *
	 * @param bytes target byte array for deserialization
	 * @return deserialized long
	 */
	public long deserialize(byte[] bytes) {
		return deserialize(bytes, 0);
	}

	/**
	 * Deserialize {@code long} by {@link #serialize(long)} convention
	 *
	 * @param bytes  target byte array for deserialization
	 * @param offset bytes array offset
	 * @return deserialized long
	 */
	public long deserialize(byte[] bytes, int offset) {
		long l = 0xffL;
		return ((bytes[8 + offset] & l)) +
		       ((bytes[7 + offset] & l) << 8) +
		       ((bytes[6 + offset] & l) << 16) +
		       ((bytes[5 + offset] & l) << 24) +
		       ((bytes[4 + offset] & l) << 32) +
		       ((bytes[3 + offset] & l) << 40) +
		       ((bytes[2 + offset] & l) << 48) +
		       (((long) bytes[1 + offset]) << 56);
	}

	public boolean isMatches(byte flag) {
		return flag == FLAG;
	}

	public int bytesLength() {
		return SIZE;
	}
}