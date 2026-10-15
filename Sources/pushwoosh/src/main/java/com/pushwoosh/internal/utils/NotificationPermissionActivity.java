package com.pushwoosh.internal.utils;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.NotificationPermissionEvent;
import com.pushwoosh.notification.PushPermissionStatusTracker;

public class NotificationPermissionActivity extends PermissionActivity {

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PWLog.debug(TAG, "onRequestPermissionsResult");

        switch (requestCode) {
            case REQUEST_CODE:
                handlePermissionsResult(permissions, grantResults);

                boolean granted = grantedPermissions.contains("android.permission.POST_NOTIFICATIONS");
                PushPermissionStatusTracker.onPermissionResult(this, granted);
                EventBus.sendEvent(new NotificationPermissionEvent(grantedPermissions, deniedPermissions));
                break;
            default:
                PWLog.warn(TAG, "Unrecognized request code " + requestCode);
                break;
        }

        finish();
    }
}
