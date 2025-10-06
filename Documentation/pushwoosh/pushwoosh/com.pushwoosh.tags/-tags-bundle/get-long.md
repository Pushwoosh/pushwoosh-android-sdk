//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[TagsBundle](index.md)/[getLong](get-long.md)

# getLong

[main]\
open fun [getLong](get-long.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), defaultValue: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)

Retrieves a long tag value by name, with a fallback default value. 

 Returns the long value associated with the given key. If the tag doesn't exist or cannot be converted to a long, returns the provided default value. This method is commonly used for timestamps and large numeric IDs. 

Example (Subscription Management):

```kotlin

	TagsBundle tags = getUserTags();
	
	long userId = tags.getLong("user_id", 0L);
	long registeredAt = tags.getLong("registered_at", 0L);
	long subscriptionEnd = tags.getLong("subscription_end", 0L);
	
	// Check if subscription is expiring soon
	long now = System.currentTimeMillis();
	long daysUntilExpiry = (subscriptionEnd - now) / (24 * 60 * 60 * 1000);
	
	if (daysUntilExpiry <= 7 && daysUntilExpiry > 0) {
	    showRenewalReminder();
	}
	
```

#### Return

tag value as long, or defaultValue if tag is not found or not a number

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;user_id&quot;, &quot;registered_at&quot;, &quot;subscription_end&quot;) |
| defaultValue | value to return if tag doesn't exist or cannot be converted to long |
