//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[TagsBundle](index.md)/[getString](get-string.md)

# getString

[main]\
open fun [getString](get-string.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)

Retrieves a string tag value by name, or null if not found. 

 Returns the string value associated with the given key. If the tag doesn't exist or is not a string type, returns null. Always check for null before using the returned value. 

Example (User Profile Display):

```kotlin

	TagsBundle tags = getUserTags();
	
	String name = tags.getString("name");
	String email = tags.getString("email");
	String customerTier = tags.getString("customer_tier");
	String favoriteCategory = tags.getString("favorite_category");
	
	// Safe null handling
	if (name != null) {
	    textViewName.setText(name);
	} else {
	    textViewName.setText("Guest User");
	}
	
	// Display tier with default
	String tier = customerTier != null ? customerTier : "standard";
	textViewTier.setText("Tier: " + tier);
	
```

#### Return

tag value as String, or null if tag is not found or not a string

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;name&quot;, &quot;email&quot;, &quot;category&quot;, &quot;language&quot;) |
