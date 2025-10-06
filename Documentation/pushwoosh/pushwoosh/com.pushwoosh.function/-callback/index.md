//[pushwoosh](../../../index.md)/[com.pushwoosh.function](../index.md)/[Callback](index.md)

# Callback

interface [Callback](index.md)&lt;[T](index.md), [E](index.md) : [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)?&gt;

The main callback interface for handling results of asynchronous operations in Pushwoosh SDK. 

 All asynchronous SDK operations (registration, tags management, user operations) use this callback interface to return results. The callback is always invoked on the main (UI) thread, making it safe to update UI elements directly without additional thread synchronization. 

**When to use callbacks:**

- When you need to know if the operation succeeded or failed
- When you need to handle operation results (e.g., push token, tags data)
- When you want to show user feedback or update UI based on results
- When implementing retry logic for failed operations

 Most SDK methods offer both callback and fire-and-forget versions. Use the callback version when you need to handle results; use the version without callback for fire-and-forget operations. **Basic usage example:**

```kotlin

  // Register for push notifications with callback
  Pushwoosh.getInstance().registerForPushNotifications((result) -> {
      if (result.isSuccess()) {
          // Operation succeeded
          String token = result.getData().getToken();
          Log.d("App", "Push token: " + token);
      } else {
          // Operation failed
          Exception error = result.getException();
          Log.e("App", "Registration failed: " + error.getMessage());
      }
  });

```
**Example with user tags:**```kotlin

  // Set tags with error handling
  TagsBundle tags = new TagsBundle.Builder()
      .putString("Name", "John Doe")
      .putInt("Age", 25)
      .build();

  Pushwoosh.getInstance().setTags(tags, (result) -> {
      if (result.isSuccess()) {
          Log.d("App", "Tags updated successfully");
          Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
      } else {
          Log.e("App", "Failed to update tags", result.getException());
          Toast.makeText(this, "Update failed. Please try again", Toast.LENGTH_SHORT).show();
      }
  });

```
**Example with user ID management:**```kotlin

  // Set user ID with callback
  Pushwoosh.getInstance().setUserId("user_12345", (result) -> {
      if (result.isSuccess()) {
          Log.d("App", "User ID set successfully");
          // Proceed with app initialization
          initializeUserSession();
      } else {
          Log.e("App", "Failed to set user ID", result.getException());
          // Retry or show error dialog
          showRetryDialog();
      }
  });

```
**Example with retry logic:**```kotlin

  private void registerWithRetry(int attemptNumber) {
      Pushwoosh.getInstance().registerForPushNotifications((result) -> {
          if (result.isSuccess()) {
              Log.d("App", "Registration successful");
          } else {
              if (attemptNumber < MAX_RETRIES) {
                  Log.w("App", "Registration failed, retrying... Attempt " + (attemptNumber + 1));
                  new Handler().postDelayed(() -> registerWithRetry(attemptNumber + 1), 5000);
              } else {
                  Log.e("App", "Registration failed after " + MAX_RETRIES + " attempts");
                  showErrorDialog("Unable to register for notifications");
              }
          }
      });
  }

```
**Example with loading indicator:**```kotlin

  // Show loading indicator during operation
  showLoadingIndicator();

  Pushwoosh.getInstance().getTags((result) -> {
      hideLoadingIndicator();

      if (result.isSuccess()) {
          TagsBundle tags = result.getData();
          String userName = tags.getString("Name", "Unknown");
          int userAge = tags.getInt("Age", 0);
          updateUserProfile(userName, userAge);
      } else {
          Log.e("App", "Failed to load tags", result.getException());
          showError("Unable to load user profile");
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
| [Result](../-result/index.md) |
| [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md) |

## Functions

| Name | Summary |
|---|---|
| [process](process.md) | [main]<br>abstract fun [process](process.md)(result: [Result](../-result/index.md)&lt;[T](index.md), [E](index.md)&gt;)<br>Handles the result of an asynchronous operation. |
