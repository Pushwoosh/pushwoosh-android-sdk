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

package com.pushwoosh.internal.platform.prefs.migration;

import static android.content.Context.MODE_PRIVATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import com.pushwoosh.internal.platform.prefs.PrefsProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class MigrationSchemeTest {

    private static final String PREFS_NAME = "migration_scheme_test_prefs";

    @Mock
    private PrefsProvider oldProvider;

    @Mock
    private PrefsProvider newProvider;

    private AutoCloseable mocks;
    private SharedPreferences oldPrefs;
    private SharedPreferences newPrefs;
    private MigrationScheme migrationScheme;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        oldPrefs = RuntimeEnvironment.getApplication().getSharedPreferences("old_" + PREFS_NAME, MODE_PRIVATE);
        newPrefs = RuntimeEnvironment.getApplication().getSharedPreferences("new_" + PREFS_NAME, MODE_PRIVATE);
        oldPrefs.edit().clear().commit();
        newPrefs.edit().clear().commit();

        when(oldProvider.providePrefs(PREFS_NAME)).thenReturn(oldPrefs);
        when(newProvider.providePrefs(PREFS_NAME)).thenReturn(newPrefs);

        migrationScheme = new MigrationScheme(PREFS_NAME);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // Verifies that put() reads values of all supported types from the old provider and implementScheme() writes them
    // into the new provider.
    @Test
    public void put_readsAllTypesFromOldProvider_andImplementSchemeWritesThemToNewProvider() {
        oldPrefs.edit()
                .putString("string_key", "hello")
                .putBoolean("boolean_key", true)
                .putLong("long_key", 123456789L)
                .putInt("int_key", 42)
                .commit();

        migrationScheme.put(oldProvider, MigrationScheme.AvailableType.STRING, "string_key");
        migrationScheme.put(oldProvider, MigrationScheme.AvailableType.BOOLEAN, "boolean_key");
        migrationScheme.put(oldProvider, MigrationScheme.AvailableType.LONG, "long_key");
        migrationScheme.put(oldProvider, MigrationScheme.AvailableType.INT, "int_key");

        migrationScheme.implementScheme(newProvider);

        assertEquals("hello", newPrefs.getString("string_key", ""));
        assertTrue(newPrefs.getBoolean("boolean_key", false));
        assertEquals(123456789L, newPrefs.getLong("long_key", 0L));
        assertEquals(42, newPrefs.getInt("int_key", 0));
    }

    // Verifies that put skips keys missing in the old prefs.
    @Test
    public void put_keyMissingInOldPrefs_doesNotAddKeyToScheme() {
        migrationScheme.put(oldProvider, MigrationScheme.AvailableType.STRING, "missing");
        migrationScheme.implementScheme(newProvider);

        assertFalse(newPrefs.contains("missing"));
    }
}
