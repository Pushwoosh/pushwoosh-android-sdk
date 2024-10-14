package com.pushwoosh.inapp.event;

import com.pushwoosh.internal.event.Event;

import java.util.concurrent.atomic.AtomicInteger;

public class ActivityBroughtOnTopEvent implements Event {
    public AtomicInteger count;
    public static ActivityBroughtOnTopEvent INSTANCE;

    public ActivityBroughtOnTopEvent() {
        this.count = new AtomicInteger();
        count.set(0);
    }

    public static ActivityBroughtOnTopEvent getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ActivityBroughtOnTopEvent();
        } else {
            INSTANCE.count.incrementAndGet();
        }
        return INSTANCE;
    }

    public static void resetCount() {
        INSTANCE.count.set(0);
    }
}
