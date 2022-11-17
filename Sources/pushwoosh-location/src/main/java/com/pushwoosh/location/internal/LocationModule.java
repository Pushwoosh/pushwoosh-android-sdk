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

package com.pushwoosh.location.internal;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.telecom.Call;

import androidx.annotation.Nullable;

import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.utils.ImageUtils;
import com.pushwoosh.internal.utils.PendingIntentUtils;
import com.pushwoosh.location.AndroidManifestLocationConfig;
import com.pushwoosh.location.foregroundservice.ForegroundServiceHelper;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.JobLocationIdProvider;
import com.pushwoosh.location.NearestZonesManager;
import com.pushwoosh.location.foregroundservice.ForegroundServiceHelperRepository;
import com.pushwoosh.location.geofence.GeoZonesUpdater;
import com.pushwoosh.location.geofence.GeofenceTracker;
import com.pushwoosh.location.geofence.GeofenceTrackerFactory;
import com.pushwoosh.location.geofencer.GeofenceReceiver;
import com.pushwoosh.location.geofencer.GeofenceStateChangedCallback;
import com.pushwoosh.location.geofencer.Geofencer;
import com.pushwoosh.location.geofencer.GeofencerFactory;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionDeclaredChecker;
import com.pushwoosh.location.internal.checker.FineLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;
import com.pushwoosh.location.internal.checker.LocationStateChecker;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.network.repository.UpdateNearestRepository;
import com.pushwoosh.location.scheduler.ServiceScheduler;
import com.pushwoosh.location.storage.GeoZoneStorage;
import com.pushwoosh.location.storage.LocationPrefs;
import com.pushwoosh.location.storage.NearestZonesStorage;
import com.pushwoosh.location.tracker.LocationTracker;
import com.pushwoosh.location.tracker.LocationTrackerCallback;
import com.pushwoosh.location.tracker.LocationTrackerFactory;
import com.pushwoosh.location.tracker.LocationUpdateListener;

import java.util.List;

/**
 * This class contains all needed singletons which used in location module
 */
@SuppressLint("StaticFieldLeak")
public class LocationModule {
    private static volatile LocationPrefs locationPrefs;
    private static final Object LOCATION_PREFS_MUTEX = new Object();

    private static volatile NearestZonesManager nearestZonesManager;
    private static final Object NEAREST_ZONES_MANAGER_MUTEX = new Object();

    private static volatile GeofenceTracker geofenceTracker;
    private static final Object GEOFENCE_TRACKER_MUTEX = new Object();

    private static volatile LocationTracker locationTracker;
    private static final Object LOCATION_MUTEX = new Object();

    private static volatile Geofencer geofencer;
    private static final Object GEOFENCER_MUTEX = new Object();

    private static volatile UpdateNearestRepository updateNearestRepository;
    private static final Object UPDATE_NEAREST_REPOSITORY_MUTEX = new Object();

    private static final NearestZonesStorage nearestZonesStorage = new NearestZonesStorage();
    private static final GeoZoneStorage geoZoneStorage = new GeoZoneStorage(AndroidPlatformModule.getPrefsProvider());

    private static final JobLocationIdProvider jobIdProvider = new JobLocationIdProvider();
    private static final LocationPermissionChecker locationPermissionChecker = new LocationPermissionChecker(getApplicationContext());
    private static final FineLocationPermissionChecker fineLocationPermissionChecker = new FineLocationPermissionChecker(getApplicationContext());
    private static final BackgroundLocationPermissionDeclaredChecker backgroundLocationPermissionDeclaredChecker = new BackgroundLocationPermissionDeclaredChecker(getApplicationContext());
    private static final BackgroundLocationPermissionChecker backgroundLocationPermissionChecker = new BackgroundLocationPermissionChecker(getApplicationContext());
    private static final LocationStateChecker locationStateChecker = new LocationStateChecker(getApplicationContext());
    private static final ServiceScheduler serviceScheduler = new ServiceScheduler(getApplicationContext());

    private static final GeoZonesUpdaterWrapper geoZonesUpdaterWrapper = new GeoZonesUpdaterWrapper();
    private static final LocationTrackerCallbackWrapper locationTrackerCallback = new LocationTrackerCallbackWrapper();
    private static final GeofenceStateChangedCallbackWrapper geofenceStateChangedCallbackWrapper = new GeofenceStateChangedCallbackWrapper();
    private static final LocationUpdateListenerWrapper locationUpdateListener = new LocationUpdateListenerWrapper();


