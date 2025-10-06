//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[putList](put-list.md)

# putList

[main]\
open fun [putList](put-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle.Builder](index.md)

Adds a tag with a list of string values, replacing any existing list. 

 Use for multi-value attributes such as interests, categories, product IDs, or any attribute where a user can have multiple selections. This method replaces the entire list - to add or remove individual items from an existing list, use [appendList](append-list.md) or [removeFromList](remove-from-list.md) instead. 

Example (Content Preferences):

```kotlin

		new TagsBundle.Builder()
		    // User interests
		    .putList("interests", Arrays.asList("technology", "sports", "travel"))
		    .putList("favorite_sports", Arrays.asList("football", "basketball", "tennis"))
		
		    // Product interactions
		    .putList("wishlist_ids", Arrays.asList("PROD-001", "PROD-042", "PROD-156"))
		    .putList("recently_viewed", Arrays.asList("CAT-electronics", "CAT-books"))
		
		    // Content subscriptions
		    .putList("newsletter_topics", Arrays.asList("daily_digest", "weekly_deals", "new_arrivals"))
		    .build();
		
```

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;interests&quot;, &quot;categories&quot;, &quot;product_ids&quot;) |
| value | list of string values (replaces existing list completely) |
