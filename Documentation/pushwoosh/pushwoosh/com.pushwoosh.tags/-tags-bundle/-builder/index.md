//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)

# Builder

[main]\
open class [Builder](index.md)

TagsBundle.Builder class is used to generate TagsBundle instances

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [appendList](append-list.md) | [main]<br>open fun [appendList](append-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle.Builder](index.md)<br>Adds append operation for given list tag |
| [build](build.md) | [main]<br>open fun [build](build.md)(): [TagsBundle](../index.md)<br>Builds and returns TagsBundle. |
| [getTagsHashMap](get-tags-hash-map.md) | [main]<br>open fun [getTagsHashMap](get-tags-hash-map.md)(): [HashMap](https://developer.android.com/reference/kotlin/java/util/HashMap.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; |
| [incrementInt](increment-int.md) | [main]<br>open fun [incrementInt](increment-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TagsBundle.Builder](index.md)<br>Adds increment operation for given tag |
| [putAll](put-all.md) | [main]<br>open fun [putAll](put-all.md)(json: JSONObject): [TagsBundle.Builder](index.md)<br>Adds all tags from key-value pairs of given json |
| [putBoolean](put-boolean.md) | [main]<br>open fun [putBoolean](put-boolean.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [TagsBundle.Builder](index.md)<br>Adds tag with boolean value |
| [putDate](put-date.md) | [main]<br>open fun [putDate](put-date.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Date](https://developer.android.com/reference/kotlin/java/util/Date.html)): [TagsBundle.Builder](index.md)<br>Adds tag with date value |
| [putInt](put-int.md) | [main]<br>open fun [putInt](put-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TagsBundle.Builder](index.md)<br>Adds tag with integer value |
| [putList](put-list.md) | [main]<br>open fun [putList](put-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle.Builder](index.md)<br>Adds tag with list value |
| [putLong](put-long.md) | [main]<br>open fun [putLong](put-long.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [TagsBundle.Builder](index.md)<br>Adds tag with long value |
| [putString](put-string.md) | [main]<br>open fun [putString](put-string.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle.Builder](index.md)<br>Adds tag with string value |
| [putStringIfNotEmpty](put-string-if-not-empty.md) | [main]<br>open fun [putStringIfNotEmpty](put-string-if-not-empty.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle.Builder](index.md) |
| [remove](remove.md) | [main]<br>open fun [remove](remove.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle.Builder](index.md)<br>Removes tag |
| [removeFromList](remove-from-list.md) | [main]<br>open fun [removeFromList](remove-from-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle.Builder](index.md)<br>Adds remove operation for given list tag |
