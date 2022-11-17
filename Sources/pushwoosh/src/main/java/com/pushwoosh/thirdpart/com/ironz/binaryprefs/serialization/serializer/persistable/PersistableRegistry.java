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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable;

import java.util.HashMap;
import java.util.Map;

public final class PersistableRegistry {

	private static final String CANNOT_FIND_MESSAGE = "Cannot find Persistable type for '%s' key. " +
	                                                  "Please, add it through 'registerPersistable' builder method.";
	private static final String ALREADY_REGISTERED_MESSAGE = "Registry already contains '%s' class for '%s' key. " +
	                                                         "Please, don't add persistable by similar key twice.";

	private final Map<String, Class<? extends Persistable>> map = new HashMap<>();

	public void register(String key, Class<? extends Persistable> clazz) {
		if (map.containsKey(key)) {
			throw new UnsupportedOperationException(String.format(ALREADY_REGISTERED_MESSAGE, clazz.getName(), key));
		}
		map.put(key, clazz);
	}

	public Class<? extends Persistable> get(String key) {
		if (!map.containsKey(key)) {
			throw new UnsupportedClassVersionError(String.format(CANNOT_FIND_MESSAGE, key));
		}
		return map.get(key);
	}
}