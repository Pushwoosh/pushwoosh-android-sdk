//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[registerForPushNotificationsWithTags](register-for-push-notifications-with-tags.md)

# registerForPushNotificationsWithTags

[main]\
open fun [registerForPushNotificationsWithTags](register-for-push-notifications-with-tags.md)(tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))

#### See also

| |
|---|
| &lt;a href=&quot;#registerForPushNotificationsWithTags(Callback, TagsBundle)&quot;&gt;registerForPushNotifications(Callback, TagsBundle)&lt;/a&gt; |

[main]\
open fun [registerForPushNotificationsWithTags](register-for-push-notifications-with-tags.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[RegisterForPushNotificationsResultData](../-register-for-push-notifications-result-data/index.md), [RegisterForPushNotificationsException](../../com.pushwoosh.exception/-register-for-push-notifications-exception/index.md)&gt;, tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))

Registers the device for push notifications and sets tags in a single request. 

 This method combines registration and tag setting operations, which is more efficient than calling [registerForPushNotifications](register-for-push-notifications.md) and [setTags](set-tags.md) separately.  Example: 

```kotlin

  // Register with user profile data from onboarding flow
  private void registerWithUserProfile(User user) {
      TagsBundle userTags = new TagsBundle.Builder()
          .putString("Name", user.getName())
          .putInt("Age", user.getAge())
          .putString("Subscription", user.getSubscriptionType()) // "free", "premium"
          .putString("Language", user.getPreferredLanguage()) // "en", "es", "fr"
          .putBoolean("Marketing_Consent", user.hasMarketingConsent())
          .build();

      Pushwoosh.getInstance().registerForPushNotificationsWithTags((result) -> {
          if (result.isSuccess()) {
              Log.d("App", "User registered with profile data");
              navigateToHomeScreen();
          } else {
              Log.e("App", "Registration failed: " + result.getException().getMessage());
              showRetryDialog();
          }
      }, userTags);
  }

```

#### Parameters

main

| | |
|---|---|
| callback | push registration callback |
| tags | tags to be set when registering for pushes |
