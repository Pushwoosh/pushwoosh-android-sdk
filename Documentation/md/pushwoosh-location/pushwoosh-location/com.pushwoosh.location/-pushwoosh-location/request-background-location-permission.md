//[pushwoosh-location](../../../index.md)/[com.pushwoosh.location](../index.md)/[PushwooshLocation](index.md)/[requestBackgroundLocationPermission](request-background-location-permission.md)

# requestBackgroundLocationPermission

[main]\
open fun [requestBackgroundLocationPermission](request-background-location-permission.md)()

Requests background location permission. Works on Android 10 or above. On Android 12 opens the application's location permission settings. Before calling this method make sure the application already has ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permission and ACCESS_BACKGROUND_LOCATION permission is declared in the AndroidManifest.xml.
