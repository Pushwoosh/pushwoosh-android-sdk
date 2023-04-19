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

package com.pushwoosh.inapp.view.strategy;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.strategy.model.ResourceType;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ResourceViewStrategyFactoryTest {

    private ResourceViewStrategyFactory resourceViewStrategyFactory;

    @Mock
    private ResourceWrapper resourceWrapper;
    @Mock
    private Resource resource;

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(resourceWrapper.getResource()).thenReturn(resource);

        resourceWrapper.getResource().isRequired();
        resourceViewStrategyFactory = new ResourceViewStrategyFactory();

        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void inAppRequiredViewStrategyTest() {
        when(resourceWrapper.getResourceType()).thenReturn(ResourceType.IN_APP);
        when(resource.isRequired()).thenReturn(true);
        ResourceViewStrategy resourceViewStrategy = resourceViewStrategyFactory.createStrategy(resourceWrapper);
        Assert.assertTrue(resourceViewStrategy instanceof InAppRequiredViewStrategy);
    }

    @Test
    public void inAppDefaultViewStrategyTest(){
        when(resourceWrapper.getResourceType()).thenReturn(ResourceType.IN_APP);
        ResourceViewStrategy resourceViewStrategy = resourceViewStrategyFactory.createStrategy(resourceWrapper);
        Assert.assertTrue(resourceViewStrategy instanceof InAppDefaultViewStrategy);

        when(resourceWrapper.getResource()).thenReturn(null);
        ResourceViewStrategy resourceViewStrategy2 = resourceViewStrategyFactory.createStrategy(resourceWrapper);
        Assert.assertTrue(resourceViewStrategy2 instanceof InAppDefaultViewStrategy);
    }

    @Test
    public void richMediaLockScreenViewStrategyTest(){
        when(resourceWrapper.getResourceType()).thenReturn(ResourceType.RICH_MEDIA);
        when(resourceWrapper.isLockScreen()).thenReturn(true);

        ResourceViewStrategy resourceViewStrategy = resourceViewStrategyFactory.createStrategy(resourceWrapper);
        Assert.assertTrue(resourceViewStrategy instanceof RichMediaLockScreenViewStrategy);

    }

    @Test
    public void richMediaViewStrategyTest(){
        when(resourceWrapper.getResourceType()).thenReturn(ResourceType.RICH_MEDIA);

        ResourceViewStrategy resourceViewStrategy = resourceViewStrategyFactory.createStrategy(resourceWrapper);
        Assert.assertTrue(resourceViewStrategy instanceof RichMediaViewStrategy);
    }
}