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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@code Set<String>} to byte array implementation and backwards
 */
public final class StringSetSerializer {

	/**
	 * Uses for detecting byte array type of {@link Set} of {@link String}
	 */
	private static final byte FLAG = -1;

	/**
	 * Minimum size primitive type of {@link Set}
	 */
	private static final int SIZE = 1;

	/**
	 * Serialize {@code Set<String>} into byte array with following scheme:
	 * [{@link #FLAG}] + (([string_size] + [string_byte_array]) * n).
	 *
	 * @param set target Set to serialize.
	 * @return specific byte array with scheme.
	 */
	public byte[] serialize(Set<String> set) {
		byte[][] bytes = new byte[set.size()][];
		int i = 0;
		int totalArraySize = 1;

		for (String s : set) {
			byte[] stringBytes = s.getBytes();
			byte[] stringSizeBytes = intToBytes(stringBytes.length);

			byte[] merged = new byte[stringBytes.length + stringSizeBytes.length];

			System.arraycopy(stringSizeBytes, 0, merged, 0, stringSizeBytes.length);
			System.arraycopy(stringBytes, 0, merged, stringSizeBytes.length, stringBytes.length);

			bytes[i] = merged;

			totalArraySize += merged.length;
			i++;
		}

		byte[] totalArray = new byte[totalArraySize];
		totalArray[0] = FLAG;

		int offset = 1;
		for (byte[] b : bytes) {
			System.arraycopy(b, 0, totalArray, offset, b.length);
			offset = offset + b.length;
		}

		return totalArray;
	}

	private byte[] intToBytes(int value) {
		int i = 0xff;
		return new byte[]{
				(byte) ((value >>> 24) & i),
				(byte) ((value >>> 16) & i),
				(byte) ((value >>> 8) & i),
				(byte) ((value) & i)
		};
	}

	/**
	 * Deserialize {@code Set<String>} by {@link #serialize(Set)} convention
	 *
	 * @param bytes target byte array for deserialization
	 * @return deserialized String Set
	 */
	public Set<String> deserialize(byte[] bytes) {
		byte flag = bytes[0];
		if (flag == FLAG) {

			Set<String> set = new HashSet<>();

			int i = 1;

			while (i < bytes.length) {

				int integerBytesSize = Integer.SIZE / 8;
				byte[] stringSizeBytes = new byte[integerBytesSize];
				System.arraycopy(bytes, i, stringSizeBytes, 0, stringSizeBytes.length);
				int stringSize = intFromBytes(stringSizeBytes);

				byte[] stringBytes = new byte[stringSize];

				for (int k = 0; k < stringBytes.length; k++) {
					int stringOffset = i + k + integerBytesSize;
					stringBytes[k] = bytes[stringOffset];
				}

				set.add(new String(stringBytes));

				i += integerBytesSize + stringSize;
			}

			return Collections.unmodifiableSet(set);
		}

		throw new ClassCastException(String.format("Set<String> cannot be deserialized in '%s' flag type", flag));
	}

	private int intFromBytes(byte[] bytes) {
		int i = 0xff;
		return ((bytes[3] & i)) +
		       ((bytes[2] & i) << 8) +
		       ((bytes[1] & i) << 16) +
		       ((bytes[0]) << 24);
	}

	public boolean isMatches(byte flag) {
		return flag == FLAG;
	}

	public int bytesLength() {
		return SIZE;
	}
}