//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[SummaryNotificationFactory](index.md)/[summaryNotificationMessage](summary-notification-message.md)

# summaryNotificationMessage

[main]\
abstract fun [summaryNotificationMessage](summary-notification-message.md)(notificationsAmount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)

Override this method to set your custom message of the group summary notification.

#### Return

Group summary notification message. By default returns &quot;{@param notificationsAmount} new messages&quot;.

#### Parameters

main

| | |
|---|---|
| notificationsAmount | - number of the notifications in the group summary |
