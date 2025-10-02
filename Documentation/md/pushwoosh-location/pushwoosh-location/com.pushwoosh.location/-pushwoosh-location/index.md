//[pushwoosh-location](../../../index.md)/[com.pushwoosh.location](../index.md)/[PushwooshLocation](index.md)

# PushwooshLocation

[main]\
open class [PushwooshLocation](index.md)

PushwooshLocation is a static class responsible for pushwoosh geolocation tracking.  By default pushwoosh-location library automatically adds following permissions: [permission.ACCESS_FINE_LOCATION](https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_FINE_LOCATION)[android.permission.ACCESS_COARSE_LOCATION](https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_COARSE_LOCATION) For Android 6 and higher these permissions should be requested dynamically before invoking PushwooshLocation.startLocationTracking()

## Constructors

| | |
|---|---|
| [PushwooshLocation](-pushwoosh-location.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [requestBackgroundLocationPermission](request-background-location-permission.md) | [main]<br>open fun [requestBackgroundLocationPermission](request-background-location-permission.md)()<br>Requests background location permission. |
| [startLocationTracking](start-location-tracking.md) | [main]<br>open fun [startLocationTracking](start-location-tracking.md)()<br>open fun [startLocationTracking](start-location-tracking.md)(callback: Callback&lt;Void, LocationNotAvailableException&gt;)<br>Starts location tracking for geo push notifications. |
| [stopLocationTracking](stop-location-tracking.md) | [main]<br>open fun [stopLocationTracking](stop-location-tracking.md)()<br>Stops geolocation tracking. |
