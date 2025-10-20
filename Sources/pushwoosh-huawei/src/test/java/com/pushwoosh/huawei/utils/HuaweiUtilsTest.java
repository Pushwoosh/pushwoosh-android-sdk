package com.pushwoosh.huawei.utils;

import android.content.Context;
import android.os.Build;

import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiApiAvailability;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HuaweiUtils device detection logic.
 * <p>
 * Tests cover the 3-level validation approach:
 * 1. Manufacturer/Brand check (contains "huawei" or "honor")
 * 2. HMS Core availability
 * 3. GMS prioritization (prefer GMS over HMS)
 * <p>
 * Test scenarios based on expected device behavior:
 * <pre>
 * | Scenario                      | Manufacturer  | HMS | GMS | Result |
 * |-------------------------------|---------------|-----|-----|--------|
 * | Non-Huawei + HMS + GMS        | Samsung       | ✅  | ✅  | false  |
 * | Huawei + HMS + GMS (old)      | Huawei        | ✅  | ✅  | false  |
 * | Huawei + HMS, no GMS (new)    | Huawei|Honor  | ✅  | ❌  | true   |
 * </pre>
 */
@RunWith(RobolectricTestRunner.class)
public class HuaweiUtilsTest {

    @Mock
    private Context mockContext;

    @Mock
    private HuaweiApiAvailability mockHuaweiApiAvailability;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ========================================================================
    // Main Scenarios (based on table)
    // ========================================================================

    /**
     * Scenario 1: Non-Huawei device with HMS + GMS → false
     * Example: Samsung with HMS Core installed from Google Play
     */
    @Test
    public void testNonHuaweiWithHmsAndGms_shouldReturnFalse() throws Exception {
        setManufacturerAndBrand("samsung", "samsung");

        try (MockedStatic<HuaweiApiAvailability> mockedStatic = mockStatic(HuaweiApiAvailability.class)) {
            mockedStatic.when(HuaweiApiAvailability::getInstance).thenReturn(mockHuaweiApiAvailability);
            when(mockHuaweiApiAvailability.isHuaweiMobileServicesAvailable(any(Context.class)))
                    .thenReturn(ConnectionResult.SUCCESS);

            boolean result = HuaweiUtils.isHuaweiDevice(mockContext);

            assertFalse("Non-Huawei device should return false even with HMS installed", result);
        }
    }

    /**
     * Scenario 2: Huawei device with HMS + GMS (old devices) → false (GMS priority)
     * Example: Huawei P30 (2019) with both GMS and HMS
     */
    @Test
    public void testHuaweiWithHmsAndGms_shouldReturnFalse() throws Exception {
        setManufacturerAndBrand("HUAWEI", "HUAWEI");

        try (MockedStatic<HuaweiApiAvailability> hmsStatic = mockStatic(HuaweiApiAvailability.class);
             MockedStatic<HuaweiUtils> utilsStatic = mockStatic(HuaweiUtils.class)) {

            // Mock HMS availability
            hmsStatic.when(HuaweiApiAvailability::getInstance).thenReturn(mockHuaweiApiAvailability);
            when(mockHuaweiApiAvailability.isHuaweiMobileServicesAvailable(any(Context.class)))
                    .thenReturn(ConnectionResult.SUCCESS);

            // Mock GMS availability (return true)
            utilsStatic.when(() -> HuaweiUtils.isGooglePlayServicesAvailable(any(Context.class)))
                    .thenReturn(true);

            // Call the real isHuaweiDevice method
            utilsStatic.when(() -> HuaweiUtils.isHuaweiDevice(any(Context.class)))
                    .thenCallRealMethod();

            boolean result = HuaweiUtils.isHuaweiDevice(mockContext);

            assertFalse("Huawei device with GMS should prioritize GMS over HMS", result);
        }
    }

    /**
     * Scenario 3: Huawei|Honor device with HMS, no GMS (new devices) → true
     * Example: Huawei P40/P50 (2020+) without GMS
     */
    @Test
    public void testHuaweiWithHmsOnly_shouldReturnTrue() throws Exception {
        setManufacturerAndBrand("Huawei", "Huawei");

        try (MockedStatic<HuaweiApiAvailability> mockedStatic = mockStatic(HuaweiApiAvailability.class)) {
            mockedStatic.when(HuaweiApiAvailability::getInstance).thenReturn(mockHuaweiApiAvailability);
            when(mockHuaweiApiAvailability.isHuaweiMobileServicesAvailable(any(Context.class)))
                    .thenReturn(ConnectionResult.SUCCESS);

            boolean result = HuaweiUtils.isHuaweiDevice(mockContext);

            assertTrue("Huawei device with HMS only should return true", result);
        }
    }

    /**
     * Scenario 3: Huawei|Honor device with HMS, no GMS (new devices) → true
     * Example: Honor Magic6 without GMS
     */
    @Test
    public void testHonorWithHmsOnly_shouldReturnTrue() throws Exception {
        setManufacturerAndBrand("honor", "honor");

        try (MockedStatic<HuaweiApiAvailability> mockedStatic = mockStatic(HuaweiApiAvailability.class)) {
            mockedStatic.when(HuaweiApiAvailability::getInstance).thenReturn(mockHuaweiApiAvailability);
            when(mockHuaweiApiAvailability.isHuaweiMobileServicesAvailable(any(Context.class)))
                    .thenReturn(ConnectionResult.SUCCESS);

            boolean result = HuaweiUtils.isHuaweiDevice(mockContext);

            assertTrue("Honor device with HMS only should return true", result);
        }
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    /**
     * HMS SDK not integrated (NoClassDefFoundError) → false
     */
    @Test
    public void testHmsNotIntegrated_shouldReturnFalse() throws Exception {
        setManufacturerAndBrand("Huawei", "Huawei");

        try (MockedStatic<HuaweiApiAvailability> mockedStatic = mockStatic(HuaweiApiAvailability.class)) {
            mockedStatic.when(HuaweiApiAvailability::getInstance)
                    .thenThrow(new NoClassDefFoundError("HMS SDK not found"));

            boolean result = HuaweiUtils.isHuaweiDevice(mockContext);

            assertFalse("Device without HMS SDK integrated should return false", result);
        }
    }

    /**
     * BRAND check: device with Huawei brand but different manufacturer
     */
    @Test
    public void testBrandCheck_shouldReturnTrue() throws Exception {
        setManufacturerAndBrand("SomeOEM", "Huawei");

        try (MockedStatic<HuaweiApiAvailability> mockedStatic = mockStatic(HuaweiApiAvailability.class)) {
            mockedStatic.when(HuaweiApiAvailability::getInstance).thenReturn(mockHuaweiApiAvailability);
            when(mockHuaweiApiAvailability.isHuaweiMobileServicesAvailable(any(Context.class)))
                    .thenReturn(ConnectionResult.SUCCESS);

            boolean result = HuaweiUtils.isHuaweiDevice(mockContext);

            assertTrue("Device with Huawei BRAND should be detected", result);
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Sets Build.MANUFACTURER and Build.BRAND using Robolectric's ReflectionHelpers
     */
    private void setManufacturerAndBrand(String manufacturer, String brand) {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", manufacturer);
        ReflectionHelpers.setStaticField(Build.class, "BRAND", brand);
    }
}
