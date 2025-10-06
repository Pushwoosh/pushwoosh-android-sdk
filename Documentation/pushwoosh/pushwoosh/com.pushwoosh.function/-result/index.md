//[pushwoosh](../../../index.md)/[com.pushwoosh.function](../index.md)/[Result](index.md)

# Result

open class [Result](index.md)&lt;[T](index.md), [E](index.md) : [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)?&gt;

Encapsulates the result of an asynchronous operation, providing a type-safe way to handle both success and failure cases. 

 This class follows the Result pattern (also known as Either or Try pattern) to represent operations that can succeed with data or fail with an exception. It ensures null-safety by guaranteeing that exactly one of the two fields (data or exception) will be non-null, never both or neither. 

**Key characteristics:**

- Type-safe: Success data type (T) and error type (E) are explicitly defined
- Null-safe: Either data or exception is non-null, but never both
- Immutable: Once created, the result cannot be changed
- Thread-safe: Can be safely passed between threads

**Usage pattern:** Always check [isSuccess](is-success.md) first to determine if the operation succeeded, then access either getData for successful results or getException for errors. **Basic success/failure handling:**

```kotlin

  Pushwoosh.getInstance().registerForPushNotifications((result) -> {
      if (result.isSuccess()) {
          // Safe to access data
          RegisterForPushNotificationsResultData data = result.getData();
          String token = data.getToken();
          Log.d("App", "Registered successfully. Token: " + token);
      } else {
          // Safe to access exception
          RegisterForPushNotificationsException exception = result.getException();
          Log.e("App", "Registration failed: " + exception.getMessage());
      }
  });

```
**Null-safe data handling (Void operations):**```kotlin

  // Some operations like setTags() return Void on success
  Pushwoosh.getInstance().setTags(tags, (result) -> {
      if (result.isSuccess()) {
          // result.getData() is null for Void operations
          Log.d("App", "Tags set successfully");
      } else {
          PushwooshException exception = result.getException();
          Log.e("App", "Failed to set tags: " + exception.getMessage());
      }
  });

```
**Detailed error handling with exception types:**```kotlin

  Pushwoosh.getInstance().registerForPushNotifications((result) -> {
      if (result.isSuccess()) {
          String token = result.getData().getToken();
          saveTokenToPreferences(token);
      } else {
          Exception exception = result.getException();

          // Log full exception with stack trace
          Log.e("App", "Registration failed", exception);

          // Handle specific error types
          if (exception instanceof NetworkException) {
              showToast("Network error. Please check your connection.");
          } else if (exception.getMessage().contains("Play Services")) {
              showPlayServicesUpdateDialog();
          } else {
              showToast("Registration failed: " + exception.getMessage());
          }
      }
  });

```
**Working with nullable data (tags retrieval):**```kotlin

  Pushwoosh.getInstance().getTags((result) -> {
      if (result.isSuccess()) {
          TagsBundle tags = result.getData(); // Never null on success

          // Safe access with default values
          String name = tags.getString("Name", "Guest");
          int age = tags.getInt("Age", 0);
          List<String> interests = tags.getList("Interests", Collections.emptyList());

          Log.d("App", "User: " + name + ", Age: " + age);
          Log.d("App", "Interests: " + interests);
      } else {
          Log.e("App", "Failed to retrieve tags", result.getException());
      }
  });

```
**Conditional logic based on success/failure:**```kotlin

  Pushwoosh.getInstance().setUserId("user_12345", (result) -> {
      if (result.isSuccess()) {
          // Proceed with app initialization
          loadUserProfile();
          syncUserData();
          enablePushFeatures();
      } else {
          // Fallback to anonymous mode
          Log.w("App", "User ID not set, using anonymous mode", result.getException());
          enableAnonymousMode();
      }
  });

```
**Logging best practices:**```kotlin

  Pushwoosh.getInstance().registerForPushNotifications((result) -> {
      if (result.isSuccess()) {
          RegisterForPushNotificationsResultData data = result.getData();
          Log.d("App", "Push registration successful");
          Log.d("App", "  Token: " + data.getToken());
          Log.d("App", "  Notifications enabled: " + data.isEnabled());

          // Send to analytics
          Analytics.logEvent("push_registered", "token", data.getToken());
      } else {
          Exception exception = result.getException();
          // Log with exception for full stack trace
          Log.e("App", "Push registration failed: " + exception.getMessage(), exception);

          // Send to crash reporting
          Crashlytics.recordException(exception);
      }
  });

```

#### Parameters

main

| | |
|---|---|
| &lt;T&gt; | the type of data returned on successful operation completion |
| &lt;E&gt; | the type of exception returned on operation failure, must extend [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md) |

#### See also

| |
|---|
| [Callback](../-callback/index.md) |
| [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md) |

## Properties

| Name | Summary |
|---|---|
| [data](data.md) | [main]<br>val [data](data.md): [T](index.md) |
| [exception](exception.md) | [main]<br>val [exception](exception.md): [E](index.md) |

## Functions

| Name | Summary |
|---|---|
| [from](from.md) | [main]<br>open fun &lt;[T](from.md), [E](from.md) : [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)?&gt; [from](from.md)(data: [T](from.md), exception: [E](from.md)): [Result](index.md)&lt;[T](from.md), [E](from.md)&gt;<br>Creates a result with both data and exception fields. |
| [fromData](from-data.md) | [main]<br>open fun &lt;[T](from-data.md), [E](from-data.md) : [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)?&gt; [fromData](from-data.md)(data: [T](from-data.md)): [Result](index.md)&lt;[T](from-data.md), [E](from-data.md)&gt;<br>Creates a successful result containing the specified data. |
| [fromException](from-exception.md) | [main]<br>open fun &lt;[T](from-exception.md), [E](from-exception.md) : [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)?&gt; [fromException](from-exception.md)(exception: [E](from-exception.md)): [Result](index.md)&lt;[T](from-exception.md), [E](from-exception.md)&gt;<br>Creates a failed result containing the specified exception. |
| [isSuccess](is-success.md) | [main]<br>open fun [isSuccess](is-success.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks whether the asynchronous operation completed successfully. |
