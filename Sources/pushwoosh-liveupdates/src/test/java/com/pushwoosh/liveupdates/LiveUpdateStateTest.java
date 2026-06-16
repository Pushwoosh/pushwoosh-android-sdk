package com.pushwoosh.liveupdates;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class LiveUpdateStateTest {

    @Test
    public void showWhen_defaultsToTrue_whenBuilderNotTouched() {
        // boolean defaults to false, but today's behaviour is "time shown" — the Builder
        // must seed showWhen=true so direct API users (no parser) keep today's behaviour.
        LiveUpdateState s = new LiveUpdateState.Builder("id", LiveUpdateOperation.START).build();

        assertTrue(s.isShowWhen());
        assertNull(s.getWhen());
        assertFalse(s.isChronometer());
        assertFalse(s.isChronometerCountDown());
    }

    @Test
    public void showProgressBar_defaultsToTrue_whenBuilderNotTouched() {
        // boolean defaults to false, but today's behaviour is "bar shown" — the Builder
        // must seed showProgressBar=true so direct API users keep the progress bar by default.
        LiveUpdateState s = new LiveUpdateState.Builder("id", LiveUpdateOperation.START).build();

        assertTrue(s.showProgressBar());
    }
}
