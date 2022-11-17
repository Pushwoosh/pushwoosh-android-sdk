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

package com.pushwoosh.location.internal.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.AndroidRuntimeException;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;
import com.pushwoosh.internal.event.Event;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.TranslucentActivity;

import androidx.annotation.Nullable;

public class ResolutionActivity extends TranslucentActivity {
    private static final String TAG = "ResolutionActivity";
    public static final String KEY_SETTINGS_REQUEST = TAG + "KEY_SETTINGS_REQUEST";
    public static final int REQUEST_CHECK_SETTINGS = 1;

    public static void resolutionSettingApi(Context context, LocationSettingsRequest request) {
        PWLog.debug(TAG, "Request resolution");
        Intent intent = new Intent(context, ResolutionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_SETTINGS_REQUEST, request);
        try {
            context.startActivity(intent);
        } catch (AndroidRuntimeException e) {
            // ignore
        }

    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocationSettingsRequest request =
                (LocationSettingsRequest) getIntent().getParcelableExtra(KEY_SETTINGS_REQUEST);
        if (request == null) {
            finish();
            return;
        }
        // The only way to show resolutiom dialog is to use method startResolutionForResult of
        // ResolvableApiException. There is no better way to pass ResolvableApiException from
        // GoogleLocationProvider to this activity so we have to make the same task again here.
        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this).checkLocationSettings(request);
        task.addOnCompleteListener(completedTask -> {
            try {
                LocationSettingsResponse response = task.getResult(ApiException.class);
            } catch (ApiException exception) {
                if (exception.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        // Cast to a resolvable exception.
                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        resolvable.startResolutionForResult(
                                this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e) {
                        PWLog.error("Can't start resolution for status code" + exception.getStatusCode(), e);
                    } catch (ClassCastException e) {
                        // Ignore, should be an impossible error.
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        EventBus.sendEvent(new ResolutionEvent(true));
                        break;
                    case Activity.RESULT_CANCELED:
                        EventBus.sendEvent(new ResolutionEvent(false));
                        break;
                    default:
                        break;
                }
                break;
        }

        finish();
    }

    public static class ResolutionEvent implements Event {
        boolean success;

        ResolutionEvent(final boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
