package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableStringBuilder;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class PrefsUtilsTest {

    private SharedPreferences prefs;

    @Before
    public void setUp() {
        prefs = ApplicationProvider.getApplicationContext()
                .getSharedPreferences("PrefsUtilsTest", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    @Test
    public void putBundle_flatBundleWithScalarTypes_roundTripsAllValues() {
        Bundle input = new Bundle();
        input.putInt("i", 7);
        input.putLong("l", 123456789012L);
        input.putBoolean("b", true);
        input.putString("s", "hello");

        SharedPreferences.Editor editor = prefs.edit();
        PrefsUtils.putBundle(editor, "root", input);
        editor.apply();

        Bundle output = PrefsUtils.getBundle(prefs, "root");

        assertEquals(4, output.size());
        assertEquals(7, output.getInt("i"));
        assertEquals(123456789012L, output.getLong("l"));
        assertTrue(output.getBoolean("b"));
        assertEquals("hello", output.getString("s"));
    }

    @Test
    public void putBundle_charSequenceValue_isPersistedAndReadAsString() {
        Bundle input = new Bundle();
        SpannableStringBuilder cs = new SpannableStringBuilder("hello");
        input.putCharSequence("cs", cs);

        SharedPreferences.Editor editor = prefs.edit();
        PrefsUtils.putBundle(editor, "root", input);
        editor.apply();

        Bundle output = PrefsUtils.getBundle(prefs, "root");

        Object value = output.get("cs");
        assertNotNull(value);
        assertEquals(String.class, value.getClass());
        assertEquals("hello", value);
    }

    @Test
    public void putBundle_nestedSubBundle_isReconstructedRecursively() {
        Bundle child = new Bundle();
        child.putInt("n", 42);
        child.putString("s", "x");

        Bundle parent = new Bundle();
        parent.putBundle("child", child);

        SharedPreferences.Editor editor = prefs.edit();
        PrefsUtils.putBundle(editor, "root", parent);
        editor.apply();

        Bundle output = PrefsUtils.getBundle(prefs, "root");

        Bundle outChild = output.getBundle("child");
        assertNotNull(outChild);
        assertEquals(42, outChild.getInt("n"));
        assertEquals("x", outChild.getString("s"));
    }

    @Test
    public void getBundle_unrelatedKeysInPrefs_areIgnored() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("other$##$x", 1);
        editor.putString("plainKey", "ignored");

        Bundle input = new Bundle();
        input.putString("k", "v");
        PrefsUtils.putBundle(editor, "root", input);
        editor.apply();

        Bundle output = PrefsUtils.getBundle(prefs, "root");

        assertFalse(output.containsKey("x"));
        assertFalse(output.containsKey("plainKey"));
        assertEquals("v", output.getString("k"));
        assertEquals(1, output.size());
    }

    @Test
    public void putBundle_nullValueInInput_removesExistingPrefEntry() {
        prefs.edit().putString("root$##$k", "old").apply();
        assertTrue(prefs.contains("root$##$k"));

        Bundle input = new Bundle();
        input.putString("k", null);

        SharedPreferences.Editor editor = prefs.edit();
        PrefsUtils.putBundle(editor, "root", input);
        editor.apply();

        assertFalse(prefs.contains("root$##$k"));
        Bundle output = PrefsUtils.getBundle(prefs, "root");
        assertFalse(output.containsKey("k"));
    }

    @Test
    public void putBundle_unsupportedValueType_isSilentlySkipped() {
        Bundle input = new Bundle();
        input.putFloat("f", 1.5f);
        input.putString("s", "ok");

        SharedPreferences.Editor editor = prefs.edit();
        PrefsUtils.putBundle(editor, "root", input);
        editor.apply();

        Bundle output = PrefsUtils.getBundle(prefs, "root");

        assertFalse(output.containsKey("f"));
        assertEquals("ok", output.getString("s"));
        assertEquals(1, output.size());
    }

    @Test
    public void getBundle_keyWithNoMatchingEntries_returnsEmptyBundle() {
        Bundle output = PrefsUtils.getBundle(prefs, "missing");

        assertNotNull(output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void putBundle_sharedPrefixKeys_areIsolatedByKeySeparator() {
        Bundle b1 = new Bundle();
        b1.putInt("id", 1);
        Bundle b2 = new Bundle();
        b2.putInt("id", 2);

        SharedPreferences.Editor editor = prefs.edit();
        PrefsUtils.putBundle(editor, "user", b1);
        PrefsUtils.putBundle(editor, "userExtra", b2);
        editor.apply();

        Bundle outUser = PrefsUtils.getBundle(prefs, "user");
        Bundle outUserExtra = PrefsUtils.getBundle(prefs, "userExtra");

        assertEquals(1, outUser.size());
        assertEquals(1, outUser.getInt("id"));
        assertEquals(1, outUserExtra.size());
        assertEquals(2, outUserExtra.getInt("id"));
    }
}
