package com.pushwoosh.featureflags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class FeatureFlagsParserTest {

    private static JSONArray contractCases() throws Exception {
        try (InputStream in = FeatureFlagsParserTest.class
                .getClassLoader()
                .getResourceAsStream("featureflags/feature_flags_contract.json")) {
            String text = new Scanner(in, "UTF-8").useDelimiter("\\A").next();
            return new JSONObject(text).getJSONArray("cases");
        }
    }

    private static JSONObject want(String caseName) throws Exception {
        JSONArray cases = contractCases();
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            if (caseName.equals(c.getString("name"))) {
                return c.getJSONObject("want");
            }
        }
        throw new AssertionError("no fixture case " + caseName);
    }

    private static FeatureFlag parseOne(String key, JSONObject flagJson) throws JSONException {
        return FeatureFlagsParser.parse(new JSONObject().put(key, flagJson)).get(key);
    }

    @Test
    public void parse_everyContractCase_matchesWant() throws Exception {
        JSONArray cases = contractCases();
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            JSONObject want = c.getJSONObject("want");
            FeatureFlag flag = parseOne("k", want);
            String name = c.getString("name");
            assertEquals(name, want.getBoolean("enabled"), flag.isEnabled());
            assertEquals(name, want.getString("rule_id"), flag.getRuleId());
            JSONObject props = want.getJSONObject("properties");
            Iterator<String> keys = props.keys();
            while (keys.hasNext()) {
                String prop = keys.next();
                Object value = props.get(prop);
                if (value instanceof String) {
                    assertEquals(name, value, flag.getString(prop, "__default__"));
                } else if (value instanceof Number) {
                    assertEquals(name, ((Number) value).doubleValue(), flag.getNumber(prop, -12345.0), 0.0);
                } else {
                    throw new AssertionError("unexpected fixture property type in " + name);
                }
            }
        }
    }

    @Test
    public void parse_overrideApplied() throws Exception {
        FeatureFlag flag =
                parseOne("new_checkout", want("everyone rule at 100% enables the flag and applies overrides"));
        assertTrue(flag.isEnabled());
        assertEquals("new_checkout", flag.getKey());
        assertEquals("r_00000001", flag.getRuleId());
        assertEquals("green", flag.getString("color", "x"));
        assertEquals(3.0, flag.getNumber("limit", -1.0), 0.0);
    }

    @Test
    public void parse_killedFlag_hasEmptyRuleIdAndDefaults() throws Exception {
        FeatureFlag flag = parseOne("beta", want("killed flag is off with no rule and defaults"));
        assertFalse(flag.isEnabled());
        assertEquals("", flag.getRuleId());
        assertEquals("blue", flag.getString("color", "x"));
    }

    @Test
    public void parse_defaultRule() throws Exception {
        FeatureFlag flag = parseOne("beta", want("no rule audience matched gives the default rule"));
        assertEquals("default", flag.getRuleId());
    }

    @Test
    public void getter_removedProperty_returnsDefault() throws Exception {
        FeatureFlag flag = parseOne("beta", want("override of a removed property is dropped"));
        assertEquals(7.0, flag.getNumber("gone", 7.0), 0.0);
        assertEquals("green", flag.getString("color", "x"));
    }

    @Test
    public void getter_typeMismatch_returnsDefault() throws Exception {
        FeatureFlag flag = parseOne(
                "f",
                new JSONObject(
                        "{\"enabled\":true,\"rule_id\":\"r\",\"properties\":{\"s\":\"3\",\"n\":3,\"b\":true,\"o\":{},\"a\":[],\"z\":null}}"));
        assertEquals(1.5, flag.getNumber("s", 1.5), 0.0);
        assertEquals("d", flag.getString("n", "d"));
        assertFalse(flag.getBool("n", false));
        assertEquals("d", flag.getString("b", "d"));
        assertTrue(flag.getBool("b", false));
        assertEquals(null, flag.getJsonObject("a", null));
        assertEquals(null, flag.getJsonArray("o", null));
        assertEquals("d", flag.getString("z", "d"));
    }

    @Test
    public void getter_json_returnsCopies() throws Exception {
        JSONObject source =
                new JSONObject("{\"enabled\":true,\"rule_id\":\"r\",\"properties\":{\"o\":{\"a\":1},\"arr\":[1,2]}}");
        FeatureFlag flag = parseOne("f", source);

        source.getJSONObject("properties").getJSONObject("o").put("a", 99);
        flag.getJsonObject("o", null).put("a", 42);
        flag.getJsonArray("arr", null).put(3);

        assertEquals(1, flag.getJsonObject("o", null).getInt("a"));
        assertEquals(2, flag.getJsonArray("arr", null).length());
    }

    @Test
    public void parse_emptyFlags_isEmptyMap() throws Exception {
        assertTrue(FeatureFlagsParser.parse(new JSONObject()).isEmpty());
    }

    @Test(expected = JSONException.class)
    public void parse_flagIsNotAnObject_throws() throws Exception {
        FeatureFlagsParser.parse(new JSONObject("{\"f\":\"oops\"}"));
    }

    @Test
    public void equals_comparesEnabledRuleAndPropertiesByJsonValue() throws Exception {
        Map<String, FeatureFlag> a = FeatureFlagsParser.parse(new JSONObject(
                "{\"f\":{\"enabled\":true,\"rule_id\":\"r\",\"properties\":{\"n\":3,\"o\":{\"x\":[1,\"y\"]}}}}"));
        Map<String, FeatureFlag> same = FeatureFlagsParser.parse(new JSONObject(
                "{\"f\":{\"enabled\":true,\"rule_id\":\"r\",\"properties\":{\"o\":{\"x\":[1,\"y\"]},\"n\":3.0}}}"));
        Map<String, FeatureFlag> otherProp = FeatureFlagsParser.parse(new JSONObject(
                "{\"f\":{\"enabled\":true,\"rule_id\":\"r\",\"properties\":{\"n\":4,\"o\":{\"x\":[1,\"y\"]}}}}"));
        Map<String, FeatureFlag> otherRule = FeatureFlagsParser.parse(new JSONObject(
                "{\"f\":{\"enabled\":true,\"rule_id\":\"r2\",\"properties\":{\"n\":3,\"o\":{\"x\":[1,\"y\"]}}}}"));
        assertEquals(a, same);
        assertNotEquals(a, otherProp);
        assertNotEquals(a, otherRule);
    }
}
