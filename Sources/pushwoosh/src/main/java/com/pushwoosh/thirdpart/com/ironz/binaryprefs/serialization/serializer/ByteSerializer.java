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
 * Byte to byte array implementation and backwards
 */
public final class ByteSerializer {

	/**
	 * Uses for detecting byte primitive type of {@link Byte}
	 */
	private static final byte FLAG = -8;

	/**
	 * Minimum size primitive type of {@link Byte}
	 */
	private static final int SIZE = 2;

	/**
	 * Serialize {@code byte} into byte array with following scheme:
	 * [{@link #FLAG}] + [byte].
	 *
	 * @param value target byte to serialize.
	 * @return specific byte array with scheme.
	 */
	public byte[] serialize(byte value) {
		return new byte[]{
				FLAG,
				value
		};
	}

	/**
	 * Deserialize {@code byte} by {@link #serialize(byte)}  convention
	 *
	 * @param bytes target byte array for deserialization
	 * @return deserialized byte
	 */
	public byte deserialize(byte[] bytes) {
		return deserialize(bytes, 0);
	}

	/**
	 * Deserialize {@code byte} by {@link #serialize(byte)}  convention
	 *
	 * @param bytes  target byte array for deserialization
	 * @param offset bytes array offset
	 * @return deserialized byte
	 */
	public byte deserialize(byte[] bytes, int offset) {
		return bytes[offset + 1];
	}

	public boolean isMatches(byte flag) {
		return flag == FLAG;
	}

	public int bytesLength() {
		return SIZE;
	}
}