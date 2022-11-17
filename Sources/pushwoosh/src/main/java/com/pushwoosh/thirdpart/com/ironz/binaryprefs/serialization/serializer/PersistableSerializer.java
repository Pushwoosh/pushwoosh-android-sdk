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

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.Persistable;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.PersistableRegistry;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.io.DataInput;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.io.DataOutput;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.io.PersistableObjectInput;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.io.PersistableObjectOutput;

/**
 * {@code Persistable} to byte array implementation and backwards
 */
public final class PersistableSerializer {

	/**
	 * Uses for detecting byte array primitive type of {@link Persistable}
	 */
	public static final byte FLAG = -11;

	private final BooleanSerializer booleanSerializer;
	private final ByteSerializer byteSerializer;
	private final ByteArraySerializer byteArraySerializer;
	private final CharSerializer charSerializer;
	private final DoubleSerializer doubleSerializer;
	private final FloatSerializer floatSerializer;
	private final IntegerSerializer integerSerializer;
	private final LongSerializer longSerializer;
	private final ShortSerializer shortSerializer;
	private final StringSerializer stringSerializer;
	private final PersistableRegistry persistableRegistry;

	public PersistableSerializer(BooleanSerializer booleanSerializer,
	                             ByteSerializer byteSerializer,
	                             ByteArraySerializer byteArraySerializer,
	                             CharSerializer charSerializer,
	                             DoubleSerializer doubleSerializer,
	                             FloatSerializer floatSerializer,
	                             IntegerSerializer integerSerializer,
	                             LongSerializer longSerializer,
	                             ShortSerializer shortSerializer,
	                             StringSerializer stringSerializer,
	                             PersistableRegistry persistableRegistry) {
		this.booleanSerializer = booleanSerializer;
		this.byteSerializer = byteSerializer;
		this.byteArraySerializer = byteArraySerializer;
		this.charSerializer = charSerializer;
		this.doubleSerializer = doubleSerializer;
		this.floatSerializer = floatSerializer;
		this.integerSerializer = integerSerializer;
		this.longSerializer = longSerializer;
		this.shortSerializer = shortSerializer;
		this.stringSerializer = stringSerializer;
		this.persistableRegistry = persistableRegistry;
	}

	/**
	 * Serialize {@code Persistable} into byte array with following scheme:
	 * [{@link PersistableSerializer#FLAG}] + [sequential primitives bytes].
	 *
	 * @param value target persistable to serialize.
	 * @return specific byte array with scheme.
	 */
	public byte[] serialize(Persistable value) {
		DataOutput output = new PersistableObjectOutput(
				booleanSerializer,
				byteSerializer,
				byteArraySerializer,
				charSerializer,
				doubleSerializer,
				floatSerializer,
				integerSerializer,
				longSerializer,
				shortSerializer,
				stringSerializer
		);
		return output.serialize(value);
	}

	/**
	 * Deserialize {@link Persistable} by {@link #serialize(Persistable)} convention
	 *
	 * @param key   key for determinate how to serialize
	 *              one type of class type or interface type by two or more
	 *              different serialization protocols.
	 * @param bytes target byte array for deserialization
	 * @return deserialized {@link Persistable}
	 */
	public Persistable deserialize(String key, byte[] bytes) {
		DataInput input = new PersistableObjectInput(
				booleanSerializer,
				byteSerializer,
				byteArraySerializer,
				charSerializer,
				doubleSerializer,
				floatSerializer,
				integerSerializer,
				longSerializer,
				shortSerializer,
				stringSerializer,
				persistableRegistry
		);
		return input.deserialize(key, bytes);
	}

	public boolean isMatches(byte flag) {
		return flag == FLAG;
	}
}