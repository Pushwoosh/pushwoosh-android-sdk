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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

public class DeviceSpecificProviderBuilderTest {

    @Mock
    private DeviceSpecific deviceSpecificA;

    @Mock
    private DeviceSpecific deviceSpecificB;

    @Mock
    private PushRegistrar pushRegistrarA;

    private AutoCloseable mocks;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        clearSingleton();
    }

    @After
    public void tearDown() throws Exception {
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
}
