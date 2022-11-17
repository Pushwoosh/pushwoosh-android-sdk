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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;

/**
 * <h3>Description</h3>
 * <p>
 * Builder class for HttpURLConnection.
 **/
class HttpURLConnectionBuilder {
	private static final int DEFAULT_TIMEOUT = 2 * 60 * 1000;
	public static final String DEFAULT_CHARSET = "UTF-8";
	public static final long FORM_FIELD_LIMIT = 4 * 1024 * 1024;
	public static final int FIELDS_LIMIT = 25;

	private final String urlString;

	private String requestMethod;
	private String requestBody;
	private int timeout = DEFAULT_TIMEOUT;

	private final Map<String, String> headers;

	public HttpURLConnectionBuilder(String urlString) {
		this.urlString = urlString;
		headers = new HashMap<>();
	}

	public HttpURLConnectionBuilder setRequestMethod(String method) {
		this.requestMethod = method;
		return this;
	}

	public HttpURLConnectionBuilder setRequestBody(String body) {
		this.requestBody = body;
		return this;
	}

	public HttpURLConnectionBuilder writeFormFields(Map<String, String> fields) {

		// We should add limit on fields because a large number of fields can throw the OOM exception
		if (fields.size() > FIELDS_LIMIT) {
			throw new IllegalArgumentException("Fields size too large: " + fields.size() + " - max allowed: " + FIELDS_LIMIT);
		}

		for (String key : fields.keySet()) {
			String value = fields.get(key);
			if (value != null && value.length() > FORM_FIELD_LIMIT) {
				throw new IllegalArgumentException("Form field " + key + " size too large: " + value.length() + " - max allowed: " + FORM_FIELD_LIMIT);
			}
		}

		try {
			String formString = getFormString(fields, DEFAULT_CHARSET);
			setHeader("Content-Type", "application/x-www-form-urlencoded");
			setRequestBody(formString);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public HttpURLConnectionBuilder setHeader(String name, String value) {
		headers.put(name, value);
		return this;
	}

	public HttpURLConnection build() throws IOException {
		HttpURLConnection connection;
		URL url = new URL(urlString);
		connection = (HttpURLConnection) url.openConnection();

		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);

		if (!TextUtils.isEmpty(requestMethod)) {
			connection.setRequestMethod(requestMethod);
			if (!TextUtils.isEmpty(requestBody) || requestMethod.equalsIgnoreCase("POST") || requestMethod.equalsIgnoreCase("PUT")) {
				connection.setDoOutput(true);
			}
		}

		for (String name : headers.keySet()) {
			connection.setRequestProperty(name, headers.get(name));
		}

		if (!TextUtils.isEmpty(requestBody)) {
			OutputStream outputStream = connection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, DEFAULT_CHARSET));
			writer.write(requestBody);
			writer.flush();
			writer.close();
		}

		return connection;
	}

	private static String getFormString(Map<String, String> params, String charset) throws UnsupportedEncodingException {
		List<String> protoList = new ArrayList<>();
		for (String key : params.keySet()) {
			String value = params.get(key);
			key = URLEncoder.encode(key, charset);
			value = URLEncoder.encode(value, charset);
			protoList.add(key + "=" + value);
		}
		return TextUtils.join("&", protoList);
	}

}
