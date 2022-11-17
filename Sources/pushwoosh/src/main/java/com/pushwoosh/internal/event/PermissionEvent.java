package com.pushwoosh.internal.event;

import java.util.List;

/**
 * This class is not abstract to keep a single entry point for location and notification permissions tests
 */
public class PermissionEvent implements Event{
    private final List<String> grantedPermissions;
    private final List<String> deniedPermissions;

    public PermissionEvent(final List<String> grantedPermissions, final List<String> deniedPermissions) {
        this.grantedPermissions = grantedPermissions;
        this.deniedPermissions = deniedPermissions;
    }

    public List<String> getGrantedPermissions() {
        return grantedPermissions;
    }

    public List<String> getDeniedPermissions() {
        return deniedPermissions;
    }
}
