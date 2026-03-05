# Module pushwoosh-location

Enables geo push notifications by tracking the device's location and registering it with Pushwoosh
geozones. When the device enters a configured geozone, Pushwoosh delivers the associated push
notification. Add this dependency when you need to trigger pushes based on the user's physical
location. The primary entry point is [PushwooshLocation].

# Package com.pushwoosh.location

Use [PushwooshLocation] to start and stop geolocation tracking and to request the background
location permission required for tracking when the app is not in the foreground.

# Package com.pushwoosh.location.network.exception

[LocationNotAvailableException] is thrown when location tracking cannot start because the required
permissions have been denied or the device's location services are disabled.
