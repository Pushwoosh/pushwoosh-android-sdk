//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[putBoolean](put-boolean.md)

# putBoolean

[main]\
open fun [putBoolean](put-boolean.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [TagsBundle.Builder](index.md)

Adds a tag with a boolean value. 

 Use for binary flags, subscription states, feature toggles, or any yes/no attribute. Boolean tags are ideal for segmenting users based on true/false conditions. 

Example (Subscription Service):

```kotlin

		new TagsBundle.Builder()
		    // Subscription status
		    .putBoolean("premium_member", true)
		    .putBoolean("trial_active", false)
		    .putBoolean("auto_renew_enabled", true)
		
		    // Communication preferences
		    .putBoolean("email_notifications", true)
		    .putBoolean("push_notifications", true)
		    .putBoolean("sms_notifications", false)
		
		    // Feature flags
		    .putBoolean("beta_features_enabled", true)
		    .putBoolean("gdpr_consent", true)
		    .build();
		
```

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;premium_member&quot;, &quot;email_subscribed&quot;, &quot;onboarding_completed&quot;) |
| value | boolean value (true or false) |
