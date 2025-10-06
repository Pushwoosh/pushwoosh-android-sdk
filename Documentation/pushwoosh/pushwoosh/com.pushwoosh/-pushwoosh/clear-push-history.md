//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[clearPushHistory](clear-push-history.md)

# clearPushHistory

[main]\
open fun [clearPushHistory](clear-push-history.md)()

Clears the push notification history. 

 This method removes all stored push notifications from the history. Usually called after processing the history retrieved via [getPushHistory](get-push-history.md).  Example: 

```kotlin

  // Get and process push history
  List<PushMessage> history = Pushwoosh.getInstance().getPushHistory();
  processHistory(history);

  // Clear history after processing
  Pushwoosh.getInstance().clearPushHistory();

```
