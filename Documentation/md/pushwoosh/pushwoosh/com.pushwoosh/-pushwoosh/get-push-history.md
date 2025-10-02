//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getPushHistory](get-push-history.md)

# getPushHistory

[main]\
open fun [getPushHistory](get-push-history.md)(): [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[PushMessage](../../com.pushwoosh.notification/-push-message/index.md)&gt;

Gets push notification history. History contains both remote and local notifications.

#### Return

Push history as List of [com.pushwoosh.notification.PushMessage](../../com.pushwoosh.notification/-push-message/index.md). Maximum of [PUSH_HISTORY_CAPACITY](-p-u-s-h_-h-i-s-t-o-r-y_-c-a-p-a-c-i-t-y.md) pushes are returned
