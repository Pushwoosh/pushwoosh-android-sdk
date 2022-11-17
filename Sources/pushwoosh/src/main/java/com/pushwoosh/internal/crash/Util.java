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

package com.pushwoosh.internal.crash;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Util {
	private static final String APP_IDENTIFIER_PATTERN_SYMBOLS = "[0-9a-f]+";
	private static final int APP_IDENTIFIER_LENGTH = 32;
	private static final Pattern APP_IDENTIFIER_PATTERN = Pattern.compile(APP_IDENTIFIER_PATTERN_SYMBOLS, Pattern.CASE_INSENSITIVE);

	/**
	 * Sanitizes an app identifier or throws an exception if it can't be sanitized.
	 *
	 * @param appIdentifier the app identifier to sanitize
	 * @return the sanitized app identifier
	 * @throws java.lang.IllegalArgumentException if the app identifier can't be sanitized because of unrecoverable input character errors
	 */
	public static String sanitizeAppIdentifier(String appIdentifier) throws IllegalArgumentException {
		if (appIdentifier == null) {
			throw new IllegalArgumentException("App ID must not be null.");
		}

		String sAppIdentifier = appIdentifier.trim();

		Matcher matcher = APP_IDENTIFIER_PATTERN.matcher(sAppIdentifier);

		if (sAppIdentifier.length() != APP_IDENTIFIER_LENGTH) {
			throw new IllegalArgumentException("App ID length must be " + APP_IDENTIFIER_LENGTH + " characters.");
		} else if (!matcher.matches()) {
			throw new IllegalArgumentException("App ID must match regex pattern /" + APP_IDENTIFIER_PATTERN_SYMBOLS + "/i");
		}

		return sAppIdentifier;
	}
}
