//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[SummaryNotificationFactory](index.md)

# SummaryNotificationFactory

abstract class [SummaryNotificationFactory](index.md)

#### Inheritors

| |
|---|
| [PushwooshSummaryNotificationFactory](../-pushwoosh-summary-notification-factory/index.md) |

## Properties

| Name | Summary |
|---|---|
| [NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID](-n-e-e-d_-t-o_-a-d-d_-n-e-w_-n-o-t-i-f-i-c-a-t-i-o-n_-c-h-a-n-n-e-l_-i-d.md) | [main]<br>open var [NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID](-n-e-e-d_-t-o_-a-d-d_-n-e-w_-n-o-t-i-f-i-c-a-t-i-o-n_-c-h-a-n-n-e-l_-i-d.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |

## Functions

| Name | Summary |
|---|---|
| [autoCancelSummaryNotification](auto-cancel-summary-notification.md) | [main]<br>open fun [autoCancelSummaryNotification](auto-cancel-summary-notification.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Override this method to set whether the summary notification will be dismissed after the user opens it. |
| [getNotificationIntent](get-notification-intent.md) | [main]<br>open fun [getNotificationIntent](get-notification-intent.md)(): Intent |
| [onGenerateSummaryNotification](on-generate-summary-notification.md) | [main]<br>fun [onGenerateSummaryNotification](on-generate-summary-notification.md)(notificationsAmount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), notificationChannelId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), groupId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): Notification |
| [shouldGenerateSummaryNotification](should-generate-summary-notification.md) | [main]<br>open fun [shouldGenerateSummaryNotification](should-generate-summary-notification.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [summaryNotificationColor](summary-notification-color.md) | [main]<br>abstract fun [summaryNotificationColor](summary-notification-color.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Override this method to set the icon color. |
| [summaryNotificationIconResId](summary-notification-icon-res-id.md) | [main]<br>abstract fun [summaryNotificationIconResId](summary-notification-icon-res-id.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Override this method to set your drawable as an icon of the group summary notification. |
| [summaryNotificationMessage](summary-notification-message.md) | [main]<br>abstract fun [summaryNotificationMessage](summary-notification-message.md)(notificationsAmount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)<br>Override this method to set your custom message of the group summary notification. |
