package com.pushwoosh.notification;

import android.os.Bundle;

public class PushMessageFactory {
    public PushMessage createPushMessage(Bundle pushBundle) {
        return new PushMessage(pushBundle);
    }
}
