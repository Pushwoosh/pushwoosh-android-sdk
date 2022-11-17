package com.pushwoosh.internal.platform;

import com.pushwoosh.internal.event.EventBus;

import java.util.concurrent.atomic.AtomicBoolean;

class ApplicationState {
    private final AtomicBoolean isForeground = new AtomicBoolean(false);

    ApplicationState() {
        EventBus.subscribe(ApplicationOpenDetector.ApplicationMovedToForegroundEvent.class, event -> {
            isForeground.set(true);
        });

        EventBus.subscribe(ApplicationOpenDetector.ApplicationMovedToBackgroundEvent.class, event -> {
            isForeground.set(false);
        });
    }

    boolean isForeground() {
        return isForeground.get();
    }
}
