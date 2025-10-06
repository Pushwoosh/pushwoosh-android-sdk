//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[TagsBundle](index.md)

# TagsBundle

open class [TagsBundle](index.md)

Immutable, thread-safe collection of tags for user segmentation and personalization. 

 TagsBundle stores key-value pairs that represent user attributes, preferences, and behaviors. Tags enable targeted push notifications, audience segmentation, and personalized content delivery through the Pushwoosh platform. Once built, a TagsBundle instance is immutable and can be safely shared across threads. 

Building vs Reading Tags:

- Use [Builder](-builder/index.md) when you need to create a custom tag collection for multiple operations
- Use [Tags](../-tags/index.md) utility methods for simple, one-off tag operations
- Use sendTags to sync tags with Pushwoosh servers

Supported Tag Types:

- Integer - for demographics, counters, scores (age, level, points)
- Long - for timestamps, large IDs (registration_timestamp, user_id)
- Boolean - for flags, subscriptions (email_subscribed, premium_user)
- String - for profile data, categories (name, gender, favorite_category)
- List - for multi-value attributes (interests, purchased_products)
- Date - for milestones, events (last_purchase, subscription_end)

Thread Safety: TagsBundle is immutable after creation. The Builder uses [ConcurrentHashMap](https://developer.android.com/reference/kotlin/java/util/concurrent/ConcurrentHashMap.html) to allow safe concurrent tag additions during construction. 

Usage Example (E-commerce App):

```kotlin

// Building tags for a premium customer
TagsBundle tags = new TagsBundle.Builder()
    .putString("customer_tier", "premium")
    .putInt("lifetime_orders", 15)
    .putLong("customer_since", System.currentTimeMillis())
    .putBoolean("newsletter_subscribed", true)
    .putList("favorite_categories", Arrays.asList("electronics", "books"))
    .putDate("last_purchase", new Date())
    .build();

// Sending tags to Pushwoosh
Pushwoosh.getInstance().sendTags(tags);

// Reading tags from bundle
String tier = tags.getString("customer_tier"); // "premium"
int orders = tags.getInt("lifetime_orders", 0); // 15
List<String> categories = tags.getList("favorite_categories"); // ["electronics", "books"]

// Converting to JSON for API calls
JSONObject json = tags.toJson();

```

#### See also

| |
|---|
| [Tags](../-tags/index.md) |
| Pushwoosh |
| &lt;a href=&quot;http://docs.pushwoosh.com/docs/segmentation-tags-and-filters&quot;&gt;Segmentation guide&lt;/a&gt; |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [main]<br>open class [Builder](-builder/index.md)<br>Builder for constructing [TagsBundle](index.md) instances using the Builder pattern. |

## Functions

| Name | Summary |
|---|---|
| [getBoolean](get-boolean.md) | [main]<br>open fun [getBoolean](get-boolean.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), defaultValue: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Retrieves a boolean tag value by name, with a fallback default value. |
| [getInt](get-int.md) | [main]<br>open fun [getInt](get-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), defaultValue: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Retrieves an integer tag value by name, with a fallback default value. |
| [getList](get-list.md) | [main]<br>open fun [getList](get-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;<br>Retrieves a list tag value by name, or null if not found. |
| [getLong](get-long.md) | [main]<br>open fun [getLong](get-long.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), defaultValue: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>Retrieves a long tag value by name, with a fallback default value. |
| [getMap](get-map.md) | [main]<br>open fun [getMap](get-map.md)(): [Map](https://developer.android.com/reference/kotlin/java/util/Map.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;<br>Returns the internal map representation of tags. |
| [getString](get-string.md) | [main]<br>open fun [getString](get-string.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)<br>Retrieves a string tag value by name, or null if not found. |
| [toJson](to-json.md) | [main]<br>open fun [toJson](to-json.md)(): JSONObject<br>Converts the TagsBundle to its JSON representation. |
