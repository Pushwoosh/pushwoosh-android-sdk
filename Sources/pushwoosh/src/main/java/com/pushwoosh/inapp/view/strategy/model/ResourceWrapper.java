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

package com.pushwoosh.inapp.view.strategy.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.utils.PWLog;

public class ResourceWrapper {

	private final Resource resource;
	private final String sound;
	private final boolean isLockScreen;
	private final ResourceType resourceType;
	private long delay;

	private ResourceWrapper(@Nullable Resource resource, @Nullable String sound, boolean isLockScreen, @NonNull ResourceType resourceType, long delay) {
		this.resource = resource;
		this.sound = sound;
		this.isLockScreen = isLockScreen;
		this.resourceType = resourceType;
		this.delay = delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public long getDelay() {
		return delay;
	}

	@Nullable
	public Resource getResource() {
		return resource;
	}

	@Nullable
	public String getSound() {
		return sound;
	}

	public boolean isLockScreen() {
		return isLockScreen;
	}

	@NonNull
	public ResourceType getResourceType() {
		return resourceType;
	}

	public static class Builder {
		private Resource resource;
		private String sound = "";
		private boolean isLockScreen = false;
		private ResourceType resourceType = ResourceType.IN_APP;
		private long delay = 0L;

		Builder setResourceType(ResourceType resourceType) {
			this.resourceType = resourceType;
			return this;
		}

		public Builder setResource(Resource resource) {
			this.resource = resource;
			return this;
		}

		public Builder setRichMedia(String richMedia) {
			if (richMedia == null) {
				return this;
			}

			Resource resource;
			try {
				resource = Resource.parseRichMedia(richMedia);
				return setResource(resource)
						.setResourceType(ResourceType.RICH_MEDIA);
			} catch (ResourceParseException e) {
				PWLog.error("Can't parse richMedia: " + richMedia, e);
			}

			return this;
		}

		public Builder setRemoteUrl(String remoteUrl) {
			if (remoteUrl == null) {
				return this;
			}

			return setResource(new Resource(remoteUrl))
					.setResourceType(ResourceType.REMOTE_URL);
		}

		public Builder setSound(String sound) {
			this.sound = sound;
			return this;
		}

		public Builder setLockScreen(boolean lockScreen) {
			isLockScreen = lockScreen;
			return this;
		}

		public Builder setDelay(long delay) {
			this.delay = delay;
			return this;
		}

		public ResourceWrapper build() {
			return new ResourceWrapper(resource, sound, isLockScreen, resourceType, delay);
		}
	}
}
