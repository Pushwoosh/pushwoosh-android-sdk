/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.inbox.storage.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.inbox.internal.data.InboxMessageInternal;
import com.pushwoosh.inbox.internal.data.InboxMessageSource;
import com.pushwoosh.inbox.internal.data.InboxMessageStatus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Regression guard for the "message flashes and disappears" race.
 *
 * A push-sourced inbox row is written locally the moment the push arrives, but
 * the server needs a few seconds to index the same message for
 * getInboxMessages. A full-list merge running inside that window used to treat
 * the server's empty snapshot as truth and delete the fresh local row — the
 * card vanished from the UI until the next sync brought it back. The fix keeps
 * PUSH-sourced rows younger than a grace period out of the snapshot deletion;
 * server-sourced rows and stale push rows are still cleaned up as before.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class DbInboxStorageFreshPushMergeTest {

    private DbInboxStorage storage;

    @Before
    public void setUp() {
        storage = new DbInboxStorage(new InboxDbHelper(RuntimeEnvironment.getApplication()));
    }

    private static InboxMessageInternal message(String id, InboxMessageSource source, long sendDateSeconds) {
        return new InboxMessageInternal.Builder()
                .setId(id)
                .setOrder(sendDateSeconds)
                .setSendDate(sendDateSeconds)
                .setExpiredDate(nowSeconds() + TimeUnit.DAYS.toSeconds(14))
                .setTitle("title-" + id)
                .setMessage("message-" + id)
                .setInboxMessageType(InboxMessageType.PLAIN)
                .setInboxMessageStatus(InboxMessageStatus.DELIVERED)
                .setSource(source)
                .build();
    }

    private static long nowSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }

    @Test
    public void fullListMerge_keepsFreshPushRow_whenServerSnapshotIsEmpty() {
        storage.mergeState(Collections.singleton(message("fresh-push", InboxMessageSource.PUSH, nowSeconds())), false);

        storage.mergeState(Collections.emptyList(), true);

        assertNotNull(
                "fresh push row must survive an empty server snapshot", storage.getActualInboxMessage("fresh-push"));
    }

    @Test
    public void fullListMerge_deletesStalePushRow_missingFromServerSnapshot() {
        long staleSendDate = nowSeconds() - TimeUnit.MINUTES.toSeconds(30);
        storage.mergeState(Collections.singleton(message("stale-push", InboxMessageSource.PUSH, staleSendDate)), false);

        storage.mergeState(Collections.emptyList(), true);

        assertNull(
                "stale push row must still be cleaned up by the snapshot merge",
                storage.getActualInboxMessage("stale-push"));
    }

    @Test
    public void fullListMerge_deletesServiceRow_missingFromServerSnapshot() {
        storage.mergeState(
                Collections.singleton(message("service-row", InboxMessageSource.SERVICE, nowSeconds())), false);

        storage.mergeState(Collections.emptyList(), true);

        assertNull(
                "server-sourced row absent from the snapshot must be deleted",
                storage.getActualInboxMessage("service-row"));
    }

    @Test
    public void fullListMerge_gracePinsAOneMinuteWindow() {
        // Bounds are probed 2s inside and outside the 60s window so a second ticking over
        // mid-test cannot flip them; widening or shrinking the window still breaks this.
        storage.mergeState(
                Collections.singleton(message("just-inside", InboxMessageSource.PUSH, nowSeconds() - 58)), false);
        storage.mergeState(
                Collections.singleton(message("just-outside", InboxMessageSource.PUSH, nowSeconds() - 62)), false);

        storage.mergeState(Collections.emptyList(), true);

        assertNotNull("a push row inside the grace window must survive", storage.getActualInboxMessage("just-inside"));
        assertNull("a push row past the grace window must be deleted", storage.getActualInboxMessage("just-outside"));
    }

    @Test
    public void fullListMerge_keepsPushRowWithSendDateInTheFuture() {
        // Clocks moved backwards between receiving the push and the merge: the age goes
        // negative, and the grace must degrade towards keeping the row, never deleting it.
        storage.mergeState(
                Collections.singleton(message("future-push", InboxMessageSource.PUSH, nowSeconds() + 3600)), false);

        storage.mergeState(Collections.emptyList(), true);

        assertNotNull(storage.getActualInboxMessage("future-push"));
    }

    @Test
    public void explicitClear_removesFreshPushRows() {
        // clearAllInboxMessages() is a user-initiated wipe, not a server snapshot —
        // the fresh-push grace must not keep anything alive there.
        storage.mergeState(Collections.singleton(message("fresh-push", InboxMessageSource.PUSH, nowSeconds())), false);

        storage.mergeState(Collections.emptyList(), true, false);

        assertNull("explicit clear must delete fresh push rows too", storage.getActualInboxMessage("fresh-push"));
    }

    @Test
    public void fullListMerge_keepsRowsPresentInServerSnapshot() {
        InboxMessageInternal fromServer = message("kept", InboxMessageSource.SERVICE, nowSeconds());
        storage.mergeState(Collections.singleton(fromServer), false);

        storage.mergeState(Collections.singleton(fromServer), true);

        assertNotNull(storage.getActualInboxMessage("kept"));
    }
}
