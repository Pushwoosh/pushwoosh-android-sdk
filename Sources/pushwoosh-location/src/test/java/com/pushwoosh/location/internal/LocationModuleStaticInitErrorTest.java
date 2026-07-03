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

package com.pushwoosh.location.internal;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mockStatic;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * Regression guard for the <b>static-init</b> angle of crash-locationmodule-prefsprovider-null-static-init.
 * A null {@code prefsProvider} used to make the {@code static final geoZoneStorage} field at
 * {@code LocationModule:101} throw during {@code <clinit>}, which the JVM wrapped in an
 * {@link ExceptionInInitializerError} that crashed the host. The fix lets that field construct gracefully,
 * so class init now completes. Forcing {@code <clinit>} in isolation via {@code Class.forName(…, true, …)}
 * pins the guard to the static-init defect (vs the deep {@code nearestZonesManager()} graph).
 *
 * <p>Kept as its own single-test class because a static initializer runs once per classloader and all module
 * tests share one JVM — isolation guarantees {@code LocationModule} is class-loaded fresh exactly here.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class LocationModuleStaticInitErrorTest {

    private static final String LOCATION_MODULE_CLASS = "com.pushwoosh.location.internal.LocationModule";

    // Verifies that LocationModule.<clinit> (the :101 static GeoZoneStorage field) no longer throws
    // ExceptionInInitializerError when getPrefsProvider() is null — the class now initializes gracefully.
    @Test
    public void staticInitWithNullPrefsProvider_initializesGracefully() throws Exception {
        try (MockedStatic<AndroidPlatformModule> apm = mockStatic(AndroidPlatformModule.class)) {
            apm.when(AndroidPlatformModule::getPrefsProvider).thenReturn(null);
            apm.when(AndroidPlatformModule::getApplicationContext).thenReturn(null);

            // initialize=true forces <clinit> => static field :101
            // new GeoZoneStorage(getPrefsProvider()==null). With the fix this constructs an empty storage
            // instead of throwing, so the class loads cleanly (no ExceptionInInitializerError escapes).
            Class<?> loaded =
                    Class.forName(LOCATION_MODULE_CLASS, true, getClass().getClassLoader());
            assertNotNull("LocationModule must initialize without ExceptionInInitializerError", loaded);
        }
    }
}
