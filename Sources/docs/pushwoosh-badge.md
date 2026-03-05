# Module pushwoosh-badge

Adds application icon badge number management to the Pushwoosh SDK. When Pushwoosh delivers a
push notification, this module automatically updates the badge count on the app icon across a wide
range of Android launchers. Add this dependency when you need to display or control unread
notification counts on the app icon. The primary entry point is [PushwooshBadge].

# Package com.pushwoosh.badge

[PushwooshBadge] is the main class for managing the application icon badge number. Use it to set
the badge to a specific value, retrieve the current value, or increment it by a delta. Passing `0`
to `setBadgeNumber` clears the badge. Changes are synchronized with the Pushwoosh backend
automatically.
