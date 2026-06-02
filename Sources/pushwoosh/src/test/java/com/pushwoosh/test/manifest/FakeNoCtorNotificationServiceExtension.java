package com.pushwoosh.test.manifest;

import com.pushwoosh.notification.NotificationServiceExtension;

public class FakeNoCtorNotificationServiceExtension extends NotificationServiceExtension {
    @SuppressWarnings("unused")
    public FakeNoCtorNotificationServiceExtension(int unused) {
        super();
    }
}
