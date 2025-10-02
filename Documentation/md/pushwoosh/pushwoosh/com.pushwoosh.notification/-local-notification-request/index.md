//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[LocalNotificationRequest](index.md)

# LocalNotificationRequest

[main]\
open class [LocalNotificationRequest](index.md) : [Serializable](https://developer.android.com/reference/kotlin/java/io/Serializable.html)

Manages local notification schedule.

## Constructors

| | |
|---|---|
| [LocalNotificationRequest](-local-notification-request.md) | [main]<br>constructor(requestId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [requestId](request-id.md) | [main]<br>open val [requestId](request-id.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [cancel](cancel.md) | [main]<br>open fun [cancel](cancel.md)()<br>Cancels local notification associated with this request and unschedules notification if it was not displayed yet. |
| [unschedule](unschedule.md) | [main]<br>open fun [unschedule](unschedule.md)()<br>Undo [scheduleLocalNotification](../../com.pushwoosh/-pushwoosh/schedule-local-notification.md). |
