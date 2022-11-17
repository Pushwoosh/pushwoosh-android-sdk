package com.pushwoosh.baidu.internal.utils;

import androidx.annotation.NonNull;

import com.pushwoosh.baidu.internal.event.BaiduPermissionEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PermissionActivity;

public class BaiduPermissionActivity extends PermissionActivity {
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PWLog.debug(TAG, "onRequestPermissionsResult");

        switch (requestCode) {
            case REQUEST_CODE:
                handlePermissionsResult(permissions, grantResults);

                EventBus.sendEvent(new BaiduPermissionEvent(grantedPermissions, deniedPermissions));
                break;
            default:
                PWLog.warn(TAG, "Unrecognized request code " + requestCode);
                break;
        }

        finish();
    }
}
