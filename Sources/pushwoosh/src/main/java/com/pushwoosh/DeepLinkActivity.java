package com.pushwoosh;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import com.pushwoosh.internal.network.CreateTestDeviceRequest;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.TranslucentActivity;
import com.pushwoosh.repository.RepositoryModule;

import java.util.Locale;

public class DeepLinkActivity extends TranslucentActivity {
	private static final String TAG = "DeepLinkActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String action = intent.getAction();
		Uri data = intent.getData();

		if (TextUtils.equals(action, Intent.ACTION_VIEW)) {
			openUrl(data);
		}

		finish();
	}

	private void openUrl(Uri uri) {
		String scheme = uri.getScheme().toLowerCase(Locale.getDefault());
		String host = uri.getHost();

		if (scheme.startsWith("pushwoosh-")) {
			if (host.equals("createTestDevice")) {
				PWLog.debug(TAG, "createTestDevice");
				createTestDevice(getApplicationContext());
			} else {
				PWLog.error(TAG, "unrecognized pushwoosh command");
			}
		} else {
			PWLog.error(TAG, "This is not pushwoosh scheme");
		}
	}

	private void createTestDevice(final Context context) {
		CreateTestDeviceRequest request = new CreateTestDeviceRequest(DeviceUtils.getDeviceName(), "Imported from the app");
		RequestManager requestManager = NetworkModule.getRequestManager();
		if (requestManager == null) {
			Toast.makeText(context, "Test device registration has failed. RequestManager is null", Toast.LENGTH_LONG).show();
			return;
		}
		requestManager.sendRequest(request, result -> {
			if (result.isSuccess()) {
				Toast.makeText(context, "Test device has been registered.", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context, "Test device registration has failed. " + (result.getException() == null ? "" : result.getException().getMessage()), Toast.LENGTH_LONG).show();
			}
		});
	}
}
