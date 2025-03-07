package com.pushwoosh.internal.utils;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.PermissionEvent;

/**
 * Activity which showing when SDK requires checked permissions
 *
 * This class is not abstract to keep a single entry point for location and notification permissions tests
 */
public class PermissionActivity extends TranslucentActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
	public static final String TAG = "PermissionActivity";

	public static final String EXTRA_PERMISSIONS = "extra_permissions";

	public static final int REQUEST_CODE = 1;

	public static List<String> grantedPermissions;
	public static List<String> deniedPermissions;

	public static void requestPermissions(Context context, String[] permissions) {
		boolean needRequestPermissions = false;
		try {
			for (String permission : permissions) {
				needRequestPermissions = needRequestPermissions || ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
			}
		} catch (Exception e) {
			PWLog.error("an error occurred while trying to requestPermissions", e);
		}

		if (needRequestPermissions) {
			PWLog.info(TAG, "Requesting permissions");
			Intent intent = new Intent(context, PermissionActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(EXTRA_PERMISSIONS, permissions);
			context.startActivity(intent);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		if (intent == null) {
			finish();
			return;
		}

		String[] permissions = (String[]) intent.getCharSequenceArrayExtra(EXTRA_PERMISSIONS);

		if (permissions == null) {
			finish();
			return;
		}

		ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case REQUEST_CODE:
				handlePermissionsResult(permissions, grantResults);

				EventBus.sendEvent(new PermissionEvent(grantedPermissions, deniedPermissions));
				break;
			default:
				PWLog.warn(TAG, "Unrecognized request code " + requestCode);
				break;
		}

		finish();
	}

	public static void handlePermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
		// If request is cancelled, the result arrays are empty.
		grantedPermissions = new ArrayList<>();
		deniedPermissions = new ArrayList<>();

		for (int i = 0; i < permissions.length; i++) {
			if (grantResults.length > i) {
				if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
					grantedPermissions.add(permissions[i]);
					continue;
				}
			}

			deniedPermissions.add(permissions[i]);
		}
	}
}
