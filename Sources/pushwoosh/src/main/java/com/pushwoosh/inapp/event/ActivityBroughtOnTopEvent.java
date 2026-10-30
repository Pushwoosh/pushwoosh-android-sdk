package com.pushwoosh.inapp.event;

import com.pushwoosh.internal.event.Event;

import java.util.concurrent.atomic.AtomicInteger;

public class ActivityBroughtOnTopEvent implements Event {
    public AtomicInteger count;
    public static volatile ActivityBroughtOnTopEvent INSTANCE;

    public ActivityBroughtOnTopEvent() {
        this.count = new AtomicInteger();
        count.set(0);
    }

    public static ActivityBroughtOnTopEvent getInstance() {
        ActivityBroughtOnTopEvent local = INSTANCE;
        if (local == null) {
            synchronized (ActivityBroughtOnTopEvent.class) {
                local = INSTANCE;
                if (local == null) {
                    local = new ActivityBroughtOnTopEvent();
                    INSTANCE = local;
                    return local;
                }
            }
        }
        local.count.incrementAndGet();
        return local;
    }

    public static void resetCount() {
        INSTANCE.count.set(0);
    }
}
