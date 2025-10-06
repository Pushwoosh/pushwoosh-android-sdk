//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[remove](remove.md)

# remove

[main]\
open fun [remove](remove.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle.Builder](index.md)

Removes a tag from the user's profile on Pushwoosh servers. 

 This operation marks the tag for deletion. When the TagsBundle is sent to Pushwoosh, the specified tag will be completely removed from the user's profile. Use this when you need to clean up outdated tags or reset user attributes. 

Example (Privacy &Data Cleanup):

```kotlin

		// User downgraded from premium to free
		new TagsBundle.Builder()
		    .putBoolean("premium_member", false)
		    .remove("premium_tier") // Remove premium-specific tag
		    .remove("subscription_end") // No longer relevant
		    .remove("auto_renew_enabled")
		    .build();
		
		// User opted out of personalization
		new TagsBundle.Builder()
		    .remove("interests")
		    .remove("favorite_categories")
		    .remove("browsing_history")
		    .putBoolean("personalization_enabled", false)
		    .build();
		
```

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name to remove (e.g., &quot;old_tag&quot;, &quot;deprecated_field&quot;) |
