//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[TagsBundle](index.md)/[getBoolean](get-boolean.md)

# getBoolean

[main]\
open fun [getBoolean](get-boolean.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), defaultValue: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Retrieves a boolean tag value by name, with a fallback default value. 

 Returns the boolean value associated with the given key. If the tag doesn't exist or is not a boolean type, returns the provided default value. Use this for feature flags, subscription status, or any true/false attribute. 

Example (Feature Access Control):

```kotlin

	TagsBundle tags = getUserTags();
	
	boolean isPremium = tags.getBoolean("premium_member", false);
	boolean pushEnabled = tags.getBoolean("push_notifications", true);
	boolean emailSubscribed = tags.getBoolean("email_subscribed", false);
	boolean betaAccess = tags.getBoolean("beta_features_enabled", false);
	
	// Control feature access
	if (isPremium) {
	    unlockPremiumFeatures();
	}
	
	// Respect notification preferences
	if (pushEnabled) {
	    scheduleNotification();
	}
	
```

#### Return

tag value as boolean, or defaultValue if tag is not found or not a boolean

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;premium_member&quot;, &quot;push_enabled&quot;, &quot;trial_active&quot;) |
| defaultValue | value to return if tag doesn't exist or is not a boolean |
