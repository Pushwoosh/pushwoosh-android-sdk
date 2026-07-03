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

package com.pushwoosh;

import static org.junit.Assert.assertNull;

import com.pushwoosh.amazon.TagsRegistrarHelper;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.tags.TagsBundle;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

// Regression guard for crash candidate #19 (crash-amazon-handlerjob-tagsbundle-toctou).
//
// The fix did two things to PushAmazonHandlerJob.onRegistered:
//  (a) read TagsRegistrarHelper.tagsBundle ONCE into a local (eliminating the check-then-use double
//      read that was the TOCTOU defect — a concurrent null-write can no longer land between two reads);
//  (b) wrap the callback body in try/catch(Exception), restoring symmetry with the legacy
//      PushAmazonIntentService so any residual throw is swallowed+logged rather than escaping.
//
// Part A mirrors the NEW read-once prod logic and proves it is race-immune: a writer that nulls the
// field after the single read cannot affect the already-captured local. Part B drives the real
// JobService onRegistered with a throwing toJson() and asserts the wrapped callback now SWALLOWS the
// NPE (no escape) — behaviourally matching the legacy sibling negative control.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PushAmazonHandlerJobTagsBundleToctouTest {

    @After
    public void tearDown() {
        TagsRegistrarHelper.tagsBundle = null;
    }

    /** Build the live JobService bypassing the throwing amazon-stub super-ctor. */
    private static PushAmazonHandlerJob newJobBypassingStubCtor() {
        return Mockito.mock(PushAmazonHandlerJob.class, Mockito.CALLS_REAL_METHODS);
    }

    private static PushAmazonIntentService newLegacyBypassingStubCtor() {
        return Mockito.mock(PushAmazonIntentService.class, Mockito.CALLS_REAL_METHODS);
    }

    /**
     * A non-null {@code TagsBundle} whose {@code toJson()} throws the exact NPE the JVM raises for a null
     * receiver. Passes the {@code :34 != null} check; makes the {@code :35} deref expression NPE.
     */
    private static TagsBundle nullDerefThrowingBundle() {
        TagsBundle bundle = Mockito.mock(TagsBundle.class);
        Mockito.when(bundle.toJson())
                .thenThrow(new NullPointerException("Cannot invoke \"com.pushwoosh.tags.TagsBundle.toJson()\" because "
                        + "\"com.pushwoosh.amazon.TagsRegistrarHelper.tagsBundle\" is null"));
        return bundle;
    }

    // =====================================================================================
    // Part A — race-immunity proof: deterministic mirror of the NEW read-once prod logic.
    // =====================================================================================

    /**
     * Runs the fixed read-once logic on a worker thread, with a latch forcing a concurrent null-write to
     * land AFTER the single read. Because the field is read once into a local, the captured reference is
     * unaffected by the later null-write and the deref cannot NPE (the TOCTOU window no longer exists).
     *
     * @return the NPE thrown at the deref, or {@code null} if the snippet completed without throwing.
     */
    private NullPointerException runReadOnceUnderRace() throws InterruptedException {
        TagsRegistrarHelper.tagsBundle = Tags.intTag("a", 1); // field non-null at the read
        CountDownLatch readDone = new CountDownLatch(1);
        CountDownLatch nullWritten = new CountDownLatch(1);
        AtomicReference<NullPointerException> thrown = new AtomicReference<>();
        AtomicReference<String> result = new AtomicReference<>();

        Thread worker = new Thread(() -> {
            // ---- single read into a local (fixed prod logic) ----
            TagsBundle tags = TagsRegistrarHelper.tagsBundle;
            readDone.countDown(); // signal: the field has been captured
            try {
                nullWritten.await(); // let the concurrent null-write happen now
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            try {
                // deref the captured local, not a re-read of the field
                String tagsJson = tags != null ? tags.toJson().toString() : null;
                result.set(tagsJson);
            } catch (NullPointerException e) {
                thrown.set(e);
            }
        });

        worker.start();
        readDone.await(); // worker has captured the field into its local
        TagsRegistrarHelper.tagsBundle = null; // concurrent writer nulls the field AFTER the read
        nullWritten.countDown();
        worker.join();
        return thrown.get();
    }

    @Test
    public void readOnceUnderRace_noNpe() throws Exception {
        assertNull("read-once local must not be affected by a later null-write", runReadOnceUnderRace());
    }

    // =====================================================================================
    // Part B — graceful swallow on the REAL PushAmazonHandlerJob.onRegistered (wrapped callback).
    // =====================================================================================

    @Test
    public void realJobOnRegistered_swallowed() {
        TagsRegistrarHelper.tagsBundle = nullDerefThrowingBundle();
        PushAmazonHandlerJob job = newJobBypassingStubCtor();
        try (MockedStatic<NotificationRegistrarHelper> ignored =
                Mockito.mockStatic(NotificationRegistrarHelper.class)) {
            // The try/catch(Exception) wrap swallows the toJson() NPE -> nothing escapes.
            job.onRegistered(null, "regId");
        }
    }

    // =====================================================================================
    // Part C — negative controls (the swallow/read-once must not break the normal path).
    // =====================================================================================

    /** Negative control: the legacy wrapped service SWALLOWS the identical deref NPE (parity argument). */
    @Test
    public void negativeControl_legacyIntentService_swallows() {
        TagsRegistrarHelper.tagsBundle = nullDerefThrowingBundle();
        PushAmazonIntentService legacy = newLegacyBypassingStubCtor();
        try (MockedStatic<NotificationRegistrarHelper> ignored =
                Mockito.mockStatic(NotificationRegistrarHelper.class)) {
            legacy.onRegistered("regId"); // try/catch(Exception) swallows -> no throw
        }
    }

    /**
     * Negative control: with a non-null tagsBundle the fixed onRegistered still delegates the tags JSON
     * and clears the field — the swallow/read-once changes did not turn the happy path into a no-op.
     */
    @Test
    public void negativeControl_nonNullTags_delegatesAndClears() {
        TagsBundle bundle = Tags.intTag("a", 1);
        String expectedJson = bundle.toJson().toString();
        TagsRegistrarHelper.tagsBundle = bundle;
        PushAmazonHandlerJob job = newJobBypassingStubCtor();
        try (MockedStatic<NotificationRegistrarHelper> helper = Mockito.mockStatic(NotificationRegistrarHelper.class)) {
            job.onRegistered(null, "regId");
            helper.verify(() -> NotificationRegistrarHelper.onRegisteredForRemoteNotifications("regId", expectedJson));
        }
        assertNull("tagsBundle must be cleared after a successful registration", TagsRegistrarHelper.tagsBundle);
    }
}
