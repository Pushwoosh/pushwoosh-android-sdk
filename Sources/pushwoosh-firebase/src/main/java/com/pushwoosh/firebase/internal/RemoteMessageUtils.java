package com.pushwoosh.firebase.internal;

import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class RemoteMessageUtils {
    public static boolean isPushwooshMessage(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        //noinspection RedundantIfStatement
        if (data != null && data.containsKey("pw_msg")) {
            return true;
        }

        return false;
    }
}
