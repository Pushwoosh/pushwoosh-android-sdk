package com.pushwoosh.inbox.internal;

import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class PushwooshInboxModuleTest {

    @Before
    public void setUp() throws Exception {
        resetModuleStatics();
    }

    @After
    public void tearDown() throws Exception {
        resetModuleStatics();
    }

    // The seam moved RequestManager resolution into InboxRepository, so the module stopped holding one and
    // its sRequestManager guard went away. The fail-fast for "plugin init has not run" must still fire —
    // it now rests on the sInboxDbHelper check inside getInboxStorage().
    @Test
    public void getInboxRepository_beforeInit_failsFast() {
        assertThrows(IllegalArgumentException.class, PushwooshInboxModule::getInboxRepository);
    }

    private void resetModuleStatics() throws Exception {
        for (String name : new String[] {"sInboxRepository", "sInboxStorage", "sInboxDbHelper"}) {
            Field field = PushwooshInboxModule.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, null);
        }
    }
}
