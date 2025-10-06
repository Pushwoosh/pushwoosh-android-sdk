//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[putString](put-string.md)

# putString

[main]\
open fun [putString](put-string.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle.Builder](index.md)

Adds a tag with a string value. 

 Use for text-based attributes such as names, categories, preferences, identifiers, or any non-numeric data. String tags are the most versatile type and support segmentation by exact match, contains, starts with, and other text-based filters. 

Example (User Profile):

```kotlin

		new TagsBundle.Builder()
		    // Demographics
		    .putString("name", "John Doe")
		    .putString("gender", "male")
		    .putString("country", "USA")
		    .putString("language", "en")
		
		    // Preferences
		    .putString("favorite_category", "electronics")
		    .putString("preferred_currency", "USD")
		    .putString("timezone", "America/New_York")
		
		    // Custom identifiers
		    .putString("customer_tier", "gold")
		    .putString("referral_code", "FRIEND2024")
		    .build();
		
```

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;name&quot;, &quot;category&quot;, &quot;language&quot;, &quot;customer_tier&quot;) |
| value | string value to store (can be null, but consider using [putStringIfNotEmpty](put-string-if-not-empty.md)) |
