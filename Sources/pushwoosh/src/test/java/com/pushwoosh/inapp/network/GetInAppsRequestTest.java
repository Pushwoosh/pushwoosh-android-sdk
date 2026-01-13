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

package com.pushwoosh.inapp.network;

import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GetInAppsRequestTest {

	@Test
	public void testGetMethod() throws Exception {
		GetInAppsRequest request = new GetInAppsRequest();
		assertEquals("getInApps", request.getMethod());
	}

	@Test
	public void testParseResponse() throws Exception {
		JSONObject response = new JSONObject("{\"inApps\":[{\"url\":\"https:\\/\\/richmedia.pushwoosh.com\\/5\\/9\\/59D14-A0C23.zip\"," +
				"\"code\":\"CE545-99A0A\",\"layout\":\"topbanner\",\"updated\":1548402966,\"closeButtonType\":1," +
				"\"hash\":\"ae274622772dc8fee4fda8e653de1a81\",\"required\":true,\"priority\":0," +
				"\"businessCase\":\"app-update-message\",\"gdpr\":\"\"}," +
				"{\"url\":\"https:\\/\\/richmedia.pushwoosh.com\\/7\\/0\\/70949-07AA8.zip\",\"code\":\"C6360-00FA8\"," +
				"\"layout\":\"topbanner\",\"updated\":1548402985,\"closeButtonType\":1,\"hash\":\"dd3554c4dc2099962514e23c12cc019f\"," +
				"\"required\":true,\"priority\":0,\"businessCase\":\"push-register\",\"gdpr\":\"\"}," +
				"{\"url\":\"https:\\/\\/richmedia.pushwoosh.com\\/B\\/5\\/B5EFB-7D128.zip\",\"code\":\"8B699-A1F47\"," +
				"\"layout\":\"topbanner\",\"updated\":1548402974,\"closeButtonType\":1,\"hash\":\"6da40bc86072402a6114d79a1db39eb5\"," +
				"\"required\":false,\"priority\":1,\"businessCase\":\"\",\"gdpr\":\"\"}]}");

		GetInAppsRequest request = new GetInAppsRequest();
		List<Resource> inApps = request.parseResponse(response);

		assertEquals(3, inApps.size());

		Resource firstInApp = inApps.get(0);
		assertEquals("https://richmedia.pushwoosh.com/5/9/59D14-A0C23.zip", firstInApp.getUrl());
		assertEquals("CE545-99A0A", firstInApp.getCode());
		assertEquals(InAppLayout.of("topbanner"), firstInApp.getLayout());
		assertEquals(1548402966, firstInApp.getUpdated());
		assertEquals("ae274622772dc8fee4fda8e653de1a81", firstInApp.getHash());
		assertTrue(firstInApp.isRequired());
		assertEquals(0, firstInApp.getPriority());

		Resource secondInApp = inApps.get(1);
		assertEquals("https://richmedia.pushwoosh.com/7/0/70949-07AA8.zip", secondInApp.getUrl());
		assertEquals("C6360-00FA8", secondInApp.getCode());
		assertEquals(InAppLayout.of("topbanner"), secondInApp.getLayout());
		assertEquals(1548402985, secondInApp.getUpdated());
		assertEquals("dd3554c4dc2099962514e23c12cc019f", secondInApp.getHash());
		assertTrue(secondInApp.isRequired());
		assertEquals(0, secondInApp.getPriority());

		Resource thirdInApp = inApps.get(2);
		assertEquals("https://richmedia.pushwoosh.com/B/5/B5EFB-7D128.zip", thirdInApp.getUrl());
		assertEquals("8B699-A1F47", thirdInApp.getCode());
		assertEquals(InAppLayout.of("topbanner"), thirdInApp.getLayout());
		assertEquals(1548402974, thirdInApp.getUpdated());
		assertEquals("6da40bc86072402a6114d79a1db39eb5", thirdInApp.getHash());
		assertFalse(thirdInApp.isRequired());
		assertEquals(1, thirdInApp.getPriority());
	}

	@Test
	public void testEmptyInApps() throws Exception {
		JSONObject response = new JSONObject("{\"inApps\":[]}");

		GetInAppsRequest request = new GetInAppsRequest();
		List<Resource> inApps = request.parseResponse(response);
		assertEquals(0, inApps.size());
	}

	@Test
	public void testNoInApps() throws Exception {
		JSONObject response = new JSONObject("{ }");

		GetInAppsRequest request = new GetInAppsRequest();
		List<Resource> inApps = request.parseResponse(response);
		assertEquals(0, inApps.size());
	}

	@Test(expected = JSONException.class)
	public void testNoUrl() throws Exception {
		JSONObject response = new JSONObject("{\"inApps\":[{\"code\":\"CE545-99A0A\",\"layout\":\"topbanner\"," +
				"\"updated\":1548402966,\"closeButtonType\":1,\"hash\":\"ae274622772dc8fee4fda8e653de1a81\"," +
				"\"required\":true,\"priority\":0,\"businessCase\":\"app-update-message\",\"gdpr\":\"\"}]}");

		GetInAppsRequest request = new GetInAppsRequest();
		List<Resource> inApps = request.parseResponse(response);
		Resource inApp = inApps.get(0);

		assertNull(inApp.getUrl());
	}

	@Test(expected = JSONException.class)
	public void testNoCode() throws Exception {
		JSONObject response = new JSONObject("{\"inApps\":[{\"url\":\"https:\\/\\/richmedia.pushwoosh.com\\/5\\/9\\/59D14-A0C23.zip\"," +
				"\"layout\":\"topbanner\",\"updated\":1548402966,\"closeButtonType\":1,\"hash\":\"ae274622772dc8fee4fda8e653de1a81\"," +
				"\"required\":true,\"priority\":0,\"businessCase\":\"app-update-message\",\"gdpr\":\"\"}]}");

		GetInAppsRequest request = new GetInAppsRequest();
		List<Resource> inApps = request.parseResponse(response);
		Resource inApp = inApps.get(0);

		assertNull(inApp.getCode());
	}

	@Test
	public void testNoLayout() throws Exception {
		JSONObject response = new JSONObject("{\"inApps\":[{\"url\":\"https:\\/\\/richmedia.pushwoosh.com\\/5\\/9\\/59D14-A0C23.zip\"," +
				"\"code\":\"CE545-99A0A\",\"updated\":1548402966,\"closeButtonType\":1,\"hash\":\"ae274622772dc8fee4fda8e653de1a81\"," +
				"\"required\":true,\"priority\":0,\"businessCase\":\"app-update-message\",\"gdpr\":\"\"}]}");
		GetInAppsRequest request = new GetInAppsRequest();
		Resource inApp = request.parseResponse(response).get(0);
		assertEquals(InAppLayout.TOP, inApp.getLayout());
	}

	@Test(expected = JSONException.class)
	public void testNoUpdatedField() throws Exception {
		JSONObject response = new JSONObject("{\"inApps\":[{\"url\":\"https:\\/\\/richmedia.pushwoosh.com\\/5\\/9\\/59D14-A0C23.zip\"," +
				"\"code\":\"CE545-99A0A\",\"layout\":\"topbanner\",\"closeButtonType\":1,\"hash\":\"ae274622772dc8fee4fda8e653de1a81\"," +
				"\"required\":true,\"priority\":0,\"businessCase\":\"app-update-message\",\"gdpr\":\"\"}]}");
		GetInAppsRequest request = new GetInAppsRequest();
		request.parseResponse(response);
	}

	@Test
	public void testNoHash() throws Exception {
		JSONObject response = new JSONObject("{\"inApps\":[{\"url\":\"https:\\/\\/richmedia.pushwoosh.com\\/5\\/9\\/59D14-A0C23.zip\"," +
				"\"code\":\"CE545-99A0A\",\"layout\":\"topbanner\",\"updated\":1548402966,\"closeButtonType\":1," +
				"\"required\":true,\"priority\":0,\"businessCase\":\"app-update-message\",\"gdpr\":\"\"}]}");
		GetInAppsRequest request = new GetInAppsRequest();
		List<Resource> inApps = request.parseResponse(response);
		Resource inApp = inApps.get(0);

		assertEquals("", inApp.getHash());
	}

	@Test
	public void testNoRequiredField() throws Exception {
		JSONObject response = new JSONObject("{\"inApps\":[{\"url\":\"https:\\/\\/richmedia.pushwoosh.com\\/5\\/9\\/59D14-A0C23.zip\"," +
				"\"code\":\"CE545-99A0A\",\"layout\":\"topbanner\",\"updated\":1548402966,\"closeButtonType\":1," +
				"\"hash\":\"ae274622772dc8fee4fda8e653de1a81\",\"priority\":0,\"gdpr\":\"\"}]}");
		GetInAppsRequest request = new GetInAppsRequest();
		List<Resource> inApps = request.parseResponse(response);
		Resource inApp = inApps.get(0);

		assertFalse(inApp.isRequired());
	}

	@Test
	public void testNoPriority() throws Exception {
		JSONObject response = new JSONObject("{\"inApps\":[{\"url\":\"https:\\/\\/richmedia.pushwoosh.com\\/5\\/9\\/59D14-A0C23.zip\"," +
				"\"code\":\"CE545-99A0A\",\"layout\":\"topbanner\",\"updated\":1548402966,\"closeButtonType\":1," +
				"\"hash\":\"ae274622772dc8fee4fda8e653de1a81\",\"required\":true,\"businessCase\":\"app-update-message\",\"gdpr\":\"\"}]}");
		GetInAppsRequest request = new GetInAppsRequest();
		List<Resource> inApps = request.parseResponse(response);
		Resource inApp = inApps.get(0);

		assertEquals(0, inApp.getPriority());
	}

	@Test(expected = JSONException.class)
	public void testNoFieldsAtAll() throws Exception {
		JSONObject response = new JSONObject("{\"inApps\":[{}]}");
		GetInAppsRequest request = new GetInAppsRequest();
		List<Resource> inApps = request.parseResponse(response);
	}
}
