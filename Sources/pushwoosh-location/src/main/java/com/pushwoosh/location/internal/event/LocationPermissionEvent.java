package com.pushwoosh.location.internal.event;

import com.pushwoosh.internal.event.PermissionEvent;

import java.util.List;

public class LocationPermissionEvent extends PermissionEvent {
    public LocationPermissionEvent(List<String> grantedPermissions, List<String> deniedPermissions) {
        super(grantedPermissions, deniedPermissions);
    }
}
