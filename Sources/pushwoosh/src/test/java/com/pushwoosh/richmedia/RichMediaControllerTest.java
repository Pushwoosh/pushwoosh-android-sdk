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

package com.pushwoosh.richmedia;



import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.inapp.event.RichMediaCloseEvent;
import com.pushwoosh.inapp.event.RichMediaErrorEvent;
import com.pushwoosh.inapp.event.RichMediaPresentEvent;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.strategy.ResourceViewStrategyFactory;
import com.pushwoosh.inapp.view.strategy.model.ResourceType;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.event.EventBus;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class RichMediaControllerTest {
    public static final PushwooshException TEST_EXCEPTION = new PushwooshException("TEST EXCEPTION");
    RichMediaController richMediaController;

    @Mock
    ResourceWrapper resourceWrapper;
    @Mock
    Resource resource;
    @Mock
    RichMedia richMedia;
    @Mock
    RichMediaFactory richMediaFactory;
    @Mock
    ResourceViewStrategyFactory resourceViewStrategyFactory;
    @Mock
    RichMediaPresentingDelegate richMediaPresentingDelegate;
    @Mock
    RichMediaStyle richMediaStyle;



    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(resource.getCode()).thenReturn("test code");
        when(resourceWrapper.getResource()).thenReturn(resource);
        when(resourceWrapper.getResourceType()).thenReturn(ResourceType.RICH_MEDIA);
        when(richMediaFactory.buildRichMedia(resourceWrapper)).thenReturn(richMedia);
        when(richMediaFactory.buildRichMedia(resource)).thenReturn(richMedia);
        when(richMedia.getResourceWrapper()).thenReturn(resourceWrapper);
        when(resourceWrapper.getResource()).thenReturn(resource);
        richMediaController = new RichMediaController(
                resourceViewStrategyFactory,
                richMediaFactory,
                richMediaStyle);
    }

    @Test
    public void simpleShowTest() {
        richMediaController.showResourceWrapper(resourceWrapper);
        verify(resourceViewStrategyFactory).showResource(resourceWrapper);
    }

    @Test
    public void manualShowTest() {
        when(richMedia.getResourceWrapper()).thenReturn(resourceWrapper);
        richMediaController.present(richMedia);
        verify(resourceViewStrategyFactory).showResource(resourceWrapper);
    }

    // SDK-866 regression guard: the controller no longer gates the delegate on resource type or
    // download state. An IN_APP resource — the case the removed isCanceled() gate used to swallow —
    // must now reach shouldPresent(); the content is fetched lazily downstream (mapToHtmlData ->
    // downloadIfNeeded), not gated here.
    @Test
    public void notRequiredInAppReachesDelegateTest() {
        when(resourceWrapper.getResourceType()).thenReturn(ResourceType.IN_APP);

        richMediaController.setDelegate(richMediaPresentingDelegate);
        richMediaController.showResourceWrapper(resourceWrapper);

        verify(richMediaPresentingDelegate).shouldPresent(any());
    }

    @Test
    public void shouldPresentOnlyTest() {
        richMediaController.setDelegate(richMediaPresentingDelegate);
        richMediaController.showResourceWrapper(resourceWrapper);
        ArgumentCaptor<RichMedia> argumentCaptor = ArgumentCaptor.forClass(RichMedia.class);

        verify(richMediaPresentingDelegate).shouldPresent(argumentCaptor.capture());
        Assert.assertEquals(1, argumentCaptor.getAllValues().size());
        Assert.assertEquals(resourceWrapper, argumentCaptor.getValue().getResourceWrapper());

        verify(richMediaPresentingDelegate, never()).onClose(any());
        verify(richMediaPresentingDelegate, never()).onPresent(any());
        verify(richMediaPresentingDelegate, never()).onError(any(), any(PushwooshException.class));
    }

    // Verifies that when delegate.shouldPresent returns true, the wrapper is forwarded to the strategy factory.
    @Test
    public void delegateApprovesShowTest() {
        when(richMediaPresentingDelegate.shouldPresent(any())).thenReturn(true);

        richMediaController.setDelegate(richMediaPresentingDelegate);
        richMediaController.showResourceWrapper(resourceWrapper);

        verify(richMediaPresentingDelegate).shouldPresent(any());
        verify(resourceViewStrategyFactory).showResource(resourceWrapper);
    }

    // Verifies that exceptions from delegate.shouldPresent are swallowed and the resource is not displayed.
    @Test
    public void delegateThrowsDuringShouldPresentTest() {
        when(richMediaPresentingDelegate.shouldPresent(any())).thenThrow(new RuntimeException("boom"));

        richMediaController.setDelegate(richMediaPresentingDelegate);
        richMediaController.showResourceWrapper(resourceWrapper);

        verify(richMediaPresentingDelegate).shouldPresent(any());
        verify(resourceViewStrategyFactory, never()).showResource(any());
    }

    // Verifies that events for remote-URL resources (empty code) skip the delegate.
    @Test
    public void remoteUrlEventSkipsDelegateTest() {
        Resource remoteResource = mock(Resource.class);
        when(remoteResource.getCode()).thenReturn("");

        richMediaController.setDelegate(richMediaPresentingDelegate);
        EventBus.sendEvent(new RichMediaPresentEvent(remoteResource));
        EventBus.sendEvent(new RichMediaErrorEvent(remoteResource, TEST_EXCEPTION));
        EventBus.sendEvent(new RichMediaCloseEvent(remoteResource));

        verify(richMediaPresentingDelegate, never()).onPresent(any());
        verify(richMediaPresentingDelegate, never()).onClose(any());
        verify(richMediaPresentingDelegate, never()).onError(any(), any());
    }

    @Test
    public void testDelegate() {
        richMediaController.setDelegate(richMediaPresentingDelegate);
        richMediaController.showResourceWrapper(resourceWrapper);
        EventBus.sendEvent(new RichMediaPresentEvent(resource));
        EventBus.sendEvent(new RichMediaErrorEvent(resource, TEST_EXCEPTION));
        EventBus.sendEvent(new RichMediaCloseEvent(resource));

        checkShouldPresent();
        checkOnPresent();
        checkOnClose();
        checkOnError();
    }

    private void checkOnError() {
        ArgumentCaptor<RichMedia> argumentCaptor = ArgumentCaptor.forClass(RichMedia.class);
        ArgumentCaptor<PushwooshException> argumentCaptorError = ArgumentCaptor.forClass(PushwooshException.class);

        verify(richMediaPresentingDelegate).onError(argumentCaptor.capture(), argumentCaptorError.capture());
        Assert.assertEquals(1, argumentCaptorError.getAllValues().size());
        Assert.assertEquals(TEST_EXCEPTION, argumentCaptorError.getValue());

        Assert.assertEquals(1, argumentCaptor.getAllValues().size());
        Assert.assertEquals(resource, argumentCaptor.getAllValues().get(0).getResourceWrapper().getResource());
    }

    private void checkShouldPresent() {
        ArgumentCaptor<RichMedia> argumentCaptor = ArgumentCaptor.forClass(RichMedia.class);

        verify(richMediaPresentingDelegate).shouldPresent(argumentCaptor.capture());
        Assert.assertEquals(1, argumentCaptor.getAllValues().size());
        Assert.assertEquals(resource, argumentCaptor.getAllValues().get(0).getResourceWrapper().getResource());
    }

    private void checkOnClose() {
        ArgumentCaptor<RichMedia> argumentCaptorClose = ArgumentCaptor.forClass(RichMedia.class);

        verify(richMediaPresentingDelegate).onClose(argumentCaptorClose.capture());
        Assert.assertEquals(1, argumentCaptorClose.getAllValues().size());
        Assert.assertEquals(resource, argumentCaptorClose.getAllValues().get(0).getResourceWrapper().getResource());
    }

    private void checkOnPresent() {
        ArgumentCaptor<RichMedia> argumentCaptorPresent = ArgumentCaptor.forClass(RichMedia.class);

        verify(richMediaPresentingDelegate).onPresent(argumentCaptorPresent.capture());
        Assert.assertEquals(1, argumentCaptorPresent.getAllValues().size());
        Assert.assertEquals(resource, argumentCaptorPresent.getAllValues().get(0).getResourceWrapper().getResource());
    }

}