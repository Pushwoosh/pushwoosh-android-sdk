package com.pushwoosh.inapp;

import java.util.HashMap;

import com.pushwoosh.inapp.mapper.InAppTagFormatModifier;

import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class InAppTagFormatModifierTest {
	@Test
	public void testFormat() throws Exception {
		assertEquals("this is test valuE", InAppTagFormatModifier.format("this is test valuE", "unsupportedModifier"));
		assertEquals("this is test valuE", InAppTagFormatModifier.format("this is test valuE", "text"));

		// String modifiers
		assertEquals("this is test valuE", InAppTagFormatModifier.format("this is test valuE", "regular"));
		assertEquals("this is test value", InAppTagFormatModifier.format("this is test valuE", "lowercase"));
		assertEquals("THIS IS TEST VALUE", InAppTagFormatModifier.format("this is test valuE", "UPPERCASE"));
		assertEquals("This Is Test  ValuE", InAppTagFormatModifier.format("this is test  valuE", "CapitalizeAllFirst"));
		assertEquals("This is test valuE", InAppTagFormatModifier.format("this is test valuE", "CapitalizeFirst"));
		assertEquals("", InAppTagFormatModifier.format("", "CapitalizeFirst"));

		// Int/Date modifiers
		assertEquals("42", InAppTagFormatModifier.format("42", "unsupportedModifier"));
		assertEquals("42", InAppTagFormatModifier.format("42", "regular"));

		assertEquals("$1234.56", InAppTagFormatModifier.format("123456", "cent"));
		assertEquals("$1.23", InAppTagFormatModifier.format("123", "cent"));
		assertEquals("$.42", InAppTagFormatModifier.format("42", "cent"));
		assertEquals("$.01", InAppTagFormatModifier.format("1", "cent"));
		assertEquals("$.00", InAppTagFormatModifier.format("", "cent"));

		assertEquals("$0", InAppTagFormatModifier.format("", "dollar"));
		assertEquals("$1", InAppTagFormatModifier.format("1", "dollar"));
		assertEquals("$1,234", InAppTagFormatModifier.format("1234", "dollar"));
		assertEquals("$12,345", InAppTagFormatModifier.format("12345", "dollar"));
		assertEquals("$123,456", InAppTagFormatModifier.format("123456", "dollar"));
		assertEquals("$9,876,543,210", InAppTagFormatModifier.format("9876543210", "dollar"));

		assertEquals("", InAppTagFormatModifier.format("", "comma"));
		assertEquals("1", InAppTagFormatModifier.format("1", "comma"));
		assertEquals("1,234", InAppTagFormatModifier.format("1234", "comma"));
		assertEquals("12,345", InAppTagFormatModifier.format("12345", "comma"));
		assertEquals("123,456", InAppTagFormatModifier.format("123456", "comma"));
		assertEquals("9,876,543,210", InAppTagFormatModifier.format("9876543210", "comma"));

		assertEquals("€0", InAppTagFormatModifier.format("", "euro"));
		assertEquals("€1", InAppTagFormatModifier.format("1", "euro"));
		assertEquals("€1,234", InAppTagFormatModifier.format("1234", "euro"));
		assertEquals("€12,345", InAppTagFormatModifier.format("12345", "euro"));
		assertEquals("€123,456", InAppTagFormatModifier.format("123456", "euro"));
		assertEquals("€9,876,543,210", InAppTagFormatModifier.format("9876543210", "euro"));

		assertEquals("¥0", InAppTagFormatModifier.format("", "jpy"));
		assertEquals("¥1", InAppTagFormatModifier.format("1", "jpy"));
		assertEquals("¥1,234", InAppTagFormatModifier.format("1234", "jpy"));
		assertEquals("¥12,345", InAppTagFormatModifier.format("12345", "jpy"));
		assertEquals("¥123,456", InAppTagFormatModifier.format("123456", "jpy"));
		assertEquals("¥9,876,543,210", InAppTagFormatModifier.format("9876543210", "jpy"));

		assertEquals("₤0", InAppTagFormatModifier.format("", "lira"));
		assertEquals("₤1", InAppTagFormatModifier.format("1", "lira"));
		assertEquals("₤1,234", InAppTagFormatModifier.format("1234", "lira"));
		assertEquals("₤12,345", InAppTagFormatModifier.format("12345", "lira"));
		assertEquals("₤123,456", InAppTagFormatModifier.format("123456", "lira"));
		assertEquals("₤9,876,543,210", InAppTagFormatModifier.format("9876543210", "lira"));

		assertEquals("Apr-25-14", InAppTagFormatModifier.format("1398407400", "M-d-y")); // 2014-04-25 06:30
		assertEquals("04-25-14", InAppTagFormatModifier.format("1398407400", "m-d-y"));
		assertEquals("Apr 25 14", InAppTagFormatModifier.format("1398407400", "M d y"));
		assertEquals("Apr 25 2014", InAppTagFormatModifier.format("1398407400", "M d Y"));
		assertEquals("Friday", InAppTagFormatModifier.format("1398407400", "l"));
		assertEquals("Apr 25", InAppTagFormatModifier.format("1398407400", "M d"));
		assertEquals("06:30", InAppTagFormatModifier.format("1398407400", "H:i"));
		assertEquals("04-25-14 06:30", InAppTagFormatModifier.format("1398407400", "m-d-y H:i"));

		// Stress
		assertEquals("", InAppTagFormatModifier.format(null, "regular"));
		assertEquals("", InAppTagFormatModifier.format("any text", null));
	}

	@Test
	public void testConvertGeoTags() throws Exception {
		HashMap<String, Object> tags1 = new HashMap<String, Object>();
		tags1.put("customTag", "tagVal");
		tags1.put("Country", "in");

		HashMap<String, Object> expectedTags1 = new HashMap<String, Object>();
		expectedTags1.put("customTag", "tagVal");
		expectedTags1.put("Country", "India");

		InAppTagFormatModifier.convertGeoTags(tags1);

		assertEquals(expectedTags1, tags1);


		HashMap<String, Object> tags2 = new HashMap<String, Object>();
		tags2.put("customTag", "tagVal");
		tags2.put("Country", "No such country");

		HashMap<String, Object> expectedTags2 = new HashMap<String, Object>();
		expectedTags2.put("customTag", "tagVal");

		InAppTagFormatModifier.convertGeoTags(tags2);

		assertEquals(expectedTags2, tags2);


		HashMap<String, Object> tags3 = new HashMap<String, Object>();
		tags3.put("customTag", "tagVal");
		tags3.put("Country", "in");
		tags3.put("City", "in, trivandrum");

		HashMap<String, Object> expectedTags3 = new HashMap<String, Object>();
		expectedTags3.put("customTag", "tagVal");
		expectedTags3.put("Country", "India");
		expectedTags3.put("City", "trivandrum");

		InAppTagFormatModifier.convertGeoTags(tags3);

		assertEquals(expectedTags3, tags3);


		HashMap<String, Object> tags4 = new HashMap<String, Object>();
		tags4.put("customTag", "tagVal");
		tags4.put("Country", "in");
		tags4.put("City", "trivandrum");

		HashMap<String, Object> expectedTags4 = new HashMap<String, Object>();
		expectedTags4.put("customTag", "tagVal");
		expectedTags4.put("Country", "India");
		expectedTags4.put("City", "trivandrum");

		InAppTagFormatModifier.convertGeoTags(tags4);

		assertEquals(expectedTags4, tags4);


		HashMap<String, Object> tags5 = new HashMap<String, Object>();
		tags5.put("customTag", "tagVal");
		tags5.put("Country", "in");
		tags5.put("City", "");

		HashMap<String, Object> expectedTags5 = new HashMap<String, Object>();
		expectedTags5.put("customTag", "tagVal");
		expectedTags5.put("Country", "India");
		expectedTags5.put("City", "");

		InAppTagFormatModifier.convertGeoTags(tags5);

		assertEquals(expectedTags5, tags5);

		HashMap<String, Object> tags6 = new HashMap<String, Object>();
		tags6.put("customTag", "tagVal");
		tags6.put("Country", "MX");
		tags6.put("City", "MX, Zamora Municipality");

		HashMap<String, Object> expectedTags6 = new HashMap<String, Object>();
		expectedTags6.put("customTag", "tagVal");
		expectedTags6.put("Country", "Mexico");
		expectedTags6.put("City", "Zamora Municipality");

		InAppTagFormatModifier.convertGeoTags(tags6);

		assertEquals(expectedTags6, tags6);
	}
}
