//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[removeTag](remove-tag.md)

# removeTag

[main]\
open fun [removeTag](remove-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle that removes a tag from the device. 

 This method is used to delete tags that are no longer needed or contain outdated information. Removing unused tags keeps your data clean and improves targeting accuracy. Common scenarios include user logout, feature deprecation, or data cleanup. 

**When to Remove Tags:**

- User logout - remove user-specific tags like name, email, subscription tier
- Feature deprecation - remove tags for discontinued features or old implementations
- Preference reset - remove user preferences when reverting to defaults
- Data cleanup - remove temporary or session-specific tags
- Privacy compliance - remove personal data when requested by user

 Example: ```kotlin

	  // User logout: Clean up user-specific data
	  private void onUserLogout() {
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Name"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Email"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Subscription_Tier"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Loyalty_Points"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Last_Purchase"));
	
	      // Also clear user ID
	      Pushwoosh.getInstance().setUserId("");
	  }
	
	  // Remove deprecated tags after app update
	  private void cleanupDeprecatedTags() {
	      // Old tag names being replaced with new ones
	      Pushwoosh.getInstance().setTags(Tags.removeTag("old_subscription_field"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("legacy_user_type"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("deprecated_preference"));
	  }
	
	  // E-commerce: Clear cart-related tags after checkout
	  private void onOrderCompleted() {
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Items_In_Cart"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Cart_Value"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Cart_Abandoned_Date"));
	  }
	
	  // Privacy: Remove user data on request
	  private void deleteUserData() {
	      // Remove all personal information tags
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Name"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Email"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Phone"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("City"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Age"));
	  }
	
	  // Reset user preferences to defaults
	  private void resetPreferences() {
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Notification_Frequency"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Preferred_Language"));
	      Pushwoosh.getInstance().setTags(Tags.removeTag("Theme_Preference"));
	  }
	
	  // Bulk cleanup using TagsBundle.Builder (more efficient)
	  private void bulkRemoveTags() {
	      TagsBundle cleanup = new TagsBundle.Builder()
	          .remove("Old_Field_1")
	          .remove("Old_Field_2")
	          .remove("Deprecated_Tag")
	          .remove("Unused_Preference")
	          .build();
	      Pushwoosh.getInstance().setTags(cleanup);
	  }
	
```

#### Return

TagsBundle containing the tag removal operation

#### Parameters

main

| | |
|---|---|
| key | tag name to remove (e.g., &quot;Name&quot;, &quot;Old_Field&quot;, &quot;Deprecated_Tag&quot;) |

#### See also

| |
|---|
| [TagsBundle.Builder](../-tags-bundle/-builder/remove.md) |
