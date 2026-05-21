/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.inapp.storage;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class InAppDbHelperTest {
    private InAppDbHelper inAppDbHelper;

    private Resource resource1;
    private Resource resource2;

    @Before
    public void setUp() throws Exception {
        inAppDbHelper = new InAppDbHelper(RuntimeEnvironment.application);

        resource1 = new Resource("code1", "url1", "hash1", 1L, InAppLayout.DIALOG, null, false, 3);
        resource2 = new Resource("code2", "url2", "hash2", 2L, InAppLayout.BOTTOM, null, true, 2);

        List<Resource> resourceList = Arrays.asList(resource1, resource2);

        inAppDbHelper.saveOrUpdateResources(resourceList);
    }

    @After
    public void tearDown() throws Exception {
        inAppDbHelper.close();
    }

    @Test
    public void getResource() {
        Resource resource = inAppDbHelper.getResource("code2");
        Assert.assertEquals(resource2, resource);
        resource = inAppDbHelper.getResource("code1");
        Assert.assertEquals(resource1, resource);
    }

    // Verifies that onUpgrade from v1 to v4 adds priority, required, gdpr and businessCase columns.
    @Test
    public void onUpgrade_fromV1ToV4_addsAllFourColumns() {
        SQLiteDatabase db = inAppDbHelper.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS inApps");
        db.execSQL("create table inApps ("
                + "code text primary key, "
                + "url text, "
                + "folder text, "
                + "layout text, "
                + "updated integer"
                + ");");

        inAppDbHelper.onUpgrade(db, 1, 4);

        Set<String> columns = getColumnNames(db, "inApps");
        Assert.assertTrue("priority column missing", columns.contains("priority"));
        Assert.assertTrue("required column missing", columns.contains("required"));
        Assert.assertTrue("gdpr column missing", columns.contains("gdpr"));
        Assert.assertTrue("businessCase column missing", columns.contains("businessCase"));
        db.close();
    }

    // Verifies that onUpgrade from v2 to v4 adds only gdpr and businessCase columns.
    @Test
    public void onUpgrade_fromV2ToV4_addsOnlyGdprAndBusinessCase() {
        SQLiteDatabase db = inAppDbHelper.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS inApps");
        db.execSQL("create table inApps ("
                + "code text primary key, "
                + "url text, "
                + "folder text, "
                + "layout text, "
                + "updated integer, "
                + "priority integer default 0, "
                + "required integer default 0"
                + ");");

        inAppDbHelper.onUpgrade(db, 2, 4);

        Set<String> columns = getColumnNames(db, "inApps");
        Assert.assertTrue("priority must remain", columns.contains("priority"));
        Assert.assertTrue("required must remain", columns.contains("required"));
        Assert.assertTrue("gdpr must be added", columns.contains("gdpr"));
        Assert.assertTrue("businessCase must be added", columns.contains("businessCase"));
        db.close();
    }

    // Verifies that onUpgrade from v3 to v4 adds only businessCase column.
    @Test
    public void onUpgrade_fromV3ToV4_addsOnlyBusinessCase() {
        SQLiteDatabase db = inAppDbHelper.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS inApps");
        db.execSQL("create table inApps ("
                + "code text primary key, "
                + "url text, "
                + "folder text, "
                + "layout text, "
                + "updated integer, "
                + "priority integer default 0, "
                + "required integer default 0, "
                + "gdpr text"
                + ");");

        inAppDbHelper.onUpgrade(db, 3, 4);

        Set<String> columns = getColumnNames(db, "inApps");
        Assert.assertTrue("gdpr must remain", columns.contains("gdpr"));
        Assert.assertTrue("businessCase must be added", columns.contains("businessCase"));
        db.close();
    }

    // Verifies that saveOrUpdateResources updates an existing resource and returns its code.
    @Test
    public void saveOrUpdateResources_updatesExistingResource_returnsCodeAndPersistsNewFields() {
        Resource updated = new Resource("code1", "url1", "hash1", 1L, InAppLayout.DIALOG, null, false, 99);

        List<String> result = inAppDbHelper.saveOrUpdateResources(Arrays.asList(updated));

        Assert.assertTrue("updated code1 must be reported", result.contains("code1"));
        Resource stored = inAppDbHelper.getResource("code1");
        Assert.assertEquals(99, stored.getPriority());
    }

    // Verifies that saveOrUpdateResources skips identical existing resource and does not report its code.
    @Test
    public void saveOrUpdateResources_identicalResource_skipsAndReturnsEmpty() {
        List<String> result = inAppDbHelper.saveOrUpdateResources(Arrays.asList(resource1));

        Assert.assertFalse("identical resource1 must not be in updated list", result.contains("code1"));
    }

    private Set<String> getColumnNames(SQLiteDatabase db, String table) {
        Set<String> names = new HashSet<>();
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        try {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                names.add(cursor.getString(nameIndex));
            }
        } finally {
            cursor.close();
        }
        return names;
    }
}
