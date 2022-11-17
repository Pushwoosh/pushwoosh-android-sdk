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

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.io.DataInput;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.io.DataOutput;

import java.io.Serializable;

/**
 * Only the identity of the class of an Persistable instance is
 * written in the serialization stream and it is the responsibility
 * of the class to save and restore the contents of its instances.
 * <p>
 * The {@link #writeExternal(DataOutput)} and {@link #readExternal(DataInput)}
 * methods of the Persistable interface are implemented by a class to give
 * the class complete control over the format and contents of the stream
 * for an object and its supertypes.
 * <p>
 *
 * @see Serializable
 */
public interface Persistable extends Serializable {
	/**
	 * The object implements the writeExternal method to save its contents
	 * by calling the methods of DataOutput for its primitive values.
	 *
	 * @param out the stream to write the object to
	 */
	void writeExternal(DataOutput out);

	/**
	 * The object implements the readExternal method to restore its
	 * contents by calling the methods of DataInput for primitive
	 * types. The readExternal method must read the values in the same sequence
	 * and with the same types as were written by writeExternal.
	 *
	 * @param in the stream to read data from in order to restore the object
	 */
	void readExternal(DataInput in);

	/**
	 * Creates and returns a deep cloned version of current object.
	 * The object implements the clone method to making a deep copy for
	 * constructing a new object for faster cache fetching proposes.
	 *
	 * @return new fully constructed persistable object
	 */
	Persistable deepClone();
}