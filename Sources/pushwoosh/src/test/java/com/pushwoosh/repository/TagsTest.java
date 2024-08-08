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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.pushwoosh.BuildConfig;
import com.pushwoosh.exception.GetTagsException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.tags.TagsBundle;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.Expectation;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.RequestManagerMock;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.pushwoosh.internal.utils.MockConfig.APP_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by etkachenko on 4/11/17.
 */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
@Ignore("Takes too long, fix later")
public class TagsTest {
	private PlatformTestManager platformTestManager;

	private RequestManagerMock requestManagerMock;
	private NotificationPrefs notificationPrefs;

	// class under test
	private PushwooshRepository pushwooshRepository;

	@Before
	public void setUp() throws Exception {
		Config configMock = MockConfig.createMock();

		platformTestManager = new PlatformTestManager(configMock);
		platformTestManager.setUp();

		requestManagerMock = platformTestManager.getRequestManager();
		pushwooshRepository = platformTestManager.getPushwooshRepository();
		notificationPrefs = platformTestManager.getNotificationPrefs();

		ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
		when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
		WhiteboxHelper.setInternalState(pushwooshRepository, "serverCommunicationManager", serverCommunicationManager);
	}

	@After
	public void tearDown() throws Exception {
		platformTestManager.tearDown();
	}

	//
	// sendTags() part
	//-----------------------------------------------------------------------


