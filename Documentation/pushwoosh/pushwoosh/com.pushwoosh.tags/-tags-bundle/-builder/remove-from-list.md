//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[removeFromList](remove-from-list.md)

# removeFromList

[main]\
open fun [removeFromList](remove-from-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle.Builder](index.md)

Removes specific values from an existing list tag without replacing the entire list. 

 This operation is performed server-side. The specified values are removed from the existing list tag. If a value appears multiple times in the list, all occurrences are removed. If the tag doesn't exist or none of the values are found, the operation has no effect. 

When to Use:

- Removing items from multi-value attributes (unsubscribe from topics, remove interests)
- Cleaning up historical data (remove old preferences, outdated categories)
- Avoiding the need to fetch and replace the entire list

Example (News App):

```kotlin

		// User unsubscribed from specific news categories
		new TagsBundle.Builder()
		    .removeFromList("subscribed_topics", Arrays.asList("politics", "sports"))
		    .removeFromList("favorite_authors", Arrays.asList("john_doe"))
		    .build();
		
		// Clean up user preferences
		new TagsBundle.Builder()
		    .removeFromList("blocked_categories", Arrays.asList("entertainment"))
		    .removeFromList("saved_for_later", Arrays.asList("article-123", "article-456"))
		    .build();
		
```

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;interests&quot;, &quot;subscribed_topics&quot;, &quot;blocked_users&quot;) |
| value | list of string values to remove from the existing list |
