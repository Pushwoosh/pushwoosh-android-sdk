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

import static com.pushwoosh.inapp.view.strategy.model.ResourceType.IN_APP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.ContextInAppFolderProvider;
import com.pushwoosh.inapp.view.strategy.ResourceViewStrategyFactory;
import com.pushwoosh.inapp.view.strategy.model.ResourceType;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.io.File;
import java.lang.reflect.Constructor;

// Regression guard for crash-getinappfolder-null-code (fix #12): a null code reaching
// ContextInAppFolderProvider.getInAppFolder no longer NPEs at `new File(inAppsDir, code)`.
// The fix folds `code == null` into the existing `context == null` guard (:45), so a null code
// returns null exactly like a null context; the delegators (isInAppDownloaded/getConfigFile/
// getInAppHtmlFile) and the RichMediaController.isCanceled reach path inherit graceful handling.
//
// Null code is only reachable from a corrupted inAppDb.db row (NULL in `code text primary key`,
// which SQLite permits for a non-NOT-NULL text PK); production Resource sources never produce it.
// The null-code Resource is built directly via the public ctor to stand in for that corrupted row,
// and the IN_APP wrapper is assembled via the private ResourceWrapper ctor to bypass
// Builder.setResource's isInApp() pre-deref and exercise the real getInAppFolder crash point.
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

    private static Resource nullCodeInAppResource() {
        // Public Resource ctor assigns mCode directly with no guard; required=false so isCanceled:226
        // takes the !isRequired() branch into isInAppDownloaded.
        return new Resource(
                /* code */ null,
                /* url */ "https://example.com/inapp.zip",
                /* hash */ null,
                /* updated */ 0L,
                InAppLayout.FULLSCREEN,
                /* tags */ null,
                /* required */ false,
                /* priority */ 0);
    }

    private static ResourceWrapper inAppWrapper(Resource resource) {
        // Private ctor (Resource, String, boolean, ResourceType, long) — assemble an IN_APP wrapper
        // holding a null-code Resource without tripping Builder.setResource's isInApp() pre-NPE.
        try {
            Constructor<ResourceWrapper> ctor = ResourceWrapper.class.getDeclaredConstructor(
                    Resource.class, String.class, boolean.class, ResourceType.class, long.class);
            ctor.setAccessible(true);
            return ctor.newInstance(resource, "", false, IN_APP, 0L);
        } catch (Exception e) {
            throw new RuntimeException("failed to build IN_APP ResourceWrapper", e);
        }
    }

    private RichMediaController controllerWith(RichMediaPresentingDelegate delegate) {
        // Real RichMediaController with the real ContextInAppFolderProvider. A delegate is set so
        // showResourceWrapper takes the useDelegate branch (the isCanceled -> isInAppDownloaded path).
        RichMediaController controller = new RichMediaController(
                mock(ResourceViewStrategyFactory.class),
                mock(RichMediaFactory.class),
                folderProvider,
                mock(RichMediaStyle.class));
        controller.setDelegate(delegate);
        return controller;
    }

    // ---- Target B: direct crash point getInAppFolder(null) now returns null instead of NPE ----

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

    // ---- Target A: full reach path through RichMediaController.isCanceled is now graceful ----

    @Test
    public void reachPath_showResourceWrapper_nullCode_abortsShowGracefully() {
        // Verifies that a null-code IN_APP wrapper aborts the show via the controller instead of letting
        // an NPE escape isCanceled (which runs before useDelegate's try). isCanceled -> isInAppDownloaded(
        // null)==false -> aborts -> useDelegate returns early; show is not presented.
        RichMediaPresentingDelegate delegate = mock(RichMediaPresentingDelegate.class);
        RichMediaController controller = controllerWith(delegate);
        ResourceWrapper wrapper = inAppWrapper(nullCodeInAppResource());

        controller.showResourceWrapper(wrapper); // must not throw — pre-fix the NPE escaped here

        verify(delegate, never()).shouldPresent(any());
    }

    // ---- Negative control: non-null code still flows through to the File / isCanceled path ----

    @Test
    public void negativeControl_nonNullCode_buildsFile() {
        // Verifies that a non-null code does NOT hit the new guard — it builds a File named after the code.
        // Discriminator: proves the guard is scoped to null and does not swallow valid codes.
        File folder = folderProvider.getInAppFolder("some-code");
        assertNotNull(folder);
        assertEquals("some-code", folder.getName());
    }

    @Test
    public void negativeControl_nonNullCode_reachPathDoesNotThrow() {
        // Verifies that the same reach path with a real-shaped IN_APP code also aborts cleanly (not
        // downloaded), confirming both null and non-null codes abort via the same branch — not a crash.
        RichMediaPresentingDelegate delegate = mock(RichMediaPresentingDelegate.class);
        RichMediaController controller = controllerWith(delegate);
        Resource nonNull = new Resource(
                "real-inapp-code", "https://example.com/inapp.zip", null, 0L, InAppLayout.FULLSCREEN, null, false, 0);
        ResourceWrapper wrapper = inAppWrapper(nonNull);

        controller.showResourceWrapper(wrapper); // must not throw

        verify(delegate, never()).shouldPresent(any());
    }
}
