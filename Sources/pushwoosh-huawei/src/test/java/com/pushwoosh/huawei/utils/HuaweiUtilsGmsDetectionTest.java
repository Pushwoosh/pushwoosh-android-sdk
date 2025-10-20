package com.pushwoosh.huawei.utils;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class HuaweiUtilsGmsDetectionTest {

    private static final String FAKE_GMS_CLASS =
            "com.pushwoosh.huawei.utils.HuaweiUtilsGmsDetectionTest$FakeGoogleApiAvailability";

    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        FakeGoogleApiAvailability.reset();
    }

    // Verifies that method returns false when GMS class is not found (ClassNotFoundException).
    @Test
    public void testGmsNotIntegrated_returnsFalse() {
        boolean result = HuaweiUtils.checkPlayServicesAvailability(
                "com.google.NonExistentClass",
                mockContext
        );

        assertFalse("Should return false when GMS class not found", result);
    }

    // Verifies that method returns false when mocked GMS returns SERVICE_MISSING error code.
    @Test
    public void testGmsServiceMissing_returnsFalse() {
        FakeGoogleApiAvailability.setResultCode(1);

        boolean result = HuaweiUtils.checkPlayServicesAvailability(FAKE_GMS_CLASS, mockContext);

        assertFalse("Should return false when GMS returns SERVICE_MISSING (1)", result);
    }

    // Verifies that method returns false when mocked GMS returns SERVICE_VERSION_UPDATE_REQUIRED error code.
    @Test
    public void testGmsVersionUpdateRequired_returnsFalse() {
        FakeGoogleApiAvailability.setResultCode(2);

        boolean result = HuaweiUtils.checkPlayServicesAvailability(FAKE_GMS_CLASS, mockContext);

        assertFalse("Should return false when GMS returns SERVICE_VERSION_UPDATE_REQUIRED (2)", result);
    }

    // Verifies that method returns false when mocked GMS returns SERVICE_DISABLED error code.
    @Test
    public void testGmsServiceDisabled_returnsFalse() {
        FakeGoogleApiAvailability.setResultCode(3);

        boolean result = HuaweiUtils.checkPlayServicesAvailability(FAKE_GMS_CLASS, mockContext);

        assertFalse("Should return false when GMS returns SERVICE_DISABLED (3)", result);
    }

    // Verifies that method returns true when mocked GMS returns SUCCESS (0).
    @Test
    public void testGmsAvailable_returnsTrue() {
        FakeGoogleApiAvailability.setResultCode(0);

        boolean result = HuaweiUtils.checkPlayServicesAvailability(FAKE_GMS_CLASS, mockContext);

        assertTrue("Should return true when GMS returns SUCCESS (0)", result);
    }

    // Verifies that method returns false when mocked GMS throws exception during invocation.
    @Test
    public void testGmsThrowsException_returnsFalse() {
        FakeGoogleApiAvailability.setThrowException(true);

        boolean result = HuaweiUtils.checkPlayServicesAvailability(FAKE_GMS_CLASS, mockContext);

        assertFalse("Should return false when GMS invocation throws exception", result);
    }

    // Verifies that method returns false when mocked GMS returns null result.
    @Test
    public void testGmsReturnsNull_returnsFalse() {
        FakeGoogleApiAvailability.setReturnNull(true);

        boolean result = HuaweiUtils.checkPlayServicesAvailability(FAKE_GMS_CLASS, mockContext);

        assertFalse("Should return false when GMS returns null", result);
    }

    public static class FakeGoogleApiAvailability {
        private static int resultCode = 0;
        private static boolean throwException = false;
        private static boolean returnNull = false;

        public static FakeGoogleApiAvailability getInstance() {
            return new FakeGoogleApiAvailability();
        }

        public Integer isGooglePlayServicesAvailable(Context context) throws Exception {
            if (throwException) {
                throw new RuntimeException("Simulated GMS error");
            }
            if (returnNull) {
                return null;
            }
            return resultCode;
        }

        static void reset() {
            resultCode = 0;
            throwException = false;
            returnNull = false;
        }

        static void setResultCode(int code) {
            reset();
            resultCode = code;
        }

        static void setThrowException(boolean value) {
            reset();
            throwException = value;
        }

        static void setReturnNull(boolean value) {
            reset();
            returnNull = value;
        }
    }
}
