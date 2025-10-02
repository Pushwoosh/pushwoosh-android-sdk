//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[TagsBundle](index.md)

# TagsBundle

open class [TagsBundle](index.md)

Immutable collection of tags specific for current device. Tags are used to target different audience selectively when sending push notification.

#### See also

| |
|---|
| &lt;a href=&quot;http://docs.pushwoosh.com/docs/segmentation-tags-and-filters&quot;&gt;Segmentation guide&lt;/a&gt; |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [main]<br>open class [Builder](-builder/index.md)<br>TagsBundle. |

## Functions

| Name | Summary |
|---|---|
| [getBoolean](get-boolean.md) | [main]<br>open fun [getBoolean](get-boolean.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), defaultValue: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [getInt](get-int.md) | [main]<br>open fun [getInt](get-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), defaultValue: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getList](get-list.md) | [main]<br>open fun [getList](get-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt; |
| [getLong](get-long.md) | [main]<br>open fun [getLong](get-long.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), defaultValue: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [getMap](get-map.md) | [main]<br>open fun [getMap](get-map.md)(): [Map](https://developer.android.com/reference/kotlin/java/util/Map.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; |
| [getString](get-string.md) | [main]<br>open fun [getString](get-string.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [toJson](to-json.md) | [main]<br>open fun [toJson](to-json.md)(): JSONObject |
