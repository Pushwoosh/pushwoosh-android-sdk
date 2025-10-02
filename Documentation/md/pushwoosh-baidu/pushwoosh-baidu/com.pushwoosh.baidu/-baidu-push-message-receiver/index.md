//[pushwoosh-baidu](../../../index.md)/[com.pushwoosh.baidu](../index.md)/[BaiduPushMessageReceiver](index.md)

# BaiduPushMessageReceiver

[main]\
open class [BaiduPushMessageReceiver](index.md)

## Constructors

| | |
|---|---|
| [BaiduPushMessageReceiver](-baidu-push-message-receiver.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [onBind](on-bind.md) | [main]<br>open fun [onBind](on-bind.md)(context: Context, errorCode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), appid: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), channelId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), requestId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)) |
| [onDelTags](on-del-tags.md) | [main]<br>open fun [onDelTags](on-del-tags.md)(context: Context, errorCode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), successTags: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, failTags: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, requestId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)) |
| [onListTags](on-list-tags.md) | [main]<br>open fun [onListTags](on-list-tags.md)(context: Context, errorCode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), tags: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, requestId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)) |
| [onMessage](on-message.md) | [main]<br>open fun [onMessage](on-message.md)(context: Context, message: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), customContentString: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)) |
| [onNotificationArrived](on-notification-arrived.md) | [main]<br>open fun [onNotificationArrived](on-notification-arrived.md)(context: Context, title: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), description: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), customContentString: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)) |
| [onNotificationClicked](on-notification-clicked.md) | [main]<br>open fun [onNotificationClicked](on-notification-clicked.md)(context: Context, title: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), description: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), customContentString: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)) |
| [onSetTags](on-set-tags.md) | [main]<br>open fun [onSetTags](on-set-tags.md)(context: Context, errorCode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), successTags: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, failTags: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, requestId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)) |
| [onUnbind](on-unbind.md) | [main]<br>open fun [onUnbind](on-unbind.md)(context: Context, errorCode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), requestId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)) |
