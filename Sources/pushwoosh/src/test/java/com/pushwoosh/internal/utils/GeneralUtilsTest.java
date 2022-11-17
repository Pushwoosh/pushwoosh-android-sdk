package com.pushwoosh.internal.utils;

import android.graphics.Color;

import com.pushwoosh.BuildConfig;
import com.pushwoosh.internal.platform.utils.GeneralUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(constants = BuildConfig.class)
public class GeneralUtilsTest {

	@Test(expected = IllegalArgumentException.class)
	public void testCheckNotNullOrEmpty_Null() throws Exception {
		GeneralUtils.checkNotNullOrEmpty(null, "Unit test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCheckNotNullOrEmpty_Empty() throws Exception {
		GeneralUtils.checkNotNullOrEmpty("", "Unit test");
	}

	@Test
	public void testCheckNotNullOrEmpty_NotNull() throws Exception {
		GeneralUtils.checkNotNullOrEmpty("Banana", "Unit test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCheckNotNull_Null() throws Exception {
		GeneralUtils.checkNotNull(null, "Unit test");
	}

	@Test
	public void testCheckNotNull_NotNull() throws Exception {
		GeneralUtils.checkNotNull("Not null", "Unit test");
	}

	@Test
	public void testParseColor() throws Exception {
		assertEquals(GeneralUtils.parseColor("#FF0000"), Color.RED); // #rrggbb
		assertEquals(GeneralUtils.parseColor("#FFFF0000"), Color.RED); // #aarrggbb
		assertEquals(GeneralUtils.parseColor("#F00"), Color.RED); // #rgb
		assertEquals(GeneralUtils.parseColor("#FF00"), Color.RED); // #argb
		assertEquals(GeneralUtils.parseColor("0,255,0,255"), Color.GREEN);  // r,g,b,a
	}
}