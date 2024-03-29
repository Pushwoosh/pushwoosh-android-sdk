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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization;

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.exception.SerializationException;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.*;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.Persistable;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.PersistableRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains all serializers which possible for data transformation.
 * This is non-public api class.
 */
public final class SerializerFactory {

	private static final String ZERO_BYTES_MESSAGE = "%s key's value is zero bytes for deserialize";

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
	private final StringSetSerializer stringSetSerializer;
	private final PersistableSerializer persistableSerializer;

	public SerializerFactory(PersistableRegistry persistableRegistry) {
		this.booleanSerializer = new BooleanSerializer();
		this.byteSerializer = new ByteSerializer();
		this.charSerializer = new CharSerializer();
		this.doubleSerializer = new DoubleSerializer();
		this.floatSerializer = new FloatSerializer();
		this.integerSerializer = new IntegerSerializer();
		this.longSerializer = new LongSerializer();
		this.shortSerializer = new ShortSerializer();
		this.stringSerializer = new StringSerializer();
		this.stringSetSerializer = new StringSetSerializer();
		this.byteArraySerializer = new ByteArraySerializer();
		this.persistableSerializer = new PersistableSerializer(
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
	}

	public Object deserialize(String key, byte[] bytes) {

		if (bytes.length == 0) {
			throw new SerializationException(String.format(ZERO_BYTES_MESSAGE, key));
		}

		byte flag = bytes[0];
		if (booleanSerializer.isMatches(flag)) {
			return booleanSerializer.deserialize(bytes);
		}
		if (integerSerializer.isMatches(flag)) {
			return integerSerializer.deserialize(bytes);
		}
		if (longSerializer.isMatches(flag)) {
			return longSerializer.deserialize(bytes);
		}
		if (doubleSerializer.isMatches(flag)) {
			return doubleSerializer.deserialize(bytes);
		}
		if (floatSerializer.isMatches(flag)) {
			return floatSerializer.deserialize(bytes);
		}
		if (stringSerializer.isMatches(flag)) {
			return stringSerializer.deserialize(bytes);
		}
		if (stringSetSerializer.isMatches(flag)) {
			return stringSetSerializer.deserialize(bytes);
		}
		if (persistableSerializer.isMatches(flag)) {
			return persistableSerializer.deserialize(key, bytes);
		}
		if (shortSerializer.isMatches(flag)) {
			return shortSerializer.deserialize(bytes);
		}
		if (byteSerializer.isMatches(flag)) {
			return byteSerializer.deserialize(bytes);
		}
		if (byteArraySerializer.isMatches(flag)) {
			return byteArraySerializer.deserialize(bytes);
		}
		if (charSerializer.isMatches(flag)) {
			return charSerializer.deserialize(bytes);
		}
		throw new UnsupportedClassVersionError(String.format("Flag verification failed. Incorrect flag '%s'", flag));
	}

	public Object redefineMutable(Object o) {
		if (o instanceof Persistable) {
			return ((Persistable) o).deepClone();
		}
		if (o instanceof Set) {
			//noinspection unchecked
			Set<String> strings = (Set<String>) o;
			return new HashSet<>(strings);
		}
		return o;
	}

	public BooleanSerializer getBooleanSerializer() {
		return booleanSerializer;
	}

	public ByteSerializer getByteSerializer() {
		return byteSerializer;
	}

	public CharSerializer getCharSerializer() {
		return charSerializer;
	}

	public DoubleSerializer getDoubleSerializer() {
		return doubleSerializer;
	}

	public FloatSerializer getFloatSerializer() {
		return floatSerializer;
	}

	public IntegerSerializer getIntegerSerializer() {
		return integerSerializer;
	}

	public LongSerializer getLongSerializer() {
		return longSerializer;
	}

	public ShortSerializer getShortSerializer() {
		return shortSerializer;
	}

	public StringSerializer getStringSerializer() {
		return stringSerializer;
	}

	public StringSetSerializer getStringSetSerializer() {
		return stringSetSerializer;
	}

	public PersistableSerializer getPersistableSerializer() {
		return persistableSerializer;
	}

	public ByteArraySerializer getByteArraySerializer() {
		return byteArraySerializer;
	}
}