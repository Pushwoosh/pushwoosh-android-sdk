//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[putLong](put-long.md)

# putLong

[main]\
open fun [putLong](put-long.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [TagsBundle.Builder](index.md)

Adds a tag with a long value. 

 Use for timestamps (registration date, last activity), large numeric IDs (user ID, transaction ID), or any numeric value that exceeds the integer range. Long values can store numbers from -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807. 

Example (News App):

```kotlin

		long now = System.currentTimeMillis();
		new TagsBundle.Builder()
		    .putLong("user_id", 9876543210L)
		    .putLong("registered_at", now)
		    .putLong("last_article_read", now - 3600000) // 1 hour ago
		    .putLong("subscription_id", 1234567890123L)
		    .build();
		
```

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;user_id&quot;, &quot;registered_at&quot;, &quot;last_login&quot;) |
| value | long value to store |
