//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[booleanTag](boolean-tag.md)

# booleanTag

[main]\
open fun [booleanTag](boolean-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle with a single boolean tag. 

 Boolean tags are ideal for storing yes/no, true/false, or on/off values. They are commonly used for user preferences, feature flags, verification statuses, and subscription states. 

**Common Use Cases:**

- Subscription status - is premium user, has active subscription, trial active
- Verification status - email verified, phone verified, identity confirmed
- User preferences - notifications enabled, marketing consent, dark mode enabled
- Feature flags - beta features enabled, new UI enabled
- Activity status - onboarding completed, profile complete, first purchase made

 Example: ```kotlin

	  // E-commerce: Track subscription and verification status
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Is_Premium_User", true));
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Email_Verified", true));
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("SMS_Notifications_Enabled", false));
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("First_Purchase_Made", true));
	
	  // News app: Track user preferences
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Breaking_News_Enabled", true));
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Daily_Digest_Enabled", false));
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Push_Notifications_Allowed", true));
	
	  // Banking app: Security and compliance
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Two_Factor_Auth_Enabled", true));
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Biometric_Login_Enabled", true));
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Marketing_Consent_Given", false));
	
	  // Fitness app: Track onboarding and features
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Onboarding_Completed", true));
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Workout_Reminders_Enabled", true));
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Premium_Features_Unlocked", false));
	
	  // General: Feature flags and A/B testing
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("New_UI_Enabled", true));
	  Pushwoosh.getInstance().setTags(Tags.booleanTag("Beta_Features_Enabled", false));
	
```

#### Return

TagsBundle containing the single boolean tag

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;Is_Premium_User&quot;, &quot;Email_Verified&quot;, &quot;Push_Notifications_Allowed&quot;) |
| value | boolean tag value (true or false) |

#### See also

| |
|---|
| [TagsBundle.Builder](../-tags-bundle/-builder/put-boolean.md) |
