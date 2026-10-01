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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * SDK-1059: one status transition is reported once, however many callers race for it.
 *
 * InboxRepository runs every updateStatus on a pool thread. The inbox UI asks to read the
 * same visible rows up to four times on the first load, so several threads change one code
 * at once. When the status check and the write were separate steps, every thread saw
 * DELIVERED, every thread reported the change, and the backend got duplicate inboxStatus
 * requests for the same message.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class DbInboxStorageConcurrentStatusTest {

    private static final int THREADS = 8;
    private static final int ROUNDS = 50;

    private DbInboxStorage storage;

    @Before
    public void setUp() {
        storage = new DbInboxStorage(new InboxDbHelper(RuntimeEnvironment.getApplication()));
    }

    private static InboxMessageInternal message(String id) {
        long now = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        return new InboxMessageInternal.Builder()
                .setId(id)
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

    private InboxMessageStatus storedStatus(String id) {
        InboxMessageInternal stored = storage.getActualInboxMessage(id);
        return stored == null ? null : stored.getInboxMessageStatus();
    }

    /** Releases one caller per entry of {@code statuses} at once; returns how many got {@code id} back, per status. */
    private int[] race(String id, InboxMessageStatus[] statuses) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(statuses.length);
        CountDownLatch ready = new CountDownLatch(statuses.length);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Collection<String>>> results = new ArrayList<>();
        for (InboxMessageStatus status : statuses) {
            results.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                return storage.updateStatus(id, status);
            }));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();

        int[] winners = new int[InboxMessageStatus.values().length];
        for (int i = 0; i < statuses.length; i++) {
            Collection<String> reported = results.get(i).get(10, TimeUnit.SECONDS);
            if (reported.contains(id)) {
                winners[statuses[i].ordinal()]++;
            } else {
                assertTrue("a caller that lost the race reports nothing", reported.isEmpty());
            }
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        return winners;
    }

    private static InboxMessageStatus[] repeat(InboxMessageStatus status) {
        InboxMessageStatus[] statuses = new InboxMessageStatus[THREADS];
        for (int i = 0; i < THREADS; i++) {
            statuses[i] = status;
        }
        return statuses;
    }

    private void assertSameStatusIsReportedOnce(InboxMessageStatus status) throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            String id = "msg-1059-" + status.name() + "-" + round;
            storage.mergeState(Collections.singleton(message(id)), false);

            int[] winners = race(id, repeat(status));

            assertEquals("round " + round + ": exactly one caller reports " + status, 1, winners[status.ordinal()]);
            assertEquals("round " + round, status, storedStatus(id));
        }
    }

    @Test
    public void concurrentRead_isReportedOnce() throws Exception {
        assertSameStatusIsReportedOnce(InboxMessageStatus.READ);
    }

    @Test
    public void concurrentOpen_isReportedOnce() throws Exception {
        assertSameStatusIsReportedOnce(InboxMessageStatus.OPEN);
    }

    @Test
    public void concurrentReadAndOpen_openIsReportedOnce() throws Exception {
        InboxMessageStatus[] statuses = new InboxMessageStatus[THREADS];
        for (int i = 0; i < THREADS; i++) {
            statuses[i] = i % 2 == 0 ? InboxMessageStatus.READ : InboxMessageStatus.OPEN;
        }
        for (int round = 0; round < ROUNDS; round++) {
            String id = "msg-1059-mixed-" + round;
            storage.mergeState(Collections.singleton(message(id)), false);

            int[] winners = race(id, statuses);

            assertEquals("round " + round + ": one OPEN", 1, winners[InboxMessageStatus.OPEN.ordinal()]);
            assertTrue("round " + round + ": at most one READ", winners[InboxMessageStatus.READ.ordinal()] <= 1);
            assertEquals("round " + round, InboxMessageStatus.OPEN, storedStatus(id));
        }
    }
}
