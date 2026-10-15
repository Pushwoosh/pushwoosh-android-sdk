package com.pushwoosh.testutil;

/**
 * A NotificationServiceExtension that opts out of SDK link handling, the way an app with its own
 * routing does. Everything else is inherited from {@link NotificationServiceMock}, so the shared
 * callback mock still records the calls.
 */
public class NoUrlNotificationServiceMock extends NotificationServiceMock {

    @Override
    protected boolean preHandleNotificationsWithUrl() {
        return false;
    }
}
