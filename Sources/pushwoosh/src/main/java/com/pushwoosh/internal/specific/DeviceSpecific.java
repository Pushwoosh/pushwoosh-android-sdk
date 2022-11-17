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

package com.pushwoosh.internal.specific;

import com.pushwoosh.internal.registrar.PushRegistrar;

import org.json.JSONObject;

/**
 * Some specific information depending on which cloud messaging service is used
 */
public interface DeviceSpecific {

	/**
	 * @return {@link com.pushwoosh.internal.registrar.PushRegistrar} which will be used for the current application
	 */
	PushRegistrar pushRegistrar();

	/**
	 * Permission which should be checking
	 * @param packageName name of package associated with current application
	 * @return full name of permission
	 */
	String permission(final String packageName);

	/**
	 * @return device type which should be sending to service
	 * @see com.pushwoosh.internal.network.PushRequest#buildParams(JSONObject)
	 */
	int deviceType();

	/**
	 * @return senderId which is setting up for the application
	 */
	String projectId();

	/**
	 * @return type of current DeviceSpecific
	 */
	String type();
}
