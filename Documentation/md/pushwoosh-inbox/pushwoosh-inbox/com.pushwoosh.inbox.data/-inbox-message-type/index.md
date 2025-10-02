//[pushwoosh-inbox](../../../index.md)/[com.pushwoosh.inbox.data](../index.md)/[InboxMessageType](index.md)

# InboxMessageType

[main]\
enum [InboxMessageType](index.md)

The Inbox message type. * Plain - without any action * RichMedia - contains a Rich media page, * URL - contains remote URL, * Deeplink - contains Deeplink

## Entries

| | |
|---|---|
| [PLAIN](-p-l-a-i-n/index.md) | [main]<br>[PLAIN](-p-l-a-i-n/index.md) |
| [RICH_MEDIA](-r-i-c-h_-m-e-d-i-a/index.md) | [main]<br>[RICH_MEDIA](-r-i-c-h_-m-e-d-i-a/index.md) |
| [URL](-u-r-l/index.md) | [main]<br>[URL](-u-r-l/index.md) |
| [DEEP_LINK](-d-e-e-p_-l-i-n-k/index.md) | [main]<br>[DEEP_LINK](-d-e-e-p_-l-i-n-k/index.md) |

## Properties

| Name | Summary |
|---|---|
| [code](code.md) | [main]<br>open val [code](code.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [getByCode](get-by-code.md) | [main]<br>open fun [getByCode](get-by-code.md)(code: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [InboxMessageType](index.md) |
| [valueOf](value-of.md) | [main]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)): [InboxMessageType](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [main]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[InboxMessageType](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
