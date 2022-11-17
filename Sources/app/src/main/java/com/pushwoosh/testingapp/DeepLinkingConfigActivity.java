/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.testingapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.testingapp.helpers.AppPreferences;

public class DeepLinkingConfigActivity extends AppCompatActivity {
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
		String apiUrl = uri.getQueryParameter("url");
		String appcode = uri.getQueryParameter("appcode");
		String senderId = uri.getQueryParameter("senderid");
		if (appcode != null)
			Pushwoosh.getInstance().setAppId(appcode);
		if (senderId != null)
			Pushwoosh.getInstance().setSenderId(senderId);
		if (apiUrl != null) {
			NetworkModule.getRequestManager().updateBaseUrl(apiUrl);
			if (AppPreferences.getSavedBool(AppPreferencesStrings.REGISTER_FOR_PUSH_NOTIFICATIONS_SWITCH).booleanValue())
				Pushwoosh.getInstance().registerForPushNotifications();
		}
	}
}
