/*
 *
 * Copyright (c) 2024. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.richmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;

import org.junit.Test;

public class RichMediaTest {

    // Verifies that an in-app resource is mapped to InAppSource with content from resource code.
    @Test
    public void constructor_inAppResource_mapsToInAppSource() {
        Resource resource = new Resource("in-app-code", true);
        ResourceWrapper wrapper = new ResourceWrapper.Builder()
                .setResource(resource)
                .setLockScreen(false)
                .build();

        RichMedia richMedia = new RichMedia(wrapper);

        assertEquals(RichMedia.Source.InAppSource, richMedia.getSource());
        assertEquals("in-app-code", richMedia.getContent());
        assertTrue(richMedia.isRequired());
        assertFalse(richMedia.isLockScreen());
        assertSame(wrapper, richMedia.getResourceWrapper());
    }

    // Verifies that a rich-media resource (code starts with "r-") is mapped to PushMessageSource.
    @Test
    public void constructor_richMediaResource_mapsToPushMessageSource() {
        Resource resource = new Resource("r-rich-media-code", false);
        ResourceWrapper wrapper = new ResourceWrapper.Builder()
                .setResource(resource)
                .setLockScreen(true)
                .build();

        RichMedia richMedia = new RichMedia(wrapper);

        assertEquals(RichMedia.Source.PushMessageSource, richMedia.getSource());
        assertEquals("r-rich-media-code", richMedia.getContent());
        assertFalse(richMedia.isRequired());
        assertTrue(richMedia.isLockScreen());
        assertSame(wrapper, richMedia.getResourceWrapper());
    }

    // Verifies that a null resource skips mapping but still applies lock-screen flag from wrapper.
    @Test
    public void constructor_nullResource_returnsEarlyWithLockScreenSet() {
        ResourceWrapper wrapper = new ResourceWrapper.Builder()
                .setLockScreen(true)
                .build();

        RichMedia richMedia = new RichMedia(wrapper);

        assertNull(richMedia.getContent());
        assertNull(richMedia.getSource());
        assertFalse(richMedia.isRequired());
        assertTrue(richMedia.isLockScreen());
        assertSame(wrapper, richMedia.getResourceWrapper());
    }

    // Verifies equals/hashCode contract based on (content, source) pair for the public DTO.
    @Test
    public void equalsAndHashCode_sameContentAndSource_areEqual() {
        ResourceWrapper wrapperA = new ResourceWrapper.Builder()
                .setResource(new Resource("in-app-code", true))
                .build();
        ResourceWrapper wrapperB = new ResourceWrapper.Builder()
                .setResource(new Resource("in-app-code", true))
                .build();
        ResourceWrapper wrapperOther = new ResourceWrapper.Builder()
                .setResource(new Resource("r-other-code", false))
                .build();

        RichMedia a = new RichMedia(wrapperA);
        RichMedia b = new RichMedia(wrapperB);
        RichMedia other = new RichMedia(wrapperOther);

        assertTrue(a.equals(a));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, other);
        assertFalse(a.equals(null));
        assertFalse(a.equals("string"));
    }
}
