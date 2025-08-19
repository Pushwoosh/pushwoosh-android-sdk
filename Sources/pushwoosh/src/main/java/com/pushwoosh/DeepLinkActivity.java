package com.pushwoosh;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.network.CreateTestDeviceRequest;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.TranslucentActivity;

import java.util.Locale;

public class DeepLinkActivity extends TranslucentActivity {
	private static final String TAG = "DeepLinkActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			Intent intent = getIntent();
			String action = intent.getAction();
			Uri data = intent.getData();

			if (data == null) {
				return;
			}

			if (TextUtils.equals(action, Intent.ACTION_VIEW)) {
				SdkStateProvider.getInstance().executeOrQueue(() -> {openUrl(data);});
			}

		} catch (Exception e) {
			PWLog.error(TAG, "can't open url", e);
		} finally {
			finish();

		}
	}

	private void openUrl(Uri uri) {
		PWLog.noise(TAG, "openUrl()");

		String scheme = uri.getScheme();
		String host = uri.getHost();
		if (scheme == null) {
			PWLog.warn(TAG, "scheme is null");
			return;
		}
		scheme = scheme.toLowerCase(Locale.getDefault());

		if (host == null) {
			PWLog.warn(TAG, "host is null");
			return;
		}

		if (!scheme.startsWith("pushwoosh-")) {
			PWLog.warn(TAG, "scheme is not belongs to pushwoosh");
			return;
		}

		if (host.equals("createTestDevice")) {
			createTestDevice(getApplicationContext());
		} else {
			PWLog.error(TAG, "unrecognized pushwoosh command");
		}
	}

	private void createTestDevice(final Context context) {
		PWLog.noise(TAG, "createTestDevice()");

		//todo: move to PushwooshRepository
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
