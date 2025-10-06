//[pushwoosh](../../../index.md)/[com.pushwoosh.function](../index.md)/[Callback](index.md)/[process](process.md)

# process

[main]\
abstract fun [process](process.md)(result: [Result](../-result/index.md)&lt;[T](index.md), [E](index.md)&gt;)

Handles the result of an asynchronous operation. 

 This method is invoked on the main (UI) thread when an asynchronous SDK operation completes, regardless of whether it succeeded or failed. The [Result](../-result/index.md) parameter encapsulates either the successful result data or an exception describing the failure. 

**Thread safety:** This method is always called on the main thread, making it safe to update UI components directly without posting to a handler or using runOnUiThread(). 

**Usage pattern:**

- Check [isSuccess](../-result/is-success.md) to determine if operation succeeded
- Use getData to access successful result (may be null for Void operations)
- Use getException to access error information on failure

**Example with push registration:**```kotlin

	  
	  public void process(@NonNull Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result) {
	      if (result.isSuccess()) {
	          RegisterForPushNotificationsResultData data = result.getData();
	          String pushToken = data.getToken();
	          boolean notificationsEnabled = data.isEnabled();
	
	          Log.d("App", "Push token: " + pushToken);
	          Log.d("App", "Notifications enabled: " + notificationsEnabled);
	
	          // Update UI
	          statusTextView.setText("Push notifications enabled");
	      } else {
	          RegisterForPushNotificationsException exception = result.getException();
	          Log.e("App", "Registration failed: " + exception.getMessage(), exception);
	
	          // Show error to user
	          AlertDialog.Builder builder = new AlertDialog.Builder(this);
	          builder.setTitle("Registration Failed")
	              .setMessage("Unable to register for push notifications: " + exception.getMessage())
	              .setPositiveButton("Retry", (dialog, which) -> retryRegistration())
	              .setNegativeButton("Cancel", null)
	              .show();
	      }
	  }
	
```
**Example with tags management:**```kotlin

	  
	  public void process(@NonNull Result<Void, PushwooshException> result) {
	      if (result.isSuccess()) {
	          // result.getData() is null for Void operations
	          Log.d("App", "Tags set successfully");
	          Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
	      } else {
	          PushwooshException exception = result.getException();
	          Log.e("App", "Failed to set tags: " + exception.getMessage(), exception);
	
	          // Determine error type and respond accordingly
	          if (exception.getMessage().contains("network")) {
	              Toast.makeText(this, "Network error. Please check your connection", Toast.LENGTH_LONG).show();
	          } else {
	              Toast.makeText(this, "Update failed. Please try again", Toast.LENGTH_SHORT).show();
	          }
	      }
	  }
	
```
**Example with getting tags:**```kotlin

	  
	  public void process(@NonNull Result<TagsBundle, GetTagsException> result) {
	      if (result.isSuccess()) {
	          TagsBundle tags = result.getData();
	
	          // Safely access tag values with defaults
	          String userName = tags.getString("Name", "Guest");
	          int userAge = tags.getInt("Age", 0);
	          String subscription = tags.getString("Subscription", "free");
	
	          Log.d("App", "User: " + userName + ", Age: " + userAge + ", Plan: " + subscription);
	
	          // Update UI with user data
	          nameTextView.setText(userName);
	          subscriptionBadge.setText(subscription.toUpperCase());
	      } else {
	          GetTagsException exception = result.getException();
	          Log.e("App", "Failed to get tags: " + exception.getMessage(), exception);
	          // Show error or use cached data
	          loadCachedUserData();
	      }
	  }
	
```
**Example with user ID operations:**```kotlin

	  
	  public void process(@NonNull Result<Void, SetUserIdException> result) {
	      if (result.isSuccess()) {
	          Log.d("App", "User ID set successfully");
	          // Continue with user initialization
	          loadUserProfile();
	          syncUserData();
	      } else {
	          SetUserIdException exception = result.getException();
	          Log.e("App", "Failed to set user ID: " + exception.getMessage(), exception);
	
	          // Critical error - user tracking won't work
	          AlertDialog.Builder builder = new AlertDialog.Builder(this);
	          builder.setTitle("Setup Error")
	              .setMessage("Failed to initialize user tracking. Some features may not work correctly.")
	              .setPositiveButton("Retry", (dialog, which) -> retryUserIdSetup())
	              .setNegativeButton("Continue Anyway", (dialog, which) -> continueWithoutTracking())
	              .setCancelable(false)
	              .show();
	      }
	  }
	
```

#### Parameters

main

| | |
|---|---|
| result | the result of the asynchronous operation, containing either success data or failure exception |
