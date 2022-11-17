package com.pushwoosh.huawei.utils;

import com.huawei.hms.push.RemoteMessage;

public class RemoteMessageUtils {
    public static boolean isPushwooshMessage(RemoteMessage remoteMessage) {
        if (remoteMessage == null || remoteMessage.getDataOfMap() == null) {
            return false;
        }
        return remoteMessage.getDataOfMap().containsKey("pw_msg");
    }
}
