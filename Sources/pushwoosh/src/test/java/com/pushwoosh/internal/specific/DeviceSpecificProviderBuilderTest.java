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

package com.pushwoosh.internal.specific;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecificProvider.Builder;
import com.pushwoosh.internal.utils.PWLog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeviceSpecificProviderBuilderTest {

    @Mock
    private DeviceSpecific deviceSpecificA;

    @Mock
    private DeviceSpecific deviceSpecificB;

    @Mock
    private PushRegistrar pushRegistrarA;

    private AutoCloseable mocks;

    private final List<String> transportLines = Collections.synchronizedList(new ArrayList<>());

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        clearSingleton();
        transportLines.clear();
        PWLog.setLogsUpdateListener((level, message) -> {
            if (level == PWLog.Level.INFO && message.contains("PUSH TRANSPORT")) {
                transportLines.add(message);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        PWLog.setLogsUpdateListener(null);
        clearSingleton();
        mocks.close();
    }

    private void clearSingleton() throws Exception {
        Field f = DeviceSpecificProvider.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    // Verifies that build(false) throws IllegalArgumentException when deviceSpecific was not set.
    @Test
    public void build_deviceSpecificNotSetAndForceReplaceFalse_throwsIllegalArgumentException() {
        Builder builder = new Builder();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> builder.build(false));
        assertEquals("You must setup deviceSpecific", ex.getMessage());
    }

    // Verifies that build(true) also validates deviceSpecific — forceReplace does not bypass the null-check.
    @Test
    public void build_deviceSpecificNotSetAndForceReplaceTrue_throwsIllegalArgumentException() {
        Builder builder = new Builder();

        assertThrows(IllegalArgumentException.class, () -> builder.build(true));
    }

    // Verifies that first build(false) creates a singleton and exposes it via getInstance() / isInited().
    @Test
    public void build_firstCallWithDeviceSpecific_createsSingletonAndDelegatesPushRegistrar() {
        when(deviceSpecificA.pushRegistrar()).thenReturn(pushRegistrarA);

        DeviceSpecificProvider provider =
                new Builder().setDeviceSpecific(deviceSpecificA).build(false);

        assertNotNull(provider);
        assertSame(provider, DeviceSpecificProvider.getInstance());
        assertTrue(DeviceSpecificProvider.isInited());
        assertSame(pushRegistrarA, provider.pushRegistrar());
    }

    // Verifies that a second build(false) with a different deviceSpecific keeps the original singleton.
    @Test
    public void build_secondCallWithForceReplaceFalse_returnsExistingSingleton() {
        when(deviceSpecificA.type()).thenReturn("typeA");

        DeviceSpecificProvider first =
                new Builder().setDeviceSpecific(deviceSpecificA).build(false);
        DeviceSpecificProvider second =
                new Builder().setDeviceSpecific(deviceSpecificB).build(false);

        assertSame(first, second);
        assertSame(first, DeviceSpecificProvider.getInstance());
        assertEquals("typeA", second.type());
    }

    // Verifies that build(true) replaces the existing singleton with a new instance backed by the new deviceSpecific.
    @Test
    public void build_secondCallWithForceReplaceTrue_replacesSingleton() {
        when(deviceSpecificB.type()).thenReturn("typeB");

        DeviceSpecificProvider first =
                new Builder().setDeviceSpecific(deviceSpecificA).build(false);
        DeviceSpecificProvider replaced =
                new Builder().setDeviceSpecific(deviceSpecificB).build(true);

        assertNotSame(first, replaced);
        assertSame(replaced, DeviceSpecificProvider.getInstance());
        assertEquals("typeB", replaced.type());
    }

    // Verifies that the first write into the empty slot is logged as SET with the transport name and its device type.
    @Test
    public void build_firstWrite_logsTransportSet() {
        when(deviceSpecificA.type()).thenReturn("Android FCM");
        when(deviceSpecificA.deviceType()).thenReturn(3);

        new Builder().setDeviceSpecific(deviceSpecificA).build(true);

        assertEquals(1, transportLines.size());
        assertEquals("[DeviceSpecificProvider] PUSH TRANSPORT SET: Android FCM (device type 3)", transportLines.get(0));
    }

    // Verifies that taking the slot over with another transport logs CHANGED naming both the previous and the new one.
    // This is the only trace of the provider race: FirebaseInitializer writes FCM and Huawei overwrites it silently.
    @Test
    public void build_typeChanged_logsTransportChangedWithBothTransports() {
        when(deviceSpecificA.type()).thenReturn("Android FCM");
        when(deviceSpecificA.deviceType()).thenReturn(3);
        when(deviceSpecificB.type()).thenReturn("Huawei");
        when(deviceSpecificB.deviceType()).thenReturn(17);

        new Builder().setDeviceSpecific(deviceSpecificA).build(true);
        new Builder().setDeviceSpecific(deviceSpecificB).build(true);

        assertEquals(2, transportLines.size());
        assertEquals(
                "[DeviceSpecificProvider] PUSH TRANSPORT CHANGED: Android FCM (device type 3)"
                        + " -> Huawei (device type 17)",
                transportLines.get(1));
    }

    // Verifies that rewriting the slot with the same transport stays silent — the plugin rollback path re-runs
    // FirebaseInitializer on every start, and logging that would report a change on each launch.
    @Test
    public void build_sameTypeWrittenAgain_logsNothingBeyondTheFirstSet() {
        when(deviceSpecificA.type()).thenReturn("Android FCM");
        when(deviceSpecificA.deviceType()).thenReturn(3);
        when(deviceSpecificB.type()).thenReturn("Android FCM");
        when(deviceSpecificB.deviceType()).thenReturn(3);

        new Builder().setDeviceSpecific(deviceSpecificA).build(true);
        new Builder().setDeviceSpecific(deviceSpecificB).build(true);

        assertEquals(1, transportLines.size());
        assertTrue(transportLines.get(0).contains("PUSH TRANSPORT SET"));
    }
}
