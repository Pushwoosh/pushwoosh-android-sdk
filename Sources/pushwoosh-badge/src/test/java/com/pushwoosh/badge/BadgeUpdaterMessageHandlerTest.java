package com.pushwoosh.badge;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Bundle;
import android.os.Looper;

import com.pushwoosh.badge.notification.BadgeUpdaterMessageHandler;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.notification.PushMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class BadgeUpdaterMessageHandlerTest {

    // handleNotification is protected, so a subclass is the only way to drive it from a test.
    private static class TestableHandler extends BadgeUpdaterMessageHandler {
        void deliver(PushMessage pushMessage) {
            handleNotification(pushMessage);
        }
    }

    private TestableHandler handler;

    @Before
    public void setUp() {
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
        BadgeModule.init();
        handler = new TestableHandler();
        setBadge(3);
    }

    private void setBadge(int value) {
        PushwooshBadge.setBadgeNumber(value);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private void handle(String pwBadges) {
        Bundle extras = new Bundle();
        if (pwBadges != null) {
            extras.putString("pw_badges", pwBadges);
        }
        // The real PushMessage constructor needs an initialized RepositoryModule, which this module
        // cannot bootstrap; the payload values still come from the real parser.
        PushMessage pushMessage = mock(PushMessage.class);
        when(pushMessage.getBadges()).thenReturn(PushBundleDataProvider.getBadges(extras));
        when(pushMessage.isBadgesAdditive()).thenReturn(PushBundleDataProvider.isBadgesAdditive(extras));

        handler.deliver(pushMessage);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void payloadWithoutBadges_keepsCurrentBadge() {
        handle(null);
        assertEquals("a push without pw_badges must not touch the badge", 3, PushwooshBadge.getBadgeNumber());
    }

    @Test
    public void absoluteBadges_overwritesBadge() {
        handle("7");
        assertEquals(7, PushwooshBadge.getBadgeNumber());
    }

    @Test
    public void additiveBadges_incrementsBadge() {
        handle("+2");
        assertEquals(5, PushwooshBadge.getBadgeNumber());
    }

    @Test
    public void negativeAdditiveBadges_decrementsBadge() {
        handle("-1");
        assertEquals(2, PushwooshBadge.getBadgeNumber());
    }

    @Test
    public void zeroBadges_clearsBadge() {
        handle("0");
        assertEquals(0, PushwooshBadge.getBadgeNumber());
    }
}
