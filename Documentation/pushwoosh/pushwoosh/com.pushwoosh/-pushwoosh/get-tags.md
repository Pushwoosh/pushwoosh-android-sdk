//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getTags](get-tags.md)

# getTags

[main]\
open fun [getTags](get-tags.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), [GetTagsException](../../com.pushwoosh.exception/-get-tags-exception/index.md)&gt;)

Gets tags associated with current device  Example: 

```kotlin

  // Retrieve and use user profile tags to personalize UI
  private void loadUserProfile() {
      Pushwoosh.getInstance().getTags((result) -> {
          if (result.isSuccess()) {
              TagsBundle tags = result.getData();

              // Read user profile data
              String userName = tags.getString("Name", "Guest");
              int userAge = tags.getInt("Age", 0);
              String subscriptionTier = tags.getString("Subscription_Tier", "free");
              boolean isPremium = "premium".equals(subscriptionTier);

              // Update UI based on tags
              updateWelcomeMessage("Welcome back, " + userName + "!");
              if (isPremium) {
                  showPremiumFeatures();
              } else {
                  showUpgradePrompt();
              }

              // Check user preferences
              List<String> interests = tags.getStringList("Interests");
              if (interests != null && !interests.isEmpty()) {
                  showPersonalizedContent(interests);
              }

              Log.d("App", "User profile loaded: " + userName + ", tier: " + subscriptionTier);
          } else {
              Log.e("App", "Failed to retrieve tags: " + result.getException().getMessage());
              // Show default UI
              showDefaultContent();
          }
      });
  }

```

#### Parameters

main

| | |
|---|---|
| callback | callback handler |
