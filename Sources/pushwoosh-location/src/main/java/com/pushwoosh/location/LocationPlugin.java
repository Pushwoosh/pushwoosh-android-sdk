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

package com.pushwoosh.location;

import java.util.Collection;
import java.util.Collections;

import com.pushwoosh.BootReceiver;
import com.pushwoosh.location.foregroundservice.ForegroundServiceHelper;
import com.pushwoosh.location.foregroundservice.ForegroundServiceHelperRepository;
import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.internal.LocationModule;
import com.pushwoosh.location.internal.checker.GcmLibraryChecker;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.storage.LocationPrefs;

public class LocationPlugin implements Plugin {

	@Override
	public void init() {
		GcmLibraryChecker.checkGcmLibraries();
		PWLog.noise(LocationConfig.TAG, "init plugin");
		LocationModule.init();
		EventBus.subscribe(BootReceiver.DeviceBootedEvent.class, event -> {
			if (LocationModule.locationPrefs().geolocationStarted().get()) {
				PWLog.noise(LocationConfig.TAG, "boot receiver");
				startForegroundService();
				LocationModule.nearestZonesManager().deviceRebooted();
			}
		});
	}

    private void startForegroundService() {
        ForegroundServiceHelper foregroundServiceHelper =
                ForegroundServiceHelperRepository.getForegroundServiceHelper();
        if (foregroundServiceHelper != null) {
            foregroundServiceHelper.startService();
        }
    }

	@Override
	public Collection<? extends MigrationScheme> getPrefsMigrationSchemes(PrefsProvider prefsProvider) {
		return Collections.singleton(LocationPrefs.provideMigrationScheme(prefsProvider));
	}
}
