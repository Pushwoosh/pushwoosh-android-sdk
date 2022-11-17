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
 * Double to byte array implementation and backwards
 */
public final class DoubleSerializer {

	/**
	 * Uses for detecting byte array primitive type of {@link Double}
	 */
	private static final byte FLAG = -5;

	/**
	 * Minimum size primitive type of {@link Double}
	 */
	private static final int SIZE = 9;

	/**
	 * Serialize {@code double} into byte array with following scheme:
	 * [{@link #FLAG}] + [double_bytes].
	 *
	 * @param value target double to serialize.
	 * @return specific byte array with scheme.
	 */
	public byte[] serialize(double value) {
		long l = Double.doubleToLongBits(value);
		return new byte[]{
				FLAG,
				(byte) (l >>> 56),
				(byte) (l >>> 48),
				(byte) (l >>> 40),
				(byte) (l >>> 32),
				(byte) (l >>> 24),
				(byte) (l >>> 16),
				(byte) (l >>> 8),
				(byte) (l)
		};
	}

	/**
	 * Deserialize {@code double} by {@link #serialize(double)} convention
	 *
	 * @param bytes target byte array for deserialization
	 * @return deserialized double
	 */
	public double deserialize(byte[] bytes) {
		return deserialize(bytes, 0);
	}

	/**
	 * Deserialize {@code double} by {@link #serialize(double)} convention
	 *
	 * @param bytes  target byte array for deserialization
	 * @param offset bytes array offset
	 * @return deserialized double
	 */
	public double deserialize(byte[] bytes, int offset) {
		int i = 0xff;
		long value = ((bytes[8 + offset] & i)) +
		             ((bytes[7 + offset] & i) << 8) +
		             ((bytes[6 + offset] & i) << 16) +
		             ((long) (bytes[5 + offset] & i) << 24) +
		             ((long) (bytes[4 + offset] & i) << 32) +
		             ((long) (bytes[3 + offset] & i) << 40) +
		             ((long) (bytes[2 + offset] & i) << 48) +
		             ((long) (bytes[1 + offset]) << 56);
		return Double.longBitsToDouble(value);
	}

	public boolean isMatches(byte flag) {
		return flag == FLAG;
	}

	public int bytesLength() {
		return SIZE;
	}
}