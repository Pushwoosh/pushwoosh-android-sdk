package com.pushwoosh.inapp.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

public class ActivityBroughtOnTopEventTest {

    @Before
    public void resetSingleton() throws Exception {
        setInstance(null);
    }

    @After
    public void tearDown() throws Exception {
        setInstance(null);
    }

    private static void setInstance(ActivityBroughtOnTopEvent value) throws Exception {
        Field f = ActivityBroughtOnTopEvent.class.getDeclaredField("INSTANCE");
        f.setAccessible(true);
        f.set(null, value);
    }

    // Verifies that first call to getInstance() creates a fresh singleton with count == 0.
    // Contract used by ModalRichMediaWindow:195 — first event has count <= 1, so popup shows.
    @Test
    public void getInstance_firstCall_createsInstanceWithZeroCount() {
        ActivityBroughtOnTopEvent result = ActivityBroughtOnTopEvent.getInstance();

        assertNotNull(result);
        assertEquals(0, result.count.get());
        assertSame(result, ActivityBroughtOnTopEvent.INSTANCE);
    }

    // Verifies that subsequent getInstance() calls return the same instance and increment the counter.
    // Contract used by ModalRichMediaWindow:195 — count > 1 suppresses popup (anti-spam).
    @Test
    public void getInstance_multipleCalls_returnsSameInstanceAndIncrementsCounter() {
        ActivityBroughtOnTopEvent first = ActivityBroughtOnTopEvent.getInstance();
        ActivityBroughtOnTopEvent second = ActivityBroughtOnTopEvent.getInstance();
        ActivityBroughtOnTopEvent third = ActivityBroughtOnTopEvent.getInstance();

        assertSame(first, second);
        assertSame(second, third);
        assertEquals(2, third.count.get());
    }

    // Verifies that resetCount() zeros out the counter without replacing the singleton instance.
    // Contract used by ModalRichMediaWindow:199 — after handler fires once, the next modal flow starts fresh.
    @Test
    public void resetCount_existingInstance_zeroesCounterAndKeepsIdentity() {
        ActivityBroughtOnTopEvent first = ActivityBroughtOnTopEvent.getInstance();
        ActivityBroughtOnTopEvent.getInstance();
        ActivityBroughtOnTopEvent.getInstance();
        assertEquals(2, first.count.get());

        ActivityBroughtOnTopEvent.resetCount();

        assertEquals(0, first.count.get());
        assertSame(first, ActivityBroughtOnTopEvent.INSTANCE);
    }
}
