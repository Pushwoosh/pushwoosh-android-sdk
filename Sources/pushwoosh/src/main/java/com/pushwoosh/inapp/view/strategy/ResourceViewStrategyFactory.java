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

package com.pushwoosh.inapp.view.strategy;

import android.content.Context;

import androidx.annotation.Nullable;

import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;

/**
 * Factory that selects display strategy based on resource type and configuration.
 */
public class ResourceViewStrategyFactory {
	private static final String TAG = "[InApp]ResourceViewStrategyFactory";

	/**
	 * Returns strategy based on type: RICH_MEDIA → RichMediaViewStrategy, IN_APP → InAppDefaultViewStrategy.
	 */
	@Nullable
	public ResourceViewStrategy createStrategy(ResourceWrapper resourceWrapper) {
		if (getContext() == null) {
			PWLog.error(TAG, "context is null");
			return null;
		}

		switch (resourceWrapper.getResourceType()) {
			case IN_APP:
				if (resourceWrapper.getResource() != null && resourceWrapper.getResource().isRequired()) {
					return new InAppRequiredViewStrategy(getContext());
				}

				return new InAppDefaultViewStrategy(getContext());
			case RICH_MEDIA:
				if (resourceWrapper.isLockScreen()) {
					return new RichMediaLockScreenViewStrategy(getContext(), resourceWrapper.getSound());
				}

				return new RichMediaViewStrategy(getContext(), resourceWrapper.getDelay());
			default:
				return new InAppDefaultViewStrategy(getContext());
		}
	}

	/**
	 * Selects strategy and triggers display.
	 */
	public void showResource(ResourceWrapper resourceWrapper) {
		try {
			String resourceCode = resourceWrapper.getResource() != null
					? resourceWrapper.getResource().getCode()
					: "unknown";
			PWLog.noise(TAG, String.format("Starting to showResource, code: %s, type: %s",
					resourceCode, resourceWrapper.getResourceType()));

			ResourceViewStrategy strategy = createStrategy(resourceWrapper);
			if (strategy != null) {
				PWLog.noise(TAG, String.format("Selected strategy: %s for resource: %s",
						strategy.getClass().getSimpleName(), resourceCode));
				strategy.show(resourceWrapper.getResource());
			} else {
				PWLog.warn(TAG, String.format("No strategy created for resource: %s", resourceCode));
			}
		} catch (Throwable t) {
			PWLog.error(TAG, "Failed to show resource", t);
		}
	}

	@Nullable
	private Context getContext() {
		return AndroidPlatformModule.getApplicationContext();
	}
}
