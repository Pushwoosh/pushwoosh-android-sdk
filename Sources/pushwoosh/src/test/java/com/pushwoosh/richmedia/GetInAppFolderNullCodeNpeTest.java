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

package com.pushwoosh.richmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.pushwoosh.inapp.storage.ContextInAppFolderProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.io.File;

// Regression guard for crash-getinappfolder-null-code (fix #12): a null code reaching
// ContextInAppFolderProvider.getInAppFolder no longer NPEs at `new File(inAppsDir, code)`.
// The fix folds `code == null` into the existing `context == null` guard (:45), so a null code
// returns null exactly like a null context; the delegators (isInAppDownloaded/getConfigFile/
// getInAppHtmlFile) inherit graceful handling.
//
// Null code is only reachable from a corrupted inAppDb.db row (NULL in `code text primary key`,
// which SQLite permits for a non-NOT-NULL text PK); production Resource sources never produce it.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class GetInAppFolderNullCodeNpeTest {

    private ContextInAppFolderProvider folderProvider;

    @Before
    public void setUp() {
        // Real prod provider over a real (Robolectric) context, so getDir(:49) returns non-null and
        // the NPE lands on `code` at :50 inside the real ContextInAppFolderProvider frame.
        folderProvider = new ContextInAppFolderProvider(RuntimeEnvironment.getApplication());
    }

    @Test
    public void getInAppFolderNull_returnsNull() {
        // Verifies that getInAppFolder returns null on a null code instead of NPEing at new File(dir, null).
        // code==null hits the folded `context == null || code == null` guard (:45), mirroring null-context.
        assertNull(folderProvider.getInAppFolder(null));
    }

    @Test
    public void isInAppDownloadedNull_returnsFalse() {
        // Verifies that isInAppDownloaded reports false (not-downloaded) on a null code rather than NPEing.
        // isInAppDownloaded(null) -> getInAppFolder(null)==null -> `inappFolder != null && ...` -> false.
        assertFalse(folderProvider.isInAppDownloaded(null));
    }

    @Test
    public void negativeControl_nonNullCode_buildsFile() {
        // Verifies that a non-null code does NOT hit the new guard — it builds a File named after the code.
        // Discriminator: proves the guard is scoped to null and does not swallow valid codes.
        File folder = folderProvider.getInAppFolder("some-code");
        assertNotNull(folder);
        assertEquals("some-code", folder.getName());
    }
}
