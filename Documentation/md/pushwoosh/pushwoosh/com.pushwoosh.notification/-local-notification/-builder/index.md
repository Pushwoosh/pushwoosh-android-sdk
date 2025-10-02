//[pushwoosh](../../../../index.md)/[com.pushwoosh.notification](../../index.md)/[LocalNotification](../index.md)/[Builder](index.md)

# Builder

[main]\
open class [Builder](index.md)

LocalNotification Builder.

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [main]<br>open fun [build](build.md)(): [LocalNotification](../index.md)<br>Builds and returns LocalNotification. |
| [setBanner](set-banner.md) | [main]<br>open fun [setBanner](set-banner.md)(url: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [LocalNotification.Builder](index.md)<br>Sets image for notification [BigPictureStyle](https://developer.android.com/reference/android/app/Notification.BigPictureStyle.html) |
| [setDelay](set-delay.md) | [main]<br>open fun [setDelay](set-delay.md)(delay: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [LocalNotification.Builder](index.md)<br>Sets the delay after which notification will be displayed. |
| [setExtras](set-extras.md) | [main]<br>open fun [setExtras](set-extras.md)(extras: Bundle): [LocalNotification.Builder](index.md)<br>Sets custom notification bundle. |
| [setLargeIcon](set-large-icon.md) | [main]<br>open fun [setLargeIcon](set-large-icon.md)(url: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [LocalNotification.Builder](index.md)<br>Sets large icon image. |
| [setLink](set-link.md) | [main]<br>open fun [setLink](set-link.md)(url: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [LocalNotification.Builder](index.md)<br>Sets url link that will be open in browser instead of default launcher activity after clicking on notification. |
| [setMessage](set-message.md) | [main]<br>open fun [setMessage](set-message.md)(message: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [LocalNotification.Builder](index.md)<br>Sets notification content. |
| [setSmallIcon](set-small-icon.md) | [main]<br>open fun [setSmallIcon](set-small-icon.md)(name: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [LocalNotification.Builder](index.md)<br>Sets small icon image. |
| [setTag](set-tag.md) | [main]<br>open fun [setTag](set-tag.md)(tag: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [LocalNotification.Builder](index.md)<br>Sets notification tag that is used to distinguish different notifications. |
