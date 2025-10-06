//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getUserId](get-user-id.md)

# getUserId

[main]\
open fun [getUserId](get-user-id.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)

Returns the current user identifier. 

 This method retrieves the user ID that was previously set using [setUserId](set-user-id.md). Returns null if no user ID has been set.  Example: 

```kotlin

  String userId = Pushwoosh.getInstance().getUserId();
  if (userId != null) {
      Log.d("Pushwoosh", "Current user: " + userId);
  } else {
      Log.d("Pushwoosh", "No user ID set");
  }

```

#### Return

current user id or null if not set

#### See also

| |
|---|
| [setUserId(String)](set-user-id.md) |
