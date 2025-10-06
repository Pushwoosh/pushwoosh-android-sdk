//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[registerForPushNotificationsWithTagsWithoutPermission](register-for-push-notifications-with-tags-without-permission.md)

# registerForPushNotificationsWithTagsWithoutPermission

[main]\
open fun [registerForPushNotificationsWithTagsWithoutPermission](register-for-push-notifications-with-tags-without-permission.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[RegisterForPushNotificationsResultData](../-register-for-push-notifications-result-data/index.md), [RegisterForPushNotificationsException](../../com.pushwoosh.exception/-register-for-push-notifications-exception/index.md)&gt;, tagsBundle: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))

Registers the device for push notifications with tags without requesting notification permission. 

 This method combines registration and tag setting while skipping the notification permission request. Useful when you want to handle the permission request yourself or have already requested it separately.  Example: 

```kotlin

  // Register with user data after custom permission flow
  private void completeRegistrationWithUserData(User user, boolean hasPermission) {
      TagsBundle userTags = new TagsBundle.Builder()
          .putString("Name", user.getName())
          .putString("Account_Type", user.getAccountType())
          .putBoolean("Notification_Permission", hasPermission)
          .putDate("Registration_Date", new Date())
          .build();

      // Register without triggering permission dialog (already handled)
      Pushwoosh.getInstance().registerForPushNotificationsWithTagsWithoutPermission((result) -> {
          if (result.isSuccess()) {
              Log.d("App", "User registered with profile data");
              if (hasPermission) {
                  showWelcomeNotification();
              }
              navigateToHome();
          } else {
              Log.e("App", "Registration failed: " + result.getException().getMessage());
              showRetryOption();
          }
      }, userTags);
  }

```

#### Parameters

main

| | |
|---|---|
| callback | push registration callback |
| tagsBundle | tags to be set when registering for pushes |

#### See also

| |
|---|
| [registerForPushNotificationsWithTags(Callback, TagsBundle)](register-for-push-notifications-with-tags.md) |
| [requestNotificationPermission()](request-notification-permission.md) |
