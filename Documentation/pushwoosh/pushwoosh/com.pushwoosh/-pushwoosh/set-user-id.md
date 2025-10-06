//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setUserId](set-user-id.md)

# setUserId

[main]\
open fun [setUserId](set-user-id.md)(userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Sets the user identifier without a callback. 

 This is a convenience method that calls [setUserId](set-user-id.md) with a null callback. The user identifier can be a Facebook ID, username, email, or any unique user ID that allows data and events to be matched across multiple devices.  Example: 

```kotlin

  // Set user ID after successful login
  private void onLoginSuccess(User user) {
      // Associate device with user account for cross-device tracking
      Pushwoosh.getInstance().setUserId(user.getUserId());

      // Also update user profile tags
      TagsBundle userProfile = new TagsBundle.Builder()
          .putString("Name", user.getName())
          .putString("Email", user.getEmail())
          .putDate("Last_Login", new Date())
          .build();
      Pushwoosh.getInstance().setTags(userProfile);

      Log.d("App", "User logged in: " + user.getUserId());
  }

```

#### Parameters

main

| | |
|---|---|
| userId | user identifier |

#### See also

| |
|---|
| [setUserId(String, Callback)](set-user-id.md) |

[main]\
open fun [setUserId](set-user-id.md)(userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), [SetUserIdException](../../com.pushwoosh.exception/-set-user-id-exception/index.md)&gt;)

Sets the user identifier with a callback. 

 This method associates a user identifier with the current device. The user ID can be a Facebook ID, username, email, or any unique identifier. This enables cross-device tracking and allows you to target users across all their devices.  Example: 

```kotlin

  // Set user ID with callback after authentication
  private void authenticateUser(String username, String password) {
      authService.login(username, password, new AuthCallback() {
          
          public void onSuccess(User user) {
              // Set user ID in Pushwoosh with callback
              Pushwoosh.getInstance().setUserId(user.getUserId(), (result) -> {
                  if (result.isSuccess()) {
                      Log.d("App", "User ID set successfully for cross-device tracking");

                      // Update additional user data
                      TagsBundle userTags = new TagsBundle.Builder()
                          .putString("Username", user.getUsername())
                          .putString("Account_Type", user.getAccountType())
                          .putDate("Last_Login", new Date())
                          .build();
                      Pushwoosh.getInstance().setTags(userTags);

                      // Navigate to home screen
                      startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                      finish();
                  } else {
                      Log.e("App", "Failed to set user ID: " + result.getException().getMessage());
                      // Continue anyway as this is not critical for login
                      startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                      finish();
                  }
              });
          }

          
          public void onError(String error) {
              Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_SHORT).show();
          }
      });
  }

```

#### Parameters

main

| | |
|---|---|
| userId | user identifier |
| callback | setUserId operation callback |
