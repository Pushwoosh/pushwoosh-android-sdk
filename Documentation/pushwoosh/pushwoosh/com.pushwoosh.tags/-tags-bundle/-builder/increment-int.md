//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[incrementInt](increment-int.md)

# incrementInt

[main]\
open fun [incrementInt](increment-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TagsBundle.Builder](index.md)

Increments an integer tag by the specified value without fetching the current value first. 

 This operation is performed server-side, making it efficient for counters that need to be updated without knowing their current value. If the tag doesn't exist, it will be created and set to the increment value. Use negative values to decrement. 

When to Use:

- Tracking cumulative actions (app opens, purchases made, articles read)
- Maintaining counters without client-side state management
- Avoiding race conditions when multiple devices update the same counter

Example (Gaming App):

```kotlin

		// User completed a level and earned points
		new TagsBundle.Builder()
		    .incrementInt("levels_completed", 1)
		    .incrementInt("total_score", 500)
		    .incrementInt("coins_earned", 100)
		    .incrementInt("deaths", -1) // negative value to decrement
		    .build();
		
		// Track daily engagement
		new TagsBundle.Builder()
		    .incrementInt("app_opens_today", 1)
		    .incrementInt("total_sessions", 1)
		    .build();
		
```

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;total_purchases&quot;, &quot;app_opens&quot;, &quot;points_earned&quot;) |
| value | value to increment by (positive to increase, negative to decrease) |
