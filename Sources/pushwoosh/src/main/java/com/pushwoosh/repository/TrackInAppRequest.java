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

package com.pushwoosh.repository;

import java.math.BigDecimal;
import java.util.Date;

import com.pushwoosh.internal.network.PushRequest;

import org.json.JSONException;
import org.json.JSONObject;

class TrackInAppRequest extends PushRequest<Void> {
	private String sku;
	private Date purchaseTime;
	private long quantity;
	private String currency;
	private BigDecimal price;

	public TrackInAppRequest(String sku, BigDecimal price, String currency, Date purchaseTime) {
		this.sku = sku;
		this.purchaseTime = purchaseTime;
		this.price = price;
		this.currency = currency;

		quantity = 1;
	}

	@Override
	public String getMethod() {
		return "setPurchase";
	}

	@Override
	protected void buildParams(JSONObject params) throws JSONException {
		params.put("productIdentifier", sku);
		params.put("transactionDate", purchaseTime.getTime() / 1000); //in seconds
		params.put("quantity", quantity);
		params.put("currency", currency);
		params.put("price", price);
	}
}
