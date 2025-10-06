//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[TagsBundle](index.md)/[getMap](get-map.md)

# getMap

[main]\
open fun [getMap](get-map.md)(): [Map](https://developer.android.com/reference/kotlin/java/util/Map.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;

Returns the internal map representation of tags. 

 This method is intended for internal SDK use and advanced scenarios. It provides direct access to the underlying tag map structure. The returned map is immutable (backed by a HashMap created during build), so modifications will throw UnsupportedOperationException. 

 For standard tag access, prefer the type-safe getter methods: [getString](get-string.md), [getInt](get-int.md), [getBoolean](get-boolean.md), [getList](get-list.md), [getLong](get-long.md).

#### Return

immutable map of tag names to their values
