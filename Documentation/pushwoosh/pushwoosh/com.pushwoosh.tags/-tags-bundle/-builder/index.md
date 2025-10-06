//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)

# Builder

open class [Builder](index.md)

Builder for constructing [TagsBundle](../index.md) instances using the Builder pattern. 

 The Builder allows efficient, fluent construction of tag collections through method chaining. All tag additions are stored client-side until [build](build.md) is called to create an immutable TagsBundle. The actual synchronization with Pushwoosh servers happens when you call sendTags. 

Thread Safety: This builder uses [ConcurrentHashMap](https://developer.android.com/reference/kotlin/java/util/concurrent/ConcurrentHashMap.html) internally, making it safe to add tags from multiple threads during construction. 

Batching Best Practice: When setting multiple tags, always use a single Builder instance and one sendTags call instead of multiple individual tag operations. This reduces network requests and improves performance. 

Usage Example (Fitness App):

```kotlin

	TagsBundle userProfile = new TagsBundle.Builder()
	    // Demographics
	    .putInt("age", 28)
	    .putString("gender", "female")
	    .putString("fitness_level", "intermediate")
	
	    // Activity tracking
	    .putInt("workouts_completed", 45)
	    .putDate("last_workout", new Date())
	    .putList("favorite_activities", Arrays.asList("yoga", "running", "cycling"))
	
	    // Subscription status
	    .putBoolean("premium_member", true)
	    .putLong("subscription_end", System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
	
	    // Preferences
	    .putBoolean("push_workouts", true)
	    .putBoolean("push_achievements", true)
	    .build();
	
	// Send all tags in one request
	Pushwoosh.getInstance().sendTags(userProfile);
	
```

#### See also

| |
|---|
| [TagsBundle](../index.md) |
| [Tags](../../-tags/index.md) |

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [appendList](append-list.md) | [main]<br>open fun [appendList](append-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle.Builder](index.md)<br>Appends values to an existing list tag without replacing the entire list. |
| [build](build.md) | [main]<br>open fun [build](build.md)(): [TagsBundle](../index.md)<br>Builds and returns an immutable [TagsBundle](../index.md) instance with all added tags. |
| [getTagsHashMap](get-tags-hash-map.md) | [main]<br>open fun [getTagsHashMap](get-tags-hash-map.md)(): [HashMap](https://developer.android.com/reference/kotlin/java/util/HashMap.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; |
| [incrementInt](increment-int.md) | [main]<br>open fun [incrementInt](increment-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TagsBundle.Builder](index.md)<br>Increments an integer tag by the specified value without fetching the current value first. |
| [putAll](put-all.md) | [main]<br>open fun [putAll](put-all.md)(json: JSONObject): [TagsBundle.Builder](index.md)<br>Imports all tags from a JSON object, adding them to the builder. |
| [putBoolean](put-boolean.md) | [main]<br>open fun [putBoolean](put-boolean.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [TagsBundle.Builder](index.md)<br>Adds a tag with a boolean value. |
| [putDate](put-date.md) | [main]<br>open fun [putDate](put-date.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Date](https://developer.android.com/reference/kotlin/java/util/Date.html)): [TagsBundle.Builder](index.md)<br>Adds a tag with a date value, formatted as &quot;yyyy-MM-dd HH:mm&quot;. |
| [putInt](put-int.md) | [main]<br>open fun [putInt](put-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TagsBundle.Builder](index.md)<br>Adds a tag with an integer value. |
| [putList](put-list.md) | [main]<br>open fun [putList](put-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle.Builder](index.md)<br>Adds a tag with a list of string values, replacing any existing list. |
| [putLong](put-long.md) | [main]<br>open fun [putLong](put-long.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [TagsBundle.Builder](index.md)<br>Adds a tag with a long value. |
| [putString](put-string.md) | [main]<br>open fun [putString](put-string.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle.Builder](index.md)<br>Adds a tag with a string value. |
| [putStringIfNotEmpty](put-string-if-not-empty.md) | [main]<br>open fun [putStringIfNotEmpty](put-string-if-not-empty.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle.Builder](index.md)<br>Adds a tag with a string value only if the value is not null or empty. |
| [remove](remove.md) | [main]<br>open fun [remove](remove.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle.Builder](index.md)<br>Removes a tag from the user's profile on Pushwoosh servers. |
| [removeFromList](remove-from-list.md) | [main]<br>open fun [removeFromList](remove-from-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle.Builder](index.md)<br>Removes specific values from an existing list tag without replacing the entire list. |
