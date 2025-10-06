//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setTags](set-tags.md)

# setTags

[main]\
open fun [setTags](set-tags.md)(tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))

Associates device with given tags. If setTags request fails tags will be resent on the next application launch.  Example: 

```kotlin

  // Update user profile after login
  private void updateUserProfile(User user) {
      TagsBundle profileTags = new TagsBundle.Builder()
          .putString("Name", user.getFullName())
          .putInt("Age", user.getAge())
          .putString("Gender", user.getGender()) // "male", "female", "other"
          .putString("City", user.getCity())
          .putString("Subscription_Tier", user.getSubscriptionTier()) // "free", "basic", "premium"
          .putBoolean("Email_Verified", user.isEmailVerified())
          .build();

      Pushwoosh.getInstance().setTags(profileTags);
  }

  // Update user preferences
  private void saveUserPreferences(UserPreferences prefs) {
      TagsBundle preferencesTags = new TagsBundle.Builder()
          .putString("Favorite_Category", prefs.getFavoriteCategory()) // "electronics", "fashion", "sports"
          .putString("Language_Preference", prefs.getLanguage()) // "en", "es", "fr"
          .putBoolean("Push_Notifications_Enabled", prefs.isPushEnabled())
          .putBoolean("Email_Notifications_Enabled", prefs.isEmailEnabled())
          .putStringList("Interests", prefs.getInterests()) // ["tech", "gaming", "travel"]
          .build();

      Pushwoosh.getInstance().setTags(preferencesTags);
  }

  // Track user activity
  private void trackUserActivity(UserActivity activity) {
      TagsBundle activityTags = new TagsBundle.Builder()
          .putDate("Last_Login", new Date())
          .putInt("Total_Purchases", activity.getTotalPurchases())
          .putDouble("Total_Spent", activity.getTotalSpent())
          .putDate("Last_Purchase_Date", activity.getLastPurchaseDate())
          .putString("Last_Viewed_Product", activity.getLastViewedProductId())
          .build();

      Pushwoosh.getInstance().setTags(activityTags);
  }

```

#### Parameters

main

| | |
|---|---|
| tags | [application tags bundle](../../com.pushwoosh.tags/-tags-bundle/index.md) |

[main]\
open fun [setTags](set-tags.md)(tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)&gt;)

Associates device with given tags. If setTags request fails tags will be resent on the next application launch.  Example: 

```kotlin

  // Update user subscription status with callback
  private void upgradeToPremium(User user) {
      TagsBundle subscriptionTags = new TagsBundle.Builder()
          .putString("Subscription_Tier", "premium")
          .putDate("Premium_Since", new Date())
          .putBoolean("Is_Premium", true)
          .putInt("Premium_Credits", 1000)
          .build();

      Pushwoosh.getInstance().setTags(subscriptionTags, (result) -> {
          if (result.isSuccess()) {
              Log.d("App", "Premium subscription tags updated successfully");
              // Show success message to user
              Toast.makeText(this, "Welcome to Premium!", Toast.LENGTH_SHORT).show();
              // Navigate to premium features
              startActivity(new Intent(this, PremiumFeaturesActivity.class));
          } else {
              Log.e("App", "Failed to update subscription tags: " + result.getException().getMessage());
              // Tags will be retried on next app launch automatically
              Toast.makeText(this, "Subscription activated. Some features may take a moment to sync.", Toast.LENGTH_LONG).show();
          }
      });
  }

```

#### Parameters

main

| | |
|---|---|
| tags | [application tags bundle](../../com.pushwoosh.tags/-tags-bundle/index.md) |
| callback | sendTags operation callback |
