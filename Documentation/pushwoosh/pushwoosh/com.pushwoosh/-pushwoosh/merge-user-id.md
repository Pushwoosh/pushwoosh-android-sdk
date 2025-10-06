//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[mergeUserId](merge-user-id.md)

# mergeUserId

[main]\
open fun [mergeUserId](merge-user-id.md)(oldUserId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), newUserId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), doMerge: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), [MergeUserException](../../com.pushwoosh.exception/-merge-user-exception/index.md)&gt;)

Merges or removes event statistics for a user identifier. 

 This method either moves all event statistics from oldUserId to newUserId (if doMerge is true) or removes all events associated with oldUserId (if doMerge is false). This is useful when migrating user accounts or cleaning up data.  Example: 

```kotlin

  // Merge user data when user logs in with different account
  Pushwoosh.getInstance().mergeUserId(
      "temp_user_123",
      "permanent_user_456",
      true, // merge events
      (result) -> {
          if (result.isSuccess()) {
              Log.d("Pushwoosh", "User data merged successfully");
          } else {
              Log.e("Pushwoosh", "Merge failed: " + result.getException().getMessage());
          }
      }
  );

  // Remove old user data without merging
  Pushwoosh.getInstance().mergeUserId(
      "old_user_123",
      "new_user_456",
      false, // remove old events
      null
  );

```

#### Parameters

main

| | |
|---|---|
| oldUserId | source user identifier |
| newUserId | destination user identifier |
| doMerge | true to merge events from oldUserId to newUserId, false to remove events for oldUserId |
| callback | method completion callback |
