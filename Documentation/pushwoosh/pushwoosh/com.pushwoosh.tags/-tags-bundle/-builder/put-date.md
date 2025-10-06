//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[putDate](put-date.md)

# putDate

[main]\
open fun [putDate](put-date.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Date](https://developer.android.com/reference/kotlin/java/util/Date.html)): [TagsBundle.Builder](index.md)

Adds a tag with a date value, formatted as &quot;yyyy-MM-dd HH:mm&quot;. 

 Use for tracking important dates, milestones, or time-based events. The date is automatically converted to the format &quot;yyyy-MM-dd HH:mm&quot; before storage. Date tags enable time-based segmentation (e.g., users who purchased in the last 30 days, subscription expiring soon). 

Example (Subscription &Events):

```kotlin

		Calendar cal = Calendar.getInstance();
		
		new TagsBundle.Builder()
		    // Important dates
		    .putDate("last_purchase", new Date())
		    .putDate("account_created", new Date())
		    .putDate("last_login", new Date())
		
		    // Subscription milestones
		    .putDate("trial_started", new Date())
		    .putDate("subscription_end", cal.getTime()) // Future date
		    .putDate("last_payment", new Date())
		
		    // Activity tracking
		    .putDate("last_app_open", new Date())
		    .putDate("onboarding_completed", new Date())
		    .build();
		
```

Note: The date format is &quot;yyyy-MM-dd HH:mm&quot; (e.g., &quot;2024-01-15 14:30&quot;). For timestamp-based tracking with millisecond precision, consider using [putLong](put-long.md) with [currentTimeMillis](https://developer.android.com/reference/kotlin/java/lang/System.html#currenttimemillis).

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;last_purchase&quot;, &quot;subscription_end&quot;, &quot;trial_started&quot;) |
| value | Date object to store (will be formatted as &quot;yyyy-MM-dd HH:mm&quot;) |
