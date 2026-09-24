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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * SDK-997: the status ladder only climbs.
 *
 * A single tap on a card can report two statuses at once — the open of the interaction and the
 * delete of a dismiss — and they run on separate pool threads. Before this guard the later
 * writer won: a delete that landed first was overwritten by the open, the message came back
 * with status 3, and the row the user dismissed stayed in the list. A read arriving after an
 * open used to undo it the same way.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class DbInboxStorageStatusLadderTest {

    private static final String ID = "msg-997";

    private DbInboxStorage storage;

    @Before
    public void setUp() {
        storage = new DbInboxStorage(new InboxDbHelper(RuntimeEnvironment.getApplication()));
        storage.mergeState(Collections.singleton(message()), false);
    }

    private static InboxMessageInternal message() {
        long now = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        return new InboxMessageInternal.Builder()
                .setId(ID)
                .setOrder(now)
                .setSendDate(now)
                .setExpiredDate(now + TimeUnit.DAYS.toSeconds(14))
                .setTitle("title")
                .setMessage("message")
                .setInboxMessageType(InboxMessageType.PLAIN)
                .setInboxMessageStatus(InboxMessageStatus.DELIVERED)
                .setSource(InboxMessageSource.PUSH)
                .build();
    }

    private InboxMessageStatus storedStatus() {
        InboxMessageInternal stored = storage.getActualInboxMessage(ID);
        return stored == null ? null : stored.getInboxMessageStatus();
    }

    @Test
    public void openThenDelete_leavesTheMessageDeleted() {
        storage.updateStatus(ID, InboxMessageStatus.OPEN);
        storage.updateStatus(ID, InboxMessageStatus.DELETED_BY_USER);

        assertEquals(InboxMessageStatus.DELETED_BY_USER, storedStatus());
    }

    @Test
    public void deleteThenOpen_doesNotResurrectTheMessage() {
        storage.updateStatus(ID, InboxMessageStatus.DELETED_BY_USER);

        Collection<String> reported = storage.updateStatus(ID, InboxMessageStatus.OPEN);

        assertEquals(
                "the open must not pull a deleted message back into the list",
                InboxMessageStatus.DELETED_BY_USER,
                storedStatus());
        assertTrue("and it is not reported either, matching iOS", reported.isEmpty());
    }

    @Test
    public void readAfterOpen_keepsTheOpen() {
        storage.updateStatus(ID, InboxMessageStatus.OPEN);

        Collection<String> reported = storage.updateStatus(ID, InboxMessageStatus.READ);

        assertEquals(InboxMessageStatus.OPEN, storedStatus());
        assertTrue("a lower status is not reported either", reported.isEmpty());
    }

    @Test
    public void repeatedOpen_isNotReportedTwice() {
        assertTrue(storage.updateStatus(ID, InboxMessageStatus.OPEN).contains(ID));

        assertTrue(
                "the open counter must not grow on a second tap",
                storage.updateStatus(ID, InboxMessageStatus.OPEN).isEmpty());
    }

    @Test
    public void deliveredAfterRead_doesNotUnreadTheMessage() {
        storage.updateStatus(ID, InboxMessageStatus.READ);

        storage.updateStatus(ID, InboxMessageStatus.DELIVERED);

        assertEquals(InboxMessageStatus.READ, storedStatus());
    }
}
