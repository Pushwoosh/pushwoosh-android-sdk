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

import android.text.TextUtils;

import com.pushwoosh.inapp.event.RichMediaCloseEvent;
import com.pushwoosh.inapp.event.RichMediaErrorEvent;
import com.pushwoosh.inapp.event.RichMediaPresentEvent;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.strategy.ResourceViewStrategyFactory;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;

/**
 * Convergence point for all Rich Media and In-App display flows.
 * All entry points (push click, inbox, silent push, postEvent) pass through here.
 */
public class RichMediaController {
    private final String TAG = RichMediaController.class.getSimpleName();

    private final ResourceViewStrategyFactory resourceViewStrategyFactory;
    private final RichMediaFactory richMediaFactory;

    private volatile RichMediaPresentingDelegate delegate;
    private final RichMediaStyle richMediaStyle;

    public RichMediaController(
            ResourceViewStrategyFactory resourceViewStrategyFactory,
            RichMediaFactory richMediaFactory,
            RichMediaStyle richMediaStyle) {
        this.richMediaStyle = richMediaStyle;
        this.resourceViewStrategyFactory = resourceViewStrategyFactory;
        this.richMediaFactory = richMediaFactory;

        EventBus.subscribe(RichMediaCloseEvent.class, this::onCloseRichMedia);
        EventBus.subscribe(RichMediaPresentEvent.class, this::onPresentRichMedia);
        EventBus.subscribe(RichMediaErrorEvent.class, this::onErrorRichMedia);
    }

    /**
     * Main entry point for displaying Rich Media or In-App content.
     * If delegate is set, calls shouldPresent() first to allow cancellation.
     */
    public void showResourceWrapper(ResourceWrapper resourceWrapper) {
        PWLog.noise(TAG, "showResourceWrapper()");
        if (resourceWrapper == null) {
            PWLog.warn(TAG, "resourceWrapper is null, aborting show");
            return;
        }

        if (resourceWrapper.getResource() == null) {
            PWLog.warn(TAG, "resource is null, aborting show");
            return;
        }
        if (delegate != null) {
            PWLog.info(TAG, "using delegate to handle show process");
            useDelegate(resourceWrapper);
        } else {
            resourceViewStrategyFactory.showResource(resourceWrapper);
        }
    }

    /**
     * Public API for showing RichMedia. Use after delegate.shouldPresent() returns false.
     */
    public void present(RichMedia richMedia) {
        PWLog.noise(TAG, "present()");

        if (richMedia == null) {
            PWLog.warn(TAG, "richMedia is null");
            return;
        }

        resourceViewStrategyFactory.showResource(richMedia.getResourceWrapper());
    }

    /**
     * Sets delegate to customize presentation: shouldPresent, onPresent, onClose, onError.
     */
    public void setDelegate(RichMediaPresentingDelegate delegate) {
        this.delegate = delegate;
    }

    public RichMediaStyle getRichMediaStyle() {
        return richMediaStyle;
    }

    private void onPresentRichMedia(RichMediaPresentEvent event) {
        PWLog.noise(TAG, "onPresentRichMedia()");

        if (delegate == null) {
            return;
        }

        Resource resource = event.getResource();
        if (resource == null) {
            PWLog.warn(TAG, "resource in event is null");
            return;
        }

        if (isRemoteUrl(resource)) {
            return;
        }

        try {
            RichMedia richMedia = richMediaFactory.buildRichMedia(resource);
            delegate.onPresent(richMedia);
        } catch (Exception e) {
            PWLog.error(TAG, "Error in delegate.onPresent()", e);
        }
    }

    private void onErrorRichMedia(RichMediaErrorEvent event) {
        PWLog.noise(TAG, "onErrorRichMedia()");

        if (event == null) {
            return;
        }

        if (event.getException() != null) {
            PWLog.error(TAG, "RichMedia error", event.getException());
        }

        if (delegate == null) {
            return;
        }

        Resource resource = event.getResource();
        if (resource == null) {
            PWLog.warn(TAG, "resource in event is null");
            return;
        }

        if (isRemoteUrl(resource)) {
            return;
        }

        try {
            RichMedia richMedia = richMediaFactory.buildRichMedia(resource);
            delegate.onError(richMedia, event.getException());
        } catch (Exception e) {
            PWLog.error(TAG, "Error in delegate.onError()", e);
        }
    }

    private void onCloseRichMedia(RichMediaCloseEvent event) {
        PWLog.noise(TAG, "onCloseRichMedia()");

        if (delegate == null) {
            return;
        }

        Resource resource = event.getResource();
        if (resource == null) {
            PWLog.warn(TAG, "resource in event is null");
            return;
        }

        if (isRemoteUrl(resource)) {
            return;
        }

        try {
            RichMedia richMedia = richMediaFactory.buildRichMedia(resource);
            delegate.onClose(richMedia);
        } catch (Exception e) {
            PWLog.error(TAG, "Error in delegate.onClose()", e);
        }
    }

    private RichMedia buildRichMedia(ResourceWrapper resourceWrapper) {
        return richMediaFactory.buildRichMedia(resourceWrapper);
    }

    private void useDelegate(ResourceWrapper resourceWrapper) {
        try {
            boolean isWillShow = delegate.shouldPresent(buildRichMedia(resourceWrapper));
            if (isWillShow) {
                resourceViewStrategyFactory.showResource(resourceWrapper);
            }
        } catch (Exception e) {
            PWLog.error(TAG, "Error in delegate.shouldPresent()", e);
        }
    }

    private boolean isRemoteUrl(Resource resource) {
        if (TextUtils.isEmpty(resource.getCode())) {
            PWLog.noise(TAG, "code is empty, resource is not RichMedia, abort use delegate");
            return true;
        }
        return false;
    }
}
