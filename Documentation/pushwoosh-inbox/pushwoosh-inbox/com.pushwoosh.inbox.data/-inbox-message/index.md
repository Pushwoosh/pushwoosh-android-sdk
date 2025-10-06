//[pushwoosh-inbox](../../../index.md)/[com.pushwoosh.inbox.data](../index.md)/[InboxMessage](index.md)

# InboxMessage

[main]\
interface [InboxMessage](index.md) : [Serializable](https://developer.android.com/reference/kotlin/java/io/Serializable.html), [Comparable](https://developer.android.com/reference/kotlin/java/lang/Comparable.html)&lt;[T](https://developer.android.com/reference/kotlin/java/lang/Comparable.html)&gt;

## Functions

| Name | Summary |
|---|---|
| [compareTo](index.md#-1554281679%2FFunctions%2F1579049883) | [main]<br>abstract fun [compareTo](index.md#-1554281679%2FFunctions%2F1579049883)(p: [T](https://developer.android.com/reference/kotlin/java/lang/Comparable.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getActionParams](get-action-params.md) | [main]<br>abstract fun [getActionParams](get-action-params.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getBannerUrl](get-banner-url.md) | [main]<br>abstract fun [getBannerUrl](get-banner-url.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getCode](get-code.md) | [main]<br>abstract fun [getCode](get-code.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getImageUrl](get-image-url.md) | [main]<br>abstract fun [getImageUrl](get-image-url.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getISO8601SendDate](get-i-s-o8601-send-date.md) | [main]<br>abstract fun [getISO8601SendDate](get-i-s-o8601-send-date.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getMessage](get-message.md) | [main]<br>abstract fun [getMessage](get-message.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getSendDate](get-send-date.md) | [main]<br>abstract fun [getSendDate](get-send-date.md)(): [Date](https://developer.android.com/reference/kotlin/java/util/Date.html) |
| [getTitle](get-title.md) | [main]<br>abstract fun [getTitle](get-title.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getType](get-type.md) | [main]<br>abstract fun [getType](get-type.md)(): [InboxMessageType](../-inbox-message-type/index.md) |
| [isActionPerformed](is-action-performed.md) | [main]<br>abstract fun [isActionPerformed](is-action-performed.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Action of the Inbox Message is performed [performAction](../../com.pushwoosh.inbox/-pushwoosh-inbox/perform-action.md) or an action was performed on the push tap ) |
| [isRead](is-read.md) | [main]<br>abstract fun [isRead](is-read.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Inbox Message which is read, see [readMessage](../../com.pushwoosh.inbox/-pushwoosh-inbox/read-message.md) |
