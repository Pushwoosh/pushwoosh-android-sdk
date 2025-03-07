package com.pushwoosh.location.internal.utils;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PermissionActivity;
import com.pushwoosh.location.internal.event.LocationPermissionEvent;

public class LocationPermissionActivity extends PermissionActivity {
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                handlePermissionsResult(permissions, grantResults);

                EventBus.sendEvent(new LocationPermissionEvent(grantedPermissions, deniedPermissions));
                break;
            default:
                PWLog.warn(TAG, "Unrecognized request code " + requestCode);
                break;
        }

        finish();
    }
}
