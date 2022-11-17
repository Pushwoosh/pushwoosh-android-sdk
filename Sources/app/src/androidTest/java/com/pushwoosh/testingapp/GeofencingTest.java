package com.pushwoosh.testingapp;

import android.Manifest;
import android.content.Context;
import android.location.LocationManager;

import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.location.PushwooshLocation;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class GeofencingTest extends BaseTest {
    public static final String MOCK_LOCATION_NAME = "mock_location";
    private MockLocationProvider mockLocationProvider;
    private Context applicationContext = AndroidPlatformModule.getApplicationContext();
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION);
    @Rule
    public GrantPermissionRule permissionRule2 = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);
    private MockLocationProvider mockGPS;
    private MockLocationProvider mockWifi;


    @Before
    public void setUp() {

        mockGPS = new MockLocationProvider(LocationManager.GPS_PROVIDER, AndroidPlatformModule.getApplicationContext());
        mockWifi = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, AndroidPlatformModule.getApplicationContext());
    }

    @After
    public void tearDown() {
        if (mockGPS != null) {
            mockGPS.shutdown();
        }
        if (mockWifi != null) {
            mockWifi.shutdown();
        }
    }

    /* Allowing app for mock locaiton

    adb shell appops set com.pushwoosh.testingapp android:mock_location allow
    Removing app for mock location

    adb shell appops set com.pushwoosh.testingapp android:mock_location deny
    Checking if app is set for mock location

    adb shell appops get com.pushwoosh.testingapp android:mock_location */

    // @Test(timeout = TIME_OUT)
    @Test
    @Ignore
    public void geoLocation() throws Exception {
        Pushwoosh.getInstance().registerForPushNotifications();
        wait(2);
        PushwooshLocation.startLocationTracking();
        wait(1);
        //   MockLocationProvider mock = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, applicationContext);





        mockGPS.pushLocation(0, 0, 0, 0);
        mockWifi.pushLocation(0, 0, 0 ,0);



        TimeUnit.SECONDS.sleep(10);
        assertEqualsTextMessage("message");

        PushwooshLocation.stopLocationTracking();
    }


}
