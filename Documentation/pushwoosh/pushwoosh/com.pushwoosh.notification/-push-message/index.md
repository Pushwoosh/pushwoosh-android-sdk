//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[PushMessage](index.md)

# PushMessage

[main]\
open class [PushMessage](index.md)

Push message data class.

## Constructors

| | |
|---|---|
| [PushMessage](-push-message.md) | [main]<br>constructor(extras: Bundle) |

## Properties

| Name | Summary |
|---|---|
| [actions](actions.md) | [main]<br>val [actions](actions.md): [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;Action&gt; |
| [badges](badges.md) | [main]<br>val [badges](badges.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [bigPictureUrl](big-picture-url.md) | [main]<br>val [bigPictureUrl](big-picture-url.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [customData](custom-data.md) | [main]<br>val [customData](custom-data.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [groupId](group-id.md) | [main]<br>val [groupId](group-id.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [header](header.md) | [main]<br>val [header](header.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [iconBackgroundColor](icon-background-color.md) | [main]<br>val [iconBackgroundColor](icon-background-color.md): [Integer](https://developer.android.com/reference/kotlin/java/lang/Integer.html) |
| [largeIconUrl](large-icon-url.md) | [main]<br>val [largeIconUrl](large-icon-url.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [led](led.md) | [main]<br>val [led](led.md): [Integer](https://developer.android.com/reference/kotlin/java/lang/Integer.html) |
| [ledOffMS](led-off-m-s.md) | [main]<br>val [ledOffMS](led-off-m-s.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [ledOnMS](led-on-m-s.md) | [main]<br>val [ledOnMS](led-on-m-s.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [message](message.md) | [main]<br>val [message](message.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [priority](priority.md) | [main]<br>val [priority](priority.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [pushHash](push-hash.md) | [main]<br>val [pushHash](push-hash.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [smallIcon](small-icon.md) | [main]<br>val [smallIcon](small-icon.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [sound](sound.md) | [main]<br>val [sound](sound.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [ticker](ticker.md) | [main]<br>val [ticker](ticker.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [vibration](vibration.md) | [main]<br>val [vibration](vibration.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [visibility](visibility.md) | [main]<br>val [visibility](visibility.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [getCampaignId](get-campaign-id.md) | [main]<br>open fun [getCampaignId](get-campaign-id.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [getMessageCode](get-message-code.md) | [main]<br>open fun [getMessageCode](get-message-code.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getMessageId](get-message-id.md) | [main]<br>open fun [getMessageId](get-message-id.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [getPushMetaData](get-push-meta-data.md) | [main]<br>open fun [getPushMetaData](get-push-meta-data.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getPushwooshNotificationId](get-pushwoosh-notification-id.md) | [main]<br>open fun [getPushwooshNotificationId](get-pushwoosh-notification-id.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [getTag](get-tag.md) | [main]<br>open fun [getTag](get-tag.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [isBadgesAdditive](is-badges-additive.md) | [main]<br>open fun [isBadgesAdditive](is-badges-additive.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isLocal](is-local.md) | [main]<br>open fun [isLocal](is-local.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isLockScreen](is-lock-screen.md) | [main]<br>open fun [isLockScreen](is-lock-screen.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isSilent](is-silent.md) | [main]<br>open fun [isSilent](is-silent.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [toBundle](to-bundle.md) | [main]<br>open fun [toBundle](to-bundle.md)(): Bundle |
| [toJson](to-json.md) | [main]<br>open fun [toJson](to-json.md)(): JSONObject |
