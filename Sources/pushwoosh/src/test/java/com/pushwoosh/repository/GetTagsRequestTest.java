package com.pushwoosh.repository;

import com.pushwoosh.tags.TagsBundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.Assert.assertEquals;

public class GetTagsRequestTest {

	@Test
	public void getMethod() throws Exception {
		GetTagsRequest request = new GetTagsRequest();
		assertEquals("getTags", request.getMethod());
	}

	// response : { "response" : { "result" : { "stringTag" : "string1", "intTag" : 42, "listTag" : [ "item1", "item2", "item3" ], "boolTag" : true } } }
	@Test
	public void testGetTags() throws Exception {
		JSONObject response = new JSONObject("{ \"result\" : { \"stringTag\" : \"string1\", \"intTag\" : 42, \"listTag\" : "
				+ "[ \"item1\", \"item2\", \"item3\" ], \"boolTag\" : true } }");

		GetTagsRequest request = new GetTagsRequest();
		TagsBundle tagsBundle = request.parseResponse(response);

		JSONAssert.assertEquals(new JSONObject("{ \"stringTag\" : \"string1\", \"intTag\" : 42, \"listTag\" : [ \"item1\", \"item2\", \"item3\" ], \"boolTag\" : true }"), tagsBundle.toJson(), true);
	}

	// response : {}
	@Test(expected = JSONException.class)
	public void testStressEmpty() throws Exception {
		JSONObject response = new JSONObject("{  }");

		GetTagsRequest request = new GetTagsRequest();
		request.parseResponse(response);
	}

	// response : { "response" : { "result" : [] } }
	@Test(expected = JSONException.class)
	public void testStressArrayResponse() throws Exception {
		JSONObject response = new JSONObject("{ \"response\" : { \"result\" : [] } }");

		GetTagsRequest request = new GetTagsRequest();
		request.parseResponse(response);
	}
}
