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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class JsonUtilsTest {
    @Test
    public void jsonToMapTest() throws Exception {
        JSONObject json = new JSONObject(
                "{\"intProp\" : 42, \"longProp\" : 191911919191919199, \"nullProp\" : null, \"boolProp\" : true, \"stringProp\" : \"someStr\", \"listProp\" : [\"i1\", \"i2\"]}");
        Map<String, Object> map = JsonUtils.jsonToMap(json);

        assertThat(map.size(), is(6));
        assertThat(map.get("intProp"), is(equalTo(42)));
        assertThat(map.get("longProp"), is(equalTo(191911919191919199L)));
        assertThat(map.get("nullProp"), is(nullValue()));
        assertThat(map.get("boolProp"), is(true));
        assertThat(map.get("stringProp"), is(equalTo("someStr")));
        assertThat(
                ((List) map.get("listProp")).toArray(),
                arrayContainingInAnyOrder(Arrays.asList(equalTo("i1"), equalTo("i2"))));
    }

    @Test
    public void nestedJsonToMapTest() throws Exception {
        JSONObject json = new JSONObject(
                "{\"nested\" : {\"intProp\" : 42, \"longProp\" : 191911919191919199, \"nullProp\" : null}}");
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
        assertThat(
                list.toArray(),
                arrayContainingInAnyOrder(
                        Arrays.asList(equalTo("i1"), equalTo(5), nullValue(), equalTo(1308582386878298L))));
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

        JSONObject expected = new JSONObject(
                "{\"intKey\" : 42, \"longKey\" : 1308582386878298, \"boolKey\" : true, \"nullKey\" : null, \"listKey\" : [\"str\", 5, null], \"mapKey\" : {\"i\": 4, \"b\": true}}");
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

        JSONArray expected =
                new JSONArray("[\"str\", 5, null, 1308582386878298, [\"str\", 5, null], {\"i\" : 4, \"b\" : true}]");
        JSONAssert.assertEquals(expected, array, true);
    }

    @Test
    public void mergeJsonTest() throws Exception {
        JSONObject from = new JSONObject("{\"i\" : 4, \"b\" : true, \"l\" : [1, \"asdf\"], \"o\" : {\"i\" : 12}}");
        JSONObject to = new JSONObject("{\"i\" : 7, \"l\" : [1], \"long\" : 1308582386878298}");

        JsonUtils.mergeJson(from, to);

        JSONObject expected = new JSONObject(
                "{\"i\" : 4, \"b\" : true, \"l\" : [1, \"asdf\"], \"o\" : {\"i\" : 12}, \"long\" : 1308582386878298}");
        JSONAssert.assertEquals(expected, to, true);
    }

    @Test
    public void clearJsonObject() throws Exception {
        JSONObject json = new JSONObject("{\"i\" : 4, \"b\" : true, \"l\" : [1, \"asdf\"], \"o\" : {\"i\" : 12}}");

        JsonUtils.clearJsonObject(json);

        JSONAssert.assertEquals(new JSONObject(), json, true);
    }

    // --- jsonToMap convertSubobjectToString=true ---

    @Test
    public void jsonToMapWithConvertSubobjectToStringSerializesNestedAsStrings() throws Exception {
        JSONObject json = new JSONObject("{\"obj\":{\"k\":1},\"arr\":[1,2]}");

        Map<String, Object> map = JsonUtils.jsonToMap(json, true);

        assertThat(map.get("obj"), is(instanceOf(String.class)));
        assertThat((String) map.get("obj"), containsString("\"k\""));
        assertThat((String) map.get("obj"), containsString("1"));
        assertThat(map.get("arr"), is(instanceOf(String.class)));
        assertThat((String) map.get("arr"), containsString("1"));
        assertThat((String) map.get("arr"), containsString("2"));
    }

    // --- jsonToList nested branches ---

    @Test
    public void jsonToListUnpacksNestedArrayAndObject() throws Exception {
        JSONArray json = new JSONArray("[[1,2],{\"k\":\"v\"},null]");

        List<Object> list = JsonUtils.jsonToList(json);

        assertThat(list, hasSize(3));
        assertThat(list.get(0), is(instanceOf(List.class)));
        List<?> nested = (List<?>) list.get(0);
        assertThat(nested, hasSize(2));
        assertThat(nested.get(0), is(equalTo(1)));
        assertThat(nested.get(1), is(equalTo(2)));
        assertThat(list.get(1), is(instanceOf(Map.class)));
        assertThat(((Map<?, ?>) list.get(1)).get("k"), is(equalTo("v")));
        assertNull(list.get(2));
    }

    // --- mapToJson error branches ---

    @Test
    public void mapToJsonWithNullKeyThrowsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put(null, "v");

        assertThrows(IllegalArgumentException.class, () -> JsonUtils.mapToJson(map));
    }

    // --- collectionToJson ---

    @Test
    public void collectionToJsonWithPrimitivesAndNull() throws Exception {
        List<Object> data = Arrays.asList("s", 1, true, null);

        JSONArray array = JsonUtils.collectionToJson(data);

        assertEquals(4, array.length());
        assertEquals("s", array.get(0));
        assertEquals(1, array.get(1));
        assertEquals(true, array.get(2));
        assertEquals(JSONObject.NULL, array.get(3));
    }

    @Test
    public void collectionToJsonRecursivelyConvertsNestedMapAndCollection() throws Exception {
        Collection<Object> data = Arrays.asList(Collections.singletonMap("k", 1), Arrays.asList("a", "b"));

        JSONArray array = JsonUtils.collectionToJson(data);

        assertEquals(2, array.length());
        assertEquals(1, array.getJSONObject(0).getInt("k"));
        assertEquals("a", array.getJSONArray(1).getString(0));
        assertEquals("b", array.getJSONArray(1).getString(1));
    }

    // --- wrap branches via mapToJson ---

    @Test
    public void mapToJsonStoresNullValueAsJsonNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("n", null);

        JSONObject json = JsonUtils.mapToJson(map);

        assertTrue(json.isNull("n"));
    }

    @Test
    public void mapToJsonPassesThroughJsonObjectAndJsonArray() throws Exception {
        JSONObject nestedObj = new JSONObject().put("x", 1);
        JSONArray nestedArr = new JSONArray().put("y");
        Map<String, Object> map = new HashMap<>();
        map.put("o", nestedObj);
        map.put("a", nestedArr);

        JSONObject json = JsonUtils.mapToJson(map);

        assertSame(nestedObj, json.get("o"));
        assertSame(nestedArr, json.get("a"));
        assertEquals(1, json.getJSONObject("o").getInt("x"));
        assertEquals("y", json.getJSONArray("a").getString(0));
    }

    @Test
    public void mapToJsonUnsupportedNonJavaTypeFallsBackToJsonNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("o", new CustomObject());

        JSONObject json = JsonUtils.mapToJson(map);

        assertTrue(json.isNull("o"));
    }

    @Test
    public void mapToJsonJavaDateSerializesAsToString() throws Exception {
        Date date = new Date(0);
        Map<String, Object> map = new HashMap<>();
        map.put("d", date);

        JSONObject json = JsonUtils.mapToJson(map);

        assertEquals(date.toString(), json.getString("d"));
    }

    // --- bundleToJson ---

    @Test
    public void bundleToJsonWithStringAndIntValues() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("s", "v");
        bundle.putInt("i", 7);

        JSONObject json = JsonUtils.bundleToJson(bundle);

        assertEquals("v", json.getString("s"));
        assertEquals(7, json.getInt("i"));
    }

    @Test
    public void bundleToJsonPrimitiveArraysAreFormattedViaArraysToString() throws Exception {
        char[] charArr = new char[] {'a', 'b'};
        float[] floatArr = new float[] {1.0f, 2.0f};
        double[] doubleArr = new double[] {1.5, 2.5};
        int[] intArr = new int[] {1, 2};
        long[] longArr = new long[] {1L, 2L};
        byte[] byteArr = new byte[] {1, 2};

        Bundle bundle = new Bundle();
        bundle.putCharArray("char", charArr);
        bundle.putFloatArray("float", floatArr);
        bundle.putDoubleArray("double", doubleArr);
        bundle.putIntArray("int", intArr);
        bundle.putLongArray("long", longArr);
        bundle.putByteArray("byte", byteArr);

        JSONObject json = JsonUtils.bundleToJson(bundle);

        Object[][] cases = new Object[][] {
            {"char", Arrays.toString(charArr)},
            {"float", Arrays.toString(floatArr)},
            {"double", Arrays.toString(doubleArr)},
            {"int", Arrays.toString(intArr)},
            {"long", Arrays.toString(longArr)},
            {"byte", Arrays.toString(byteArr)},
        };
        for (Object[] c : cases) {
            String key = (String) c[0];
            String expected = (String) c[1];
            assertEquals("primitive array branch " + key, expected, json.getString(key));
        }
    }

    @Test
    public void bundleToJsonObjectArrayFormattedViaDeepToString() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putStringArray("a", new String[] {"x", "y"});

        JSONObject json = JsonUtils.bundleToJson(bundle);

        assertEquals("[x, y]", json.getString("a"));
    }

    // --- bundleToJsonWithUserData ---

    @Test
    public void bundleToJsonWithUserDataUnpacksObjectStringUnderUserdata() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("u", "{\"name\":\"alice\"}");

        JSONObject json = JsonUtils.bundleToJsonWithUserData(bundle);

        assertTrue(json.has("userdata"));
        assertEquals("alice", json.getJSONObject("userdata").getString("name"));
        // original "u" key is also preserved per implementation
        assertEquals("{\"name\":\"alice\"}", json.getString("u"));
    }

    @Test
    public void bundleToJsonWithUserDataUnpacksArrayStringUnderUserdata() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("u", "[1,2,3]");

        JSONObject json = JsonUtils.bundleToJsonWithUserData(bundle);

        assertTrue(json.has("userdata"));
        assertEquals(1, json.getJSONArray("userdata").getInt(0));
        assertEquals(2, json.getJSONArray("userdata").getInt(1));
        assertEquals(3, json.getJSONArray("userdata").getInt(2));
    }

    // --- jsonStringToBundle ---

    @Test
    public void jsonStringToBundleConvertsPrimitives() {
        Bundle bundle = JsonUtils.jsonStringToBundle("{\"s\":\"v\",\"i\":5,\"b\":true,\"l\":1308582386878298}");

        assertEquals("v", bundle.getString("s"));
        assertEquals(5, bundle.getInt("i"));
        assertTrue(bundle.getBoolean("b"));
        assertEquals(1308582386878298L, bundle.getLong("l"));
    }

    @Test
    public void jsonStringToBundleInvalidJsonReturnsEmptyBundle() {
        Bundle bundle = JsonUtils.jsonStringToBundle("not a json");

        assertTrue(bundle.isEmpty());
    }

    @Test
    public void jsonStringToBundleConvertSubobjectsToStringStoresNestedAsString() {
        Bundle bundle = JsonUtils.jsonStringToBundle("{\"nested\":{\"k\":1}}", true);

        String nested = bundle.getString("nested");
        assertThat(nested, containsString("k"));
        assertThat(nested, containsString("1"));
    }

    @Test
    public void jsonStringToBundleConvertSubobjectsFalseStoresNestedAsBundle() {
        Bundle bundle = JsonUtils.jsonStringToBundle("{\"nested\":{\"k\":\"v\"}}", false);

        Bundle nested = bundle.getBundle("nested");
        assertThat(nested, is(instanceOf(Bundle.class)));
        assertEquals("v", nested.getString("k"));
    }

    @Test
    public void jsonStringToBundleArrayValueThrowsIllegalArgumentException() {
        // Documents existing behavior: mapToBundle has no branch for ArrayList,
        // so IllegalArgumentException escapes the JSONException catch in jsonStringToBundle.
        assertThrows(IllegalArgumentException.class, () -> JsonUtils.jsonStringToBundle("{\"arr\":[1,2]}", false));
    }

    // --- fixtures ---

    private static final class CustomObject {
        @Override
        public String toString() {
            return "custom";
        }
    }
}
