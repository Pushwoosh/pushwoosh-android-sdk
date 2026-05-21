package com.pushwoosh.internal.event;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class EmitterTest {

    private static final class TestEvent implements Event {
        final int id;

        TestEvent(int id) {
            this.id = id;
        }
    }

    private static final class ConditionEvent implements Event {}

    /**
     * Anonymous subclass that exposes the protected {@code emit} so tests can pump events into
     * the base emitter (mirrors the way internal SDK code emits from inside its own emitters).
     */
    private static final class TestEmitter<T extends Event> extends Emitter<T> {
        void publicEmit(T event) {
            emit(event);
        }
    }

    @Mock
    EventListener<TestEvent> listener;

    @Mock
    EventListener<TestEvent> listenerB;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        EventBus.clearSubscribersMap();
    }

    @After
    public void tearDown() {
        EventBus.clearSubscribersMap();
    }

    @Test
    public void bind_replacesPreviousListener() {
        TestEmitter<TestEvent> emitter = new TestEmitter<>();
        TestEvent event = new TestEvent(1);
        emitter.bind(listener);
        emitter.bind(listenerB);

        emitter.publicEmit(event);

        verify(listenerB, times(1)).onReceive(event);
        verifyNoInteractions(listener);
    }

    @Test
    public void when_eventAfterCondition_isDeliveredImmediately() {
        TestEmitter<TestEvent> source = new TestEmitter<>();
        TestEmitter<ConditionEvent> condition = new TestEmitter<>();
        Emitter<TestEvent> combined = Emitter.when(source, condition);
        combined.bind(listener);

        condition.publicEmit(new ConditionEvent());
        TestEvent event = new TestEvent(1);
        source.publicEmit(event);

        verify(listener, times(1)).onReceive(event);
    }

    @Test
    public void when_eventsBeforeCondition_areBufferedAndFlushedInOrder() {
        TestEmitter<TestEvent> source = new TestEmitter<>();
        TestEmitter<ConditionEvent> condition = new TestEmitter<>();
        Emitter<TestEvent> combined = Emitter.when(source, condition);
        combined.bind(listener);

        TestEvent first = new TestEvent(1);
        TestEvent second = new TestEvent(2);
        source.publicEmit(first);
        source.publicEmit(second);

        verifyNoInteractions(listener);

        condition.publicEmit(new ConditionEvent());

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onReceive(first);
        inOrder.verify(listener).onReceive(second);
    }
}
