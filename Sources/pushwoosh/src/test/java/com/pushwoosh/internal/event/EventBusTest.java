package com.pushwoosh.internal.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class EventBusTest {

    private static final class TestEvent implements Event {
        final int id;

        TestEvent(int id) {
            this.id = id;
        }
    }

    private static final class OtherEvent implements Event {}

    @Before
    public void setUp() {
        EventBus.clearSubscribersMap();
    }

    @After
    public void tearDown() {
        EventBus.clearSubscribersMap();
    }

    @Test
    public void subscribe_returnsSubscription_andRegistersListener() {
        EventListener<TestEvent> listener = event -> {};

        Subscription<TestEvent> subscription = EventBus.subscribe(TestEvent.class, listener);

        assertNotNull(subscription);
        Map<Class<? extends Event>, List<EventListener<?>>> map = EventBus.getSubscribersMap();
        assertEquals(1, map.get(TestEvent.class).size());
    }

    @Test(expected = NullPointerException.class)
    public void subscribe_withNullListener_throws() {
        EventBus.subscribe(TestEvent.class, null);
    }

    @Test
    public void sendEvent_noSubscribers_returnsFalse() {
        boolean delivered = EventBus.sendEvent(new TestEvent(1));

        assertFalse(delivered);
    }

    @Test
    public void sendEvent_afterAllUnsubscribed_returnsFalse() {
        Subscription<TestEvent> subscription = EventBus.subscribe(TestEvent.class, event -> {});
        subscription.unsubscribe();

        boolean delivered = EventBus.sendEvent(new TestEvent(1));

        assertFalse(delivered);
    }

    @Test
    public void sendEvent_dispatchesToListenerOnMain() {
        AtomicInteger received = new AtomicInteger();
        EventBus.subscribe(TestEvent.class, event -> received.set(event.id));

        boolean delivered = EventBus.sendEvent(new TestEvent(42));
        ShadowLooper.idleMainLooper();

        assertTrue(delivered);
        assertEquals(42, received.get());
    }

    @Test
    public void sendEvent_throwingListener_doesNotPreventOthers() {
        AtomicInteger second = new AtomicInteger();
        EventBus.subscribe(TestEvent.class, event -> {
            throw new RuntimeException("boom");
        });
        EventBus.subscribe(TestEvent.class, event -> second.set(event.id));

        EventBus.sendEvent(new TestEvent(7));
        ShadowLooper.idleMainLooper();

        assertEquals(7, second.get());
    }

    @Test
    public void sendEvent_throwingErrorListener_doesNotPreventOthers() {
        AtomicInteger second = new AtomicInteger();
        EventBus.subscribe(TestEvent.class, event -> {
            throw new AssertionError("fatal");
        });
        EventBus.subscribe(TestEvent.class, event -> second.set(event.id));

        EventBus.sendEvent(new TestEvent(9));
        ShadowLooper.idleMainLooper();

        assertEquals(9, second.get());
    }

    @Test
    public void sendEvent_throwingListener_doesNotCrashLooper() {
        EventBus.subscribe(TestEvent.class, event -> {
            throw new RuntimeException("boom");
        });

        EventBus.sendEvent(new TestEvent(1));
        ShadowLooper.idleMainLooper();

        AtomicInteger followUp = new AtomicInteger();
        EventBus.subscribe(OtherEvent.class, event -> followUp.incrementAndGet());
        EventBus.sendEvent(new OtherEvent());
        ShadowLooper.idleMainLooper();

        assertEquals(1, followUp.get());
    }

    @Test
    public void unsubscribeDuringDispatch_currentDeliveryStillCompletes() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<EventListener<TestEvent>> holder = new AtomicReference<>();
        EventListener<TestEvent> listener = event -> {
            calls.incrementAndGet();
            EventBus.unsubscribe(TestEvent.class, holder.get());
        };
        holder.set(listener);
        EventBus.subscribe(TestEvent.class, listener);

        EventBus.sendEvent(new TestEvent(1));
        ShadowLooper.idleMainLooper();

        assertEquals(1, calls.get());

        EventBus.sendEvent(new TestEvent(2));
        ShadowLooper.idleMainLooper();

        assertEquals(1, calls.get());
    }

    @Test
    public void subscribeDuringDispatch_doesNotReceiveCurrentEvent() {
        AtomicInteger lateCalls = new AtomicInteger();
        EventListener<TestEvent> lateListener = event -> lateCalls.incrementAndGet();

        EventBus.subscribe(TestEvent.class, event -> EventBus.subscribe(TestEvent.class, lateListener));

        EventBus.sendEvent(new TestEvent(1));
        ShadowLooper.idleMainLooper();

        assertEquals(0, lateCalls.get());

        EventBus.sendEvent(new TestEvent(2));
        ShadowLooper.idleMainLooper();

        assertEquals(1, lateCalls.get());
    }

    @Test
    public void concurrentSubscribe_doesNotLoseListeners() throws Exception {
        int iterations = 50;
        int threads = 16;
        int listenersPerThread = 32;
        int total = threads * listenersPerThread;

        for (int iter = 0; iter < iterations; iter++) {
            EventBus.clearSubscribersMap();

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < listenersPerThread; i++) {
                            EventBus.subscribe(TestEvent.class, event -> {});
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue("workers did not finish on iteration " + iter, done.await(10, TimeUnit.SECONDS));
            executor.shutdown();
            assertTrue(
                    "executor did not terminate on iteration " + iter, executor.awaitTermination(5, TimeUnit.SECONDS));

            List<EventListener<?>> registered = EventBus.getSubscribersMap().get(TestEvent.class);
            assertNotNull("listeners missing on iteration " + iter, registered);
            assertEquals("lost listeners on iteration " + iter, total, registered.size());
        }
    }

    @Test
    public void subscriptionUnsubscribe_removesListener() {
        EventListener<TestEvent> listener = event -> {};
        Subscription<TestEvent> subscription = EventBus.subscribe(TestEvent.class, listener);

        subscription.unsubscribe();

        List<EventListener<?>> registered = EventBus.getSubscribersMap().get(TestEvent.class);
        assertNotNull(registered);
        assertTrue(registered.isEmpty());
    }
}