    private static final Object PENDING_INTENT_MUTEX = new Object();
    private static PendingIntent pendingIntent;
    private static final Object GEOFENCING_CLIENT = new Object();
    private static GeofencingClient geofencingClient;
    private static ForegroundServiceHelper foregroundServiceHelper;
    private static final Object FOREGROUND_SERVICE_HELPER_MUTEX = new Object();

    public static void init() {
        nearestZonesManager();
    }

    @Nullable
    private static GeofencingClient createGeofencingClient() {
        if (geofencingClient == null) {
            synchronized (GEOFENCING_CLIENT) {
                if (geofencingClient == null) {
                    geofencingClient = createGeofencingClientNotThreadSafely();
                }
            }
        }
        return geofencingClient;
    }

    @Nullable
    private static GeofencingClient createGeofencingClientNotThreadSafely() {
        Context applicationContext = getApplicationContext();
        if (applicationContext != null)
            return LocationServices.getGeofencingClient(applicationContext);
        else {
            return null;
        }
    }

    private static PendingIntent createPendingIntent() {
        if (pendingIntent == null) {
            synchronized (PENDING_INTENT_MUTEX) {
                if (pendingIntent == null) {
                    pendingIntent = createPendingIntentNotThreadSefely();
                }
            }
        }
        return pendingIntent;
    }

