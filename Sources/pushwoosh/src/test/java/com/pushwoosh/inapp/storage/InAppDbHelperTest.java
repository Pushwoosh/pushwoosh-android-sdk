/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.inapp.storage;

import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class InAppDbHelperTest {
    private InAppDbHelper inAppDbHelper;

    private Resource resource1;
    private Resource resource2;
    private Resource resourceConsent;
    private Resource resourceDelete;

    @Before
    public void setUp() throws Exception {
        inAppDbHelper = new InAppDbHelper(RuntimeEnvironment.application);

        resource1 = new Resource("code1","url1", "hash1", 1L,  InAppLayout.DIALOG, null, false, 3, null, "");
        resource2 = new Resource("code2","url2", "hash2", 2L,  InAppLayout.BOTTOM, null, true, 2, null, "");
        resourceConsent = new Resource("code3","url3", "hash3", 3L,  InAppLayout.FULLSCREEN, null, true, 1,  null, "Consent");
        resourceDelete = new Resource("code4","url4", "hash4", 4L,  InAppLayout.TOP, null, false, 2,  null, "Delete");

        List<Resource> resourceList = Arrays.asList(resource1, resource2, resourceConsent, resourceDelete);

        inAppDbHelper.saveOrUpdateResources(resourceList);
    }

    @After
    public void tearDown() throws Exception {
        inAppDbHelper.close();
    }

    @Test
    public void getResource() {
        Resource resource = inAppDbHelper.getResource("code2");
        Assert.assertEquals(resource2, resource);
        resource = inAppDbHelper.getResource("code1");
        Assert.assertEquals(resource1, resource);
    }

    @Test
    public void getResourceGDPRConsent() {
        Resource resource = inAppDbHelper.getResourceGDPRConsent();
        Assert.assertEquals(resourceConsent, resource);
    }

    @Test
    public void getResourceGDPRDeletion() {
        Resource resource = inAppDbHelper.getResourceGDPRDeletion();
        Assert.assertEquals(resourceDelete,  resource);
    }
}