	@Test
	public void sendIntTagTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.intTag("intTag", 42), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(3000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"intTag\" : 42}"), params.getJSONObject("tags"), true);
	}

	@Test
	public void sendLongTagTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.longTag("longTag", 9223372036854775807L), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"longTag\" : 9223372036854775807}"), params.getJSONObject("tags"), true);
	}

	@Test
	public void sendBooleanTagTrueTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.booleanTag("boolTag", true), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"boolTag\" : true}"), params.getJSONObject("tags"), true);
	}

	@Test
	public void sendStringTagTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.stringTag("stringTag", "someString"), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"stringTag\" : \"someString\"}"), params.getJSONObject("tags"), true);
	}

	@Test
	public void sendBooleanTagFalseTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.booleanTag("boolTag", false), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"boolTag\" : false}"), params.getJSONObject("tags"), true);
	}

	@Test
	public void sendListTagTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);
		List<String> listTags = new ArrayList<>();
		listTags.add("item1");
		listTags.add("item2");
		listTags.add("item3");

		// steps:
		pushwooshRepository.sendTags(Tags.listTag("listTag", listTags), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"listTag\" : [ \"item1\", \"item2\", \"item3\"]}"), params.getJSONObject("tags"), true);
	}

	@Test
	public void sendListTagEmptyTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.listTag("listTag", Collections.emptyList()), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"listTag\" : []}"), params.getJSONObject("tags"), true);
	}

	/* FIXME: date can be adjusted by 1 hour
	@Test
	public void sendDateTagTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.dateTag("dateTag", new Date(8080808080808L)), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"dateTag\" : \"2226-01-27 02:54\"}"), params.getJSONObject("tags"), true);
	}
	*/

	@Test
	public void sendIncrementIntTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.incrementInt("incremental", 5), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"incremental\" : { \"operation\" : \"increment\", \"value\" : 5}}"), params.getJSONObject("tags"), true);
	}

	@Test
	public void sendAppendListTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		List<String> listTags = new ArrayList<>();
		listTags.add("item1");
		listTags.add("item2");
		listTags.add("item3");

		// steps:
		pushwooshRepository.sendTags(Tags.appendList("incremental", listTags), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"incremental\" : { \"operation\" : \"append\", \"value\" : [ \"item1\", \"item2\", \"item3\" ]}}"), params.getJSONObject("tags"), true);
	}

	@Test
	public void sendRemoveTagTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.removeTag("toRemove"), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"toRemove\" : null}"), params.getJSONObject("tags"), true);
	}

	@Test
	public void sendEmptyTagTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.empty(), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{}"), params.getJSONObject("tags"), true);
	}

	@Test
	public void sendTagsBundleTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		List<String> listTag = new ArrayList<>();
		listTag.add("i1");
		listTag.add("i2");
		TagsBundle tags = new TagsBundle.Builder()
				.putInt("intTag", 3)
				.putLong("longTag", 12342445345356L)
				.putBoolean("boolTag", true)
	//			.putDate("dateTag", new Date(5050505050505L)) // FIXME: date can be adjusted by 1 hour
				.putList("listTag", listTag)
				.putString("stringTag", "smStr")
				.remove("toRemove")
				.build();
		pushwooshRepository.sendTags(tags, callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"intTag\" : 3, \"longTag\" : 12342445345356, \"boolTag\" : true, \"listTag\" : [\"i1\", \"i2\"], \"stringTag\" : \"smStr\", \"toRemove\" : null}"), params.getJSONObject("tags"), true);
	}

	//Tests sendTags with null listener
	@Test
	public void sendTagsNullListenerTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.intTag("intTag", 42), null);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"intTag\" : 42}"), params.getJSONObject("tags"), true);
	}

	//Tests sendTags request throws exception
	@Test
	public void setTagsWithExceptionTest() throws Exception {
		ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		requestManagerMock.setException(new NetworkException("test exception"), SetTagsRequest.class);

		// steps:
		pushwooshRepository.sendTags(Tags.intTag("intTag", 42), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000)).process(resultCaptor.capture());
		Result<Void, PushwooshException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(false));
	}

	//Tests accumulation and setTags sends only one correct request
	@Test
	public void setTagsAccumulationTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		Callback<Void, PushwooshException> callback = CallbackWrapper.spy();
		Expectation<JSONObject> expectation = requestManagerMock.expect(SetTagsRequest.class);

		List<String> listTag = new ArrayList<>();
		listTag.add("val1");
		listTag.add("val2");

		// steps:
		pushwooshRepository.sendTags(Tags.intTag("intTag", 4), callback);
		pushwooshRepository.sendTags(Tags.longTag("longTag", 4444444444444444444L), callback);
		pushwooshRepository.sendTags(Tags.stringTag("stringTag", "stringValue"), callback);
		pushwooshRepository.sendTags(Tags.booleanTag("boolTag", true), callback);
		pushwooshRepository.sendTags(Tags.listTag("listTag", listTag), callback);
		// pushwooshRepository.sendTags(Tags.dateTag("dateTag", new Date(20202020202020L)), callback); TODO: date can be adjusted by 1 hour
		pushwooshRepository.sendTags(Tags.removeTag("toRemove"), callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(2000).times(6)).process(any());

		verify(expectation, timeout(2000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();
		JSONAssert.assertEquals(new JSONObject("{\"intTag\" : 4, \"longTag\" : 4444444444444444444, \"boolTag\" : true, \"listTag\" : [\"val1\", \"val2\"], \"stringTag\" : \"stringValue\", \"toRemove\" : null}"), params.getJSONObject("tags"), true);
	}

	//
	// getTags() part
	//-----------------------------------------------------------------------

	//Tests getTags sends correct request and parse response
	// { \"result\" : { \"stringTag\" : \"string1\", \"intTag\" : 42, \"boolTag\" : true, \"listTag\" : [ \"val1\", \"val2\" ] } }
	@Test
	public void getTagsTest() throws Exception {
		Callback<TagsBundle, GetTagsException> callback = CallbackWrapper.spy();
		ArgumentCaptor<Result<TagsBundle, GetTagsException>> resultCaptor = ArgumentCaptor.forClass(Result.class);

		JSONObject response = new JSONObject("{ \"result\" : { \"stringTag\" : \"string1\", \"intTag\" : 42, \"boolTag\" : true, \"listTag\" : [ \"val1\", \"val2\" ] } }");
		requestManagerMock.setResponse(response, GetTagsRequest.class);

		// steps:
		pushwooshRepository.getTags(callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(1000)).process(resultCaptor.capture());
		Result<TagsBundle, GetTagsException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));
		JSONAssert.assertEquals(new JSONObject("{ \"stringTag\" : \"string1\", \"intTag\" : 42, \"boolTag\" : true, \"listTag\" : [ \"val1\", \"val2\" ] }"), result.getData().toJson(), true);
	}

	//Tests getTags onError with not empty tags
	@Test
	public void getTagsOnErrorWithTagsNotNullTest() throws Exception {
		Callback<TagsBundle, GetTagsException> callback = CallbackWrapper.spy();
		ArgumentCaptor<Result<TagsBundle, GetTagsException>> resultCaptor = ArgumentCaptor.forClass(Result.class);

		JSONObject cache = new JSONObject("{ \"stringTag\" : \"string1\", \"intTag\" : 42, \"boolTag\" : true, \"listTag\" : [ \"val1\", \"val2\" ] }");
		notificationPrefs.tags().set(cache);

		requestManagerMock.setException(new NetworkException("test exception"), GetTagsRequest.class);

		// steps:
		pushwooshRepository.getTags(callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(1000)).process(resultCaptor.capture());
		Result<TagsBundle, GetTagsException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(true));
		JSONAssert.assertEquals(new JSONObject("{ \"stringTag\" : \"string1\", \"intTag\" : 42, \"boolTag\" : true, \"listTag\" : [ \"val1\", \"val2\" ] }"), result.getData().toJson(), true);
	}

	//Tests getTag onError with empty tags
	@Test
	public void getTagsOnErrorTest() throws Exception {
		Callback<TagsBundle, GetTagsException> callback = CallbackWrapper.spy();
		ArgumentCaptor<Result<TagsBundle, GetTagsException>> resultCaptor = ArgumentCaptor.forClass(Result.class);

		requestManagerMock.setException(new NetworkException("test exception"), GetTagsRequest.class);

		// steps:
		pushwooshRepository.getTags(callback);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(callback, timeout(1000)).process(resultCaptor.capture());
		Result<TagsBundle, GetTagsException> result = resultCaptor.getValue();
		assertThat(result.isSuccess(), is(false));
	}

	//
	// sendAppOpen() part
	//-----------------------------------------------------------------------

	//Tests sendAppOpen method sends appOpenRequest with correct parameters
	@Test
	public void sendAppOpenTest() throws Exception {
		ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
		Expectation<JSONObject> expectation = requestManagerMock.expect(AppOpenRequest.class);

		// steps:
		pushwooshRepository.sendAppOpen();
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// postcondition:
		verify(expectation, timeout(1000)).fulfilled(captor.capture());
		JSONObject params = captor.getValue();

		assertThat(params.getString("application"), is(equalTo(APP_ID)));
	}
}
