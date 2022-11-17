package com.pushwoosh.testingapp;

import android.Manifest;
import android.content.Context;
import android.location.LocationManager;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.Log;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LocationTest extends BaseTest {
    @ClassRule public static GrantPermissionRule permissionRule2 = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);
    private static final int TIMEOUT_FIVE_MINUTES = 5 * 60 * 1000;
    private MockLocationProvider mockLocationProvider;
    private Context context = AndroidPlatformModule.getApplicationContext();

    @BeforeClass
    public static void setUpClass() {
        Log.d(BaseTest.TAG, "LocationTest start");
    }

    @AfterClass
    public static void tearDownClass() {
        Log.d(BaseTest.TAG, "LocationTest end");
    }

    @Before
    public void setUp() {
        clearPushHistory();
    }

    @After
    public void tearDown() {
        if (mockLocationProvider != null) {
            mockLocationProvider.shutdown();
            mockLocationProvider = null;
        }
        stopTrackingLocation();
    }

    @Test(timeout = TIMEOUT_FIVE_MINUTES)
    public void testZone1() throws Exception {
        final double testLocationLat = 20.68296;
        final double testLocationLon = -88.56867;

        assertTrue(registerForPushMessages());
        TimeUnit.SECONDS.sleep(5);
        assertTrue(startTrackingLocation());

        final String testLocationMessage = "You are in testZone!";
        Log.d(BaseTest.TAG, "TestZone1");
        startTestLocationProviderLoop(LocationManager.GPS_PROVIDER, testLocationLat, testLocationLon, testLocationMessage);
        Log.d(BaseTest.TAG, "TestZone1 done");
    }

    private void startTestLocationProviderLoop(String providerName, double lat, double lon, String message) throws Exception {
        if (mockLocationProvider == null) {
            mockLocationProvider = new MockLocationProvider(providerName, context);
        }
        while (!isOneOfPushMessagesEqualsText(message)) {
            // reset location:
            mockLocationProvider.pushLocation(0, 0, 0, 0);
            TimeUnit.MILLISECONDS.sleep(2000);
            // push location for 10 seconds:
            for (int i = 0; i < 100; i++) {
                mockLocationProvider.pushLocation(lat, lon, 0, 0);
                TimeUnit.MILLISECONDS.sleep(100);
            }
        }
    }
}