    @Nullable
    private static PendingIntent createPendingIntentNotThreadSefely() {
        Context context = getApplicationContext();
        if (context == null) {
            return null;
        }
        Intent intent = new Intent(context, GeofenceReceiver.class);
        intent.setAction(GeofenceReceiver.getGeofenceAction(context));
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    @Nullable
    public static LocationTracker locationTracker() {
        if (locationTracker == null) {
            synchronized (LOCATION_MUTEX) {
                if (locationTracker == null) {
                    if (getApplicationContext() == null) {
                        return null;
                    }

                    locationTracker = LocationTrackerFactory.createLocationTracker(
                            getApplicationContext(),
                            locationTrackerCallback,
                            locationPermissionChecker,
                            locationStateChecker,
                            locationUpdateListener,
                            foregroundServiceHelper());
                }
            }
        }

        return locationTracker;
    }

    public static NearestZonesManager nearestZonesManager() {
        PWLog.noise(LocationConfig.TAG, "get nearestZonesManager");
        if (nearestZonesManager == null) {
            synchronized (NEAREST_ZONES_MANAGER_MUTEX) {
                if (nearestZonesManager == null) {
                    nearestZonesManager = new NearestZonesManager(
                            geofenceTracker(),
                            locationPrefs(),
                            locationTracker(),
                            locationPermissionChecker,
                            fineLocationPermissionChecker,
                            backgroundLocationPermissionDeclaredChecker,
                            backgroundLocationPermissionChecker,
                            serviceScheduler);
                    geoZonesUpdaterWrapper.setGeoZonesUpdater(nearestZonesManager);
                    locationTrackerCallback.setLocationTrackerCallback(nearestZonesManager);
                    if (locationPrefs().geolocationStarted().get()) {
                        geofenceTracker().startTracking();
                    }
                }
            }
        }

        return nearestZonesManager;
    }

    private static GeofenceTracker geofenceTracker() {
        if (geofenceTracker == null) {
            synchronized (GEOFENCE_TRACKER_MUTEX) {
                if (geofenceTracker == null) {
                    geofenceTracker = GeofenceTrackerFactory.createGeofenceTracker(geofencer(), geoZonesUpdaterWrapper, geoZoneStorage, locationTracker(), fineLocationPermissionChecker);
                    geofenceStateChangedCallbackWrapper.setGeofenceStateChangedCallback(geofenceTracker);
                    locationUpdateListener.setLocationUpdateListener(geofenceTracker);
                }
            }
        }

        return geofenceTracker;
    }

    private static ForegroundServiceHelper foregroundServiceHelper() {
        ImageUtils imageUtils = new ImageUtils();
        if (foregroundServiceHelper == null) {
            synchronized (FOREGROUND_SERVICE_HELPER_MUTEX) {
                if (foregroundServiceHelper == null) {
                    AndroidManifestLocationConfig config = new AndroidManifestLocationConfig(getApplicationContext());
                    foregroundServiceHelper = new ForegroundServiceHelper(AndroidPlatformModule.getApplicationContext(), config, imageUtils);
                    ForegroundServiceHelperRepository.setForegroundServiceHelper(foregroundServiceHelper);
                }
            }
        }
        return foregroundServiceHelper;
    }

    @Nullable
    public static Geofencer geofencer() {
        if (geofencer == null) {
            synchronized (GEOFENCER_MUTEX) {
                if (geofencer == null) {
                    if (getApplicationContext() == null) {
                        return null;
                    }
                    geofencer = GeofencerFactory.createGeofencer(getApplicationContext(), locationPermissionChecker, createGeofencingClient(), createPendingIntent());
                }
            }
        }

        return geofencer;
    }

    public static UpdateNearestRepository updateNearestRepository() {
        if (updateNearestRepository == null) {
            synchronized (UPDATE_NEAREST_REPOSITORY_MUTEX) {
                if (updateNearestRepository == null) {
                    updateNearestRepository = new UpdateNearestRepository(nearestZonesStorage, locationTracker());
                }
            }
        }

        return updateNearestRepository;
    }

    public static LocationPrefs locationPrefs() {
        if (locationPrefs == null) {
            synchronized (LOCATION_PREFS_MUTEX) {
                if (locationPrefs == null) {
                    locationPrefs = new LocationPrefs(AndroidPlatformModule.getPrefsProvider());
                }
            }
        }

        return locationPrefs;
    }

    public static JobLocationIdProvider jobLocationIdProvider() {
        return jobIdProvider;
    }

    @Nullable
    private static Context getApplicationContext() {
        return AndroidPlatformModule.getApplicationContext();
    }

    public static GeofenceStateChangedCallback geofenceStateChangedCallback() {
        return geofenceStateChangedCallbackWrapper;
    }

    private static class GeoZonesUpdaterWrapper implements GeoZonesUpdater {

        private GeoZonesUpdater geoZonesUpdater;

        void setGeoZonesUpdater(final GeoZonesUpdater geoZonesUpdater) {
            this.geoZonesUpdater = geoZonesUpdater;
        }

        @Override
        public void requestUpdateGeoZones(Callback<Boolean, PushwooshException> callback) {
            geoZonesUpdater.requestUpdateGeoZones(requestUpdateGeoZonesCallback ->
                callback.process(Result.fromData(geoZonesUpdater != null
                        && requestUpdateGeoZonesCallback.getData()))
            );
        }
    }

    private static class LocationTrackerCallbackWrapper implements LocationTrackerCallback {

        private LocationTrackerCallback locationTrackerCallback;

        void setLocationTrackerCallback(final LocationTrackerCallback locationTrackerCallback) {
            this.locationTrackerCallback = locationTrackerCallback;
        }

        @Override
        public void failedProvidingLocation() {
            if (locationTrackerCallback != null) {
                locationTrackerCallback.failedProvidingLocation();
            }
        }

        @Override
        public void successProvidingLocation() {
            if (locationTrackerCallback != null) {
                locationTrackerCallback.successProvidingLocation();
            }
        }
    }

    private static class GeofenceStateChangedCallbackWrapper implements GeofenceStateChangedCallback {

        private GeofenceStateChangedCallback geofenceStateChangedCallback;

        void setGeofenceStateChangedCallback(final GeofenceStateChangedCallback geofenceStateChangedCallback) {
            this.geofenceStateChangedCallback = geofenceStateChangedCallback;
        }

        @Override
        public void onGeofenceStateChanged(final List<String> zoneIds, final int geofenceTransition) {
            if (geofenceStateChangedCallback != null) {
                geofenceStateChangedCallback.onGeofenceStateChanged(zoneIds, geofenceTransition);
            }
        }
    }

    private static class LocationUpdateListenerWrapper implements LocationUpdateListener {
        private LocationUpdateListener locationUpdateListener;

        void setLocationUpdateListener(final LocationUpdateListener locationUpdateListener) {
            this.locationUpdateListener = locationUpdateListener;
        }

        @Override
        public void locationUpdated(final Location location) {
            if (locationUpdateListener != null) {
                locationUpdateListener.locationUpdated(location);
            }
        }
    }
}
