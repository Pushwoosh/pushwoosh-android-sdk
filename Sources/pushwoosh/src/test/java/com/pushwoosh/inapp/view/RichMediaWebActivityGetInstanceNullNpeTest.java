package com.pushwoosh.inapp.view;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class RichMediaWebActivityGetInstanceNullNpeTest {

    private MockedStatic<PushwooshPlatform> pushwooshPlatformStatic;

    @Before
    public void setUp() {
        EventBus.clearSubscribersMap();
    }

    @After
    public void tearDown() {
        if (pushwooshPlatformStatic != null) {
            pushwooshPlatformStatic.close();
        }
        EventBus.clearSubscribersMap();
    }

    private Intent intentWithResource() {
        Resource resource = new Resource("code1", "url1", "hash1", 0, InAppLayout.FULLSCREEN, null, true, 1);
        return WebActivity.applyIntentParams(new Intent(), resource, "123", 0);
    }

    private ActivityController<RichMediaWebActivity> driveOnCreate() {
        Intent intent = intentWithResource();
        ActivityController<RichMediaWebActivity> controller =
                Robolectric.buildActivity(RichMediaWebActivity.class, intent);
        controller.create(intent.getExtras());
        return controller;
    }

    // Verifies that with the singleton forced null, getInstance() really is null (load-bearing fact).
    @Test
    public void getInstance_whenSingletonNull_isNull() {
        pushwooshPlatformStatic = Mockito.mockStatic(PushwooshPlatform.class);
        pushwooshPlatformStatic.when(PushwooshPlatform::getInstance).thenReturn(null);

        assertNull(PushwooshPlatform.getInstance());
    }

    // Verifies that a failed-init singleton (getInstance()==null) no longer crashes onCreate: the first
    // NPE in super.onCreate is caught, finish()+return short-circuits before startTimerEnableBackButton:144,
    // so the second deref never happens. Regression guard for the missing `return` after finish().
    @Test
    public void onCreate_getInstanceNull_finishesGracefully_1() {
        assertOnCreateFinishesGracefully();
    }

    // Verifies the same graceful short-circuit on a fresh controller — confirms determinism.
    @Test
    public void onCreate_getInstanceNull_finishesGracefully_2() {
        assertOnCreateFinishesGracefully();
    }

    // Verifies the same graceful short-circuit on a fresh controller — confirms determinism.
    @Test
    public void onCreate_getInstanceNull_finishesGracefully_3() {
        assertOnCreateFinishesGracefully();
    }

    private void assertOnCreateFinishesGracefully() {
        pushwooshPlatformStatic = Mockito.mockStatic(PushwooshPlatform.class);
        pushwooshPlatformStatic.when(PushwooshPlatform::getInstance).thenReturn(null);

        ActivityController<RichMediaWebActivity> controller = driveOnCreate();

        RichMediaWebActivity activity = controller.get();
        assertTrue(
                "onCreate must finish() the activity after super.onCreate threw on the null singleton",
                activity.isFinishing());
    }

    // Verifies that a real, fully-built singleton (init completed) runs onCreate to completion without
    // finishing — proves getInstance()==null is the necessary condition for the finish() short-circuit.
    @Test
    public void onCreate_realPlatform_doesNotCrash() {
        // No mockStatic here: PlatformTestManager builds the real singleton via Builder.build().
        PlatformTestManager platformTestManager = new PlatformTestManager();
        try {
            EventBus.clearSubscribersMap();
            Assert.assertNotNull(PushwooshPlatform.getInstance());

            ActivityController<RichMediaWebActivity> controller = driveOnCreate();
            Assert.assertNotNull(controller.get());
            assertFalse(
                    "with a real singleton onCreate should run to completion, not finish()",
                    controller.get().isFinishing());
        } finally {
            platformTestManager.tearDown();
        }
    }
}
