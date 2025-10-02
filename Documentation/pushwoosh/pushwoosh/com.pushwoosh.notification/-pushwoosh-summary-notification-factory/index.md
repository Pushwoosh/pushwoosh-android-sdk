//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[PushwooshSummaryNotificationFactory](index.md)

# PushwooshSummaryNotificationFactory

[main]\
open class [PushwooshSummaryNotificationFactory](index.md) : [SummaryNotificationFactory](../-summary-notification-factory/index.md)

## Constructors

| | |
|---|---|
| [PushwooshSummaryNotificationFactory](-pushwoosh-summary-notification-factory.md) | [main]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID](../-summary-notification-factory/-n-e-e-d_-t-o_-a-d-d_-n-e-w_-n-o-t-i-f-i-c-a-t-i-o-n_-c-h-a-n-n-e-l_-i-d.md) | [main]<br>open var [NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID](../-summary-notification-factory/-n-e-e-d_-t-o_-a-d-d_-n-e-w_-n-o-t-i-f-i-c-a-t-i-o-n_-c-h-a-n-n-e-l_-i-d.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |

## Functions

| Name | Summary |
|---|---|
| [autoCancelSummaryNotification](../-summary-notification-factory/auto-cancel-summary-notification.md) | [main]<br>open fun [autoCancelSummaryNotification](../-summary-notification-factory/auto-cancel-summary-notification.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Override this method to set whether the summary notification will be dismissed after the user opens it. |
| [getNotificationIntent](../-summary-notification-factory/get-notification-intent.md) | [main]<br>open fun [getNotificationIntent](../-summary-notification-factory/get-notification-intent.md)(): Intent |
| [onGenerateSummaryNotification](../-summary-notification-factory/on-generate-summary-notification.md) | [main]<br>fun [onGenerateSummaryNotification](../-summary-notification-factory/on-generate-summary-notification.md)(notificationsAmount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), notificationChannelId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), groupId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): Notification |
| [shouldGenerateSummaryNotification](../-summary-notification-factory/should-generate-summary-notification.md) | [main]<br>open fun [shouldGenerateSummaryNotification](../-summary-notification-factory/should-generate-summary-notification.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [summaryNotificationColor](summary-notification-color.md) | [main]<br>open fun [summaryNotificationColor](summary-notification-color.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Override this method to set the icon color. |
| [summaryNotificationIconResId](summary-notification-icon-res-id.md) | [main]<br>open fun [summaryNotificationIconResId](summary-notification-icon-res-id.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Override this method to set your drawable as an icon of the group summary notification. |
| [summaryNotificationMessage](summary-notification-message.md) | [main]<br>open fun [summaryNotificationMessage](summary-notification-message.md)(notificationsAmount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)<br>Override this method to set your custom message of the group summary notification. |
