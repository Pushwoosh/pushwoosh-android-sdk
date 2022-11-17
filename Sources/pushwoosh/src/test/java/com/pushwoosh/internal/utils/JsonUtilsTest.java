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

package com.pushwoosh.internal.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class JsonUtilsTest {
	@Test
	public void jsonToMapTest() throws Exception {
		JSONObject json = new JSONObject("{\"intProp\" : 42, \"longProp\" : 191911919191919199, \"nullProp\" : null, \"boolProp\" : true, \"stringProp\" : \"someStr\", \"listProp\" : [\"i1\", \"i2\"]}");
		Map<String, Object> map = JsonUtils.jsonToMap(json);

		assertThat(map.size(), is(6));
		assertThat(map.get("intProp"), is(equalTo(42)));
		assertThat(map.get("longProp"), is(equalTo(191911919191919199L)));
		assertThat(map.get("nullProp"), is(nullValue()));
		assertThat(map.get("boolProp"), is(true));
		assertThat(map.get("stringProp"), is(equalTo("someStr")));
		assertThat(((List) map.get("listProp")).toArray(), arrayContainingInAnyOrder(Arrays.asList(equalTo("i1"), equalTo("i2"))));
	}

	@Test
	public void nestedJsonToMapTest() throws Exception {
		JSONObject json = new JSONObject("{\"nested\" : {\"intProp\" : 42, \"longProp\" : 191911919191919199, \"nullProp\" : null}}");
		Map<String, Object> map = JsonUtils.jsonToMap(json);

		Map<String, Object> nested = (Map<String, Object>) map.get("nested");
		assertThat(nested.get("intProp"), is(equalTo(42)));
		assertThat(nested.get("longProp"), is(equalTo(191911919191919199L)));
		assertThat(nested.get("nullProp"), is(nullValue()));
	}

	@Test
	public void nullJsonToMapTest() throws Exception {
		Map<String, Object> map = JsonUtils.jsonToMap(null);

		assertThat(map.keySet(), is(empty()));
	}

	@Test
	public void jsonToListTest() throws Exception {
		JSONArray json = new JSONArray("[\"i1\", 5, null, 1308582386878298]");
		List<Object> list = JsonUtils.jsonToList(json);

		assertThat(list, hasSize(4));
		assertThat(list.toArray(), arrayContainingInAnyOrder(Arrays.asList(equalTo("i1"), equalTo(5), nullValue(), equalTo(1308582386878298L))));
	}


	@Test
	public void emptyJsonToMapTest() throws Exception {
		JSONObject json = new JSONObject("{}");
		Map<String, Object> map = JsonUtils.jsonToMap(json);

		assertThat(map.keySet(), is(empty()));
	}

	@Test
	public void mapToJson() throws Exception {
		List<Object> nestedList = Arrays.asList("str", 5, null);
		Map<String, Object> nestedMap = new HashMap<>();
		nestedMap.put("i", 4);
		nestedMap.put("b", true);

		Map<String, Object> map = new HashMap<>();
		map.put("intKey", 42);
		map.put("longKey", 1308582386878298L);
		map.put("boolKey", true);
		map.put("nullKey", null);
		map.put("listKey", nestedList);
		map.put("mapKey", nestedMap);

		JSONObject json = JsonUtils.mapToJson(map);

		JSONObject expected = new JSONObject("{\"intKey\" : 42, \"longKey\" : 1308582386878298, \"boolKey\" : true, \"nullKey\" : null, \"listKey\" : [\"str\", 5, null], \"mapKey\" : {\"i\": 4, \"b\": true}}");
		JSONAssert.assertEquals(expected, json, true);
	}

	@Test
	public void nullMapToJson() throws Exception {
		JSONObject json = JsonUtils.mapToJson(null);
		JSONAssert.assertEquals(new JSONObject(), json, true);
	}

	@Test
	public void arrayToJson() throws Exception {
		List<Object> nestedList = Arrays.asList("str", 5, null);
		Map<String, Object> nestedMap = new HashMap<>();
		nestedMap.put("i", 4);
		nestedMap.put("b", true);
		List<Object> list = Arrays.asList("str", 5, null, 1308582386878298L, nestedList.toArray(), nestedMap);

		JSONArray array = JsonUtils.arrayToJson(list.toArray());

		JSONArray expected = new JSONArray("[\"str\", 5, null, 1308582386878298, [\"str\", 5, null], {\"i\" : 4, \"b\" : true}]");
		JSONAssert.assertEquals(expected, array, true);
	}

	@Test
	public void emptyMapToJson() throws Exception {
		JSONObject json = JsonUtils.mapToJson(Collections.emptyMap());
		JSONAssert.assertEquals(new JSONObject(), json, true);
	}

	@Test
	public void mergeJsonTest() throws Exception {
		JSONObject from = new JSONObject("{\"i\" : 4, \"b\" : true, \"l\" : [1, \"asdf\"], \"o\" : {\"i\" : 12}}");
		JSONObject to = new JSONObject("{\"i\" : 7, \"l\" : [1], \"long\" : 1308582386878298}");

		JsonUtils.mergeJson(from, to);

		JSONObject expected = new JSONObject("{\"i\" : 4, \"b\" : true, \"l\" : [1, \"asdf\"], \"o\" : {\"i\" : 12}, \"long\" : 1308582386878298}");
		JSONAssert.assertEquals(expected, to, true);
	}

	@Test
	public void toEmptyMergeJsonTest() throws Exception {
		JSONObject from = new JSONObject("{\"i\" : 4, \"b\" : true, \"l\" : [1, \"asdf\"], \"o\" : {\"i\" : 12}}");
		JSONObject to = new JSONObject("{}");

		JsonUtils.mergeJson(from, to);

		JSONObject expected = new JSONObject("{\"i\" : 4, \"b\" : true, \"l\" : [1, \"asdf\"], \"o\" : {\"i\" : 12}}");
		JSONAssert.assertEquals(expected, to, true);
	}

	@Test
	public void fromEmptyMergeJsonTest() throws Exception {
		JSONObject from = new JSONObject("{}");
		JSONObject to = new JSONObject("{\"i\" : 7, \"l\" : [1], \"long\" : 1308582386878298}");

		JsonUtils.mergeJson(from, to);

		JSONObject expected = new JSONObject("{\"i\" : 7, \"l\" : [1], \"long\" : 1308582386878298}");
		JSONAssert.assertEquals(expected, to, true);
	}

	@Test
	public void fromEmptyToEmptyMergeJsonTest() throws Exception {
		JSONObject from = new JSONObject("{}");
		JSONObject to = new JSONObject("{}");

		JsonUtils.mergeJson(from, to);

		JSONObject expected = new JSONObject("{}");
		JSONAssert.assertEquals(expected, to, true);
	}

	@Test
	public void clearJsonObject() throws Exception {
		JSONObject json = new JSONObject("{\"i\" : 4, \"b\" : true, \"l\" : [1, \"asdf\"], \"o\" : {\"i\" : 12}}");

		JsonUtils.clearJsonObject(json);

		JSONAssert.assertEquals(new JSONObject(), json, true);
	}

	@Test
	public void clearEmptyJsonObject() throws Exception {
		JSONObject json = new JSONObject("{}");

		JsonUtils.clearJsonObject(json);

		JSONAssert.assertEquals(new JSONObject(), json, true);
	}
}