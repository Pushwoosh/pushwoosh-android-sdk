//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[RegisterForPushNotificationsResultData](index.md)/[isEnabled](is-enabled.md)

# isEnabled

[main]\
open fun [isEnabled](is-enabled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Returns whether push notifications are currently enabled for this app. 

 This indicates if the user has granted notification permission to the app. On Android 13+ (API level 33+), users must explicitly grant notification permission. On earlier versions, notifications are enabled by default but users can disable them in system settings. 

 Returns `false` if: 

- User denied notification permission (Android 13+)
- User disabled notifications in system settings
- App's notification channel is blocked

**Example:**

```kotlin

if (!resultData.isEnabled()) {
    // Notifications are disabled, show explanation to user
    new AlertDialog.Builder(this)
        .setTitle("Enable Notifications")
        .setMessage("Please enable notifications to receive important updates")
        .setPositiveButton("Settings", (dialog, which) -> {
            // Open app notification settings
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        })
        .show();
}

```

#### Return

`true` if notifications are enabled, `false` otherwise
