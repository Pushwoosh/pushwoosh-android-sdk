//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[empty](empty.md)

# empty

[main]\
open fun [empty](empty.md)(): [TagsBundle](../-tags-bundle/index.md)

Returns an empty TagsBundle singleton instance. 

 This method provides a reusable empty TagsBundle that contains no tags. It's useful when you need to pass a TagsBundle parameter but have no tags to set, or when implementing conditional tag logic where an empty bundle represents &quot;no changes&quot;. 

**When to Use:**

- Default values - provide empty bundle when no tags are available
- Conditional logic - return empty bundle when conditions aren't met
- Placeholder - use as a safe placeholder in method signatures
- Memory efficiency - reuse the singleton instead of creating new empty bundles

 Example: ```kotlin

	  // Conditional tag setting based on user state
	  private TagsBundle getUserTags(User user) {
	      if (user == null || !user.isLoggedIn()) {
	          // No user data available, return empty bundle
	          return Tags.empty();
	      }
	
	      return new TagsBundle.Builder()
	          .putString("Name", user.getName())
	          .putString("Email", user.getEmail())
	          .build();
	  }
	
	  // Use with optional features
	  private void updateUserPreferences(UserPreferences prefs) {
	      TagsBundle tags = prefs.hasChanges()
	          ? buildPreferencesTags(prefs)
	          : Tags.empty();
	
	      if (tags != Tags.empty()) {
	          Pushwoosh.getInstance().setTags(tags);
	      }
	  }
	
	  // Safe method parameters
	  private void registerUser(User user, TagsBundle additionalTags) {
	      // If no additional tags provided, use empty bundle
	      if (additionalTags == null) {
	          additionalTags = Tags.empty();
	      }
	
	      TagsBundle userTags = new TagsBundle.Builder()
	          .putString("User_ID", user.getId())
	          .putAll(additionalTags.toJson())
	          .build();
	
	      Pushwoosh.getInstance().registerForPushNotificationsWithTags(userTags);
	  }
	
	  // Factory pattern for tag generation
	  private TagsBundle createTagsForEvent(Event event) {
	      switch (event.getType()) {
	          case LOGIN:
	              return new TagsBundle.Builder()
	                  .putDate("Last_Login", new Date())
	                  .build();
	          case LOGOUT:
	              // No tags needed for logout
	              return Tags.empty();
	          case PURCHASE:
	              return Tags.incrementInt("Total_Purchases", 1);
	          default:
	              return Tags.empty();
	      }
	  }
	
```

#### Return

Singleton empty TagsBundle instance
