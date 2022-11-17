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

package com.pushwoosh.inapp.model;

import java.io.Serializable;

/**
 * View model for representing InApp into webView
 *
 * @see com.pushwoosh.inapp.view.InAppFragment
 * @see com.pushwoosh.inapp.view.RichMediaWebActivity
 */
public class HtmlData implements Serializable {

	private static final long serialVersionUID = -4864095466623838402L;

	private String code;
	private String url;
	private String htmlContent;

	public HtmlData(String code, String url, String htmlContent) {
		this.code = code;
		this.url = url;
		this.htmlContent = htmlContent;
	}

	public String getCode() {
		return code;
	}

	public String getUrl() {
		return url;
	}

	public String getHtmlContent() {
		return htmlContent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		HtmlData htmlData = (HtmlData) o;

		return code != null ? code.equals(htmlData.code) : htmlData.code == null && (url != null ? url.equals(htmlData.url) : htmlData.url == null);

	}

	@Override
	public int hashCode() {
		int result = code != null ? code.hashCode() : 0;
		result = 31 * result + (url != null ? url.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "HtmlData{" +
		       "code='" + code + '\'' +
		       ", url='" + url + '\'' +
		       '}';
	}
}