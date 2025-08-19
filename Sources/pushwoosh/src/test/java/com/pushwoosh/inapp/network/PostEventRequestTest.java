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

import static com.pushwoosh.internal.utils.MockConfig.APP_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.network.PushRequestHelper;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PostEventRequestTest {

	private PlatformTestManager platformTestManager;
	private InAppStorage inAppStorage;

	@Before
	public void setUp() throws Exception {
		Config configMock = MockConfig.createMock();

		platformTestManager = new PlatformTestManager(configMock);
		platformTestManager.setUp();

		inAppStorage = platformTestManager.getInAppStorage();
	}

	@After
	public void tearDown() throws Exception {
		platformTestManager.tearDown();
	}

	@Test
	public void getMethod() throws Exception {
		PostEventRequest request = new PostEventRequest("", "", Tags.empty());
		assertEquals("postEvent", request.getMethod());
	}

	@Test
	public void testBuildParams() throws Exception {
		PostEventRequest request = new PostEventRequest("testEvent", "", Tags.intTag("intTag", 15));


		JSONObject params = PushRequestHelper.getParams(request);
		assertThat(params.getString("application"), is(equalTo(APP_ID)));
		assertThat(params.getString("event"), is(equalTo("testEvent")));
		JSONAssert.assertEquals(new JSONObject("{\"intTag\" : 15}"), params.getJSONObject("attributes"), true);
	}

	@Test
	public void testParseResponseWithSavedCode() throws Exception {
		final String testCode = "1234-5678";
		JSONObject response = new JSONObject("{ \"code\" : \"" + testCode + "\" }");
		when(inAppStorage.getResource(testCode)).thenReturn(new Resource(testCode, null, null, 0, null, null, false, -1, null, null));

		PostEventRequest request = new PostEventRequest("", "", Tags.empty());
		String code = request.parseResponse(response).getCode();
		assert code != null;
		assertThat(code, is(equalTo(testCode)));
	}

	@Test
	public void testStressBadCode() throws Exception {
		JSONObject response = new JSONObject("{ \"code\" : [] }");

		PostEventRequest request = new PostEventRequest("", "", Tags.empty());
		request.parseResponse(response);
	}

	@Test
	public void testNoCode() throws Exception {
		JSONObject response = new JSONObject("{}");

		PostEventRequest request = new PostEventRequest("", "", Tags.empty());
		String code = request.parseResponse(response).getCode();
		assertThat(code, is(isEmptyString()));
	}
}
