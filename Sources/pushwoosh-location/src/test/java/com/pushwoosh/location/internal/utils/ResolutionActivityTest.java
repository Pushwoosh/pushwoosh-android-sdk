package com.pushwoosh.location.internal.utils;

import android.content.Context;
import android.content.Intent;
import android.util.AndroidRuntimeException;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import static com.pushwoosh.location.internal.utils.LocationConfig.LOCATION_LOW_INTERVAL;
import static org.mockito.Matchers.any;

@RunWith(RobolectricTestRunner.class)
public class ResolutionActivityTest {

    @Test
    public void resolutionSettingApi() {
        Context context = Mockito.mock(Context.class);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_LOW_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .build();

        Mockito.doThrow(new AndroidRuntimeException()).when(context).startActivity(any(Intent.class));

        ResolutionActivity.resolutionSettingApi(context, request);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        Mockito.verify(context).startActivity(intentArgumentCaptor.capture());
        Intent intent = intentArgumentCaptor.getValue();
        LocationSettingsRequest actualRequest = intent.getParcelableExtra(ResolutionActivity.KEY_SETTINGS_REQUEST);
        Assert.assertEquals(request, actualRequest);
    }
}