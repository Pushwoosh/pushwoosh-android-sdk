package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class BackgroundExecutorTest {

    @Test
    public void main_throwingTask_doesNotBreakSubsequentTasks() {
        BackgroundExecutor.main(() -> {
            throw new RuntimeException("boom");
        });
        ShadowLooper.idleMainLooper();

        AtomicInteger followUp = new AtomicInteger();
        BackgroundExecutor.main(followUp::incrementAndGet);
        ShadowLooper.idleMainLooper();

        assertEquals(1, followUp.get());
    }

    @Test
    public void main_throwingError_doesNotBreakSubsequentTasks() {
        BackgroundExecutor.main(() -> {
            throw new AssertionError("fatal");
        });
        ShadowLooper.idleMainLooper();

        AtomicInteger followUp = new AtomicInteger();
        BackgroundExecutor.main(followUp::incrementAndGet);
        ShadowLooper.idleMainLooper();

        assertEquals(1, followUp.get());
    }
}
