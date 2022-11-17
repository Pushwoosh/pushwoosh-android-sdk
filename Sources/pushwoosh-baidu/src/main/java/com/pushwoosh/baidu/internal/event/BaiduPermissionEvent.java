package com.pushwoosh.baidu.internal.event;

import com.pushwoosh.internal.event.PermissionEvent;

import java.util.List;

public class BaiduPermissionEvent extends PermissionEvent {
    public BaiduPermissionEvent(List<String> grantedPermissions, List<String> deniedPermissions) {
        super(grantedPermissions, deniedPermissions);
    }
}
