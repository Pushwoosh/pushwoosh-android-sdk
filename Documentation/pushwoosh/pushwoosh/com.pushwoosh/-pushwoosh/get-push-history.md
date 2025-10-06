//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getPushHistory](get-push-history.md)

# getPushHistory

[main]\
open fun [getPushHistory](get-push-history.md)(): [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[PushMessage](../../com.pushwoosh.notification/-push-message/index.md)&gt;

Returns the push notification history. 

 This method retrieves a list of recently received push notifications, including both remote and local notifications. The history is limited to [PUSH_HISTORY_CAPACITY](-p-u-s-h_-h-i-s-t-o-r-y_-c-a-p-a-c-i-t-y.md) most recent pushes.  Example: 

```kotlin

  List<PushMessage> history = Pushwoosh.getInstance().getPushHistory();
  for (PushMessage message : history) {
      Log.d("Pushwoosh", "Message: " + message.getMessage());
      Log.d("Pushwoosh", "Received at: " + message.getTimestamp());
  }

```

#### Return

Push history as List of [com.pushwoosh.notification.PushMessage](../../com.pushwoosh.notification/-push-message/index.md). Maximum of [PUSH_HISTORY_CAPACITY](-p-u-s-h_-h-i-s-t-o-r-y_-c-a-p-a-c-i-t-y.md) pushes are returned
