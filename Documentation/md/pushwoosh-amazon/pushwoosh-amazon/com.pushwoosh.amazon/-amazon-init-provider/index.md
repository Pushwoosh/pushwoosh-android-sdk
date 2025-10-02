//[pushwoosh-amazon](../../../index.md)/[com.pushwoosh.amazon](../index.md)/[AmazonInitProvider](index.md)

# AmazonInitProvider

[main]\
open class [AmazonInitProvider](index.md)

## Constructors

| | |
|---|---|
| [AmazonInitProvider](-amazon-init-provider.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [delete](delete.md) | [main]<br>open fun [delete](delete.md)(uri: Uri, selection: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), selectionArgs: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getType](get-type.md) | [main]<br>open fun [getType](get-type.md)(uri: Uri): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [insert](insert.md) | [main]<br>open fun [insert](insert.md)(uri: Uri, values: ContentValues): Uri |
| [onCreate](on-create.md) | [main]<br>open fun [onCreate](on-create.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [query](query.md) | [main]<br>open fun [query](query.md)(uri: Uri, projection: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, selection: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), selectionArgs: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, sortOrder: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): Cursor |
| [update](update.md) | [main]<br>open fun [update](update.md)(uri: Uri, values: ContentValues, selection: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), selectionArgs: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
