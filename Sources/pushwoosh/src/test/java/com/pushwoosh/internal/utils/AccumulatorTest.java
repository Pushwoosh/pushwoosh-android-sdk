package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class AccumulatorTest {

    @Mock
    private Accumulator.Completion<String> completion;

    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void accumulate_singleItem_flushesAfterDelay() {
        Accumulator<String> accumulator = new Accumulator<>(completion, 100);

        accumulator.accumulate("a");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(completion, times(1)).onAccumulated(captor.capture());
        assertEquals(Arrays.asList("a"), captor.getValue());
    }

    @Test
    public void accumulate_multipleItemsWithinWindow_mergedIntoSingleCallbackPreservingOrder() {
        Accumulator<String> accumulator = new Accumulator<>(completion, 100);

        accumulator.accumulate("a");
        accumulator.accumulate("b");
        accumulator.accumulate("c");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(completion, times(1)).onAccumulated(captor.capture());
        assertEquals(Arrays.asList("a", "b", "c"), captor.getValue());
    }

    @Test
    public void accumulate_afterFlush_schedulesNewDelayedRunnable() {
        Accumulator<String> accumulator = new Accumulator<>(completion, 100);

        accumulator.accumulate("a");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        accumulator.accumulate("b");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(completion, times(2)).onAccumulated(captor.capture());
        List<List<String>> values = captor.getAllValues();
        assertEquals(Arrays.asList("a"), values.get(0));
        assertEquals(Arrays.asList("b"), values.get(1));
    }

    // Verifies that concurrent accumulate(...) calls from many threads all land in a single
    // delayed flush — pins the synchronized(accumulatedData) thread-safety contract.
    @Test
    public void accumulate_concurrentCallsFromMultipleThreads_allItemsCollectedInSingleFlush() throws Exception {
        Accumulator<String> accumulator = new Accumulator<>(completion, 100);

        final int threadCount = 10;
        final CountDownLatch startSignal = new CountDownLatch(1);
        final CountDownLatch finishedSignal = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                final String item = "item-" + i;
                pool.submit(() -> {
                    try {
                        startSignal.await();
                        accumulator.accumulate(item);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishedSignal.countDown();
                    }
                });
            }

            startSignal.countDown();
            assertTrue("threads did not finish accumulating in time", finishedSignal.await(2, TimeUnit.SECONDS));
        } finally {
            pool.shutdown();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(completion, times(1)).onAccumulated(captor.capture());
        List<String> captured = captor.getValue();
        assertEquals("all items must land in the single drain", threadCount, captured.size());

        Set<String> expected = new HashSet<>();
        for (int i = 0; i < threadCount; i++) {
            expected.add("item-" + i);
        }
        assertEquals("captured items must match the set of submitted items", expected, new HashSet<>(captured));
    }
}
