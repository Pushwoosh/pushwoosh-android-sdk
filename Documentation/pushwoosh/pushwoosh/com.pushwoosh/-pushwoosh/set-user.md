//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setUser](set-user.md)

# setUser

[main]\
open fun [setUser](set-user.md)(userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;)

Sets the user identifier and registers associated email addresses without a callback. 

 This is a convenience method that calls [setUser](set-user.md) with a null callback. This method combines setting a user ID with registering email addresses in a single operation.  Example: 

```kotlin

  // Set user with multiple email addresses after registration
  private void completeUserRegistration(User user) {
      List<String> userEmails = new ArrayList<>();
      userEmails.add(user.getPrimaryEmail());

      // Add work email if provided
      if (user.getWorkEmail() != null) {
          userEmails.add(user.getWorkEmail());
      }

      // Set user ID and associate all email addresses
      Pushwoosh.getInstance().setUser(user.getUserId(), userEmails);

      Log.d("App", "User registered with " + userEmails.size() + " email(s)");
  }

```

#### Parameters

main

| | |
|---|---|
| userId | user identifier |
| emails | user's emails array list |

#### See also

| |
|---|
| [setUser(String, List, Callback)](set-user.md) |

[main]\
open fun [setUser](set-user.md)(userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), [SetUserException](../../com.pushwoosh.exception/-set-user-exception/index.md)&gt;)

Sets the user identifier and registers associated email addresses with a callback. 

 This method sets a user identifier and associates one or more email addresses with the user. The user ID can be a Facebook ID, username, or any unique identifier. This enables cross-device tracking and email-based targeting for multichannel campaigns.  Example: 

```kotlin

  // Set user with email addresses and handle result
  private void linkUserAccount(User user) {
      List<String> userEmails = new ArrayList<>();
      userEmails.add(user.getPrimaryEmail());

      // Add secondary emails if available
      if (user.hasSecondaryEmails()) {
          userEmails.addAll(user.getSecondaryEmails());
      }

      Pushwoosh.getInstance().setUser(user.getUserId(), userEmails, (result) -> {
          if (result.isSuccess()) {
              Log.d("App", "User account linked successfully with " + userEmails.size() + " email(s)");

              // Update user profile with additional data
              TagsBundle profileTags = new TagsBundle.Builder()
                  .putString("Name", user.getName())
                  .putString("Account_Status", "verified")
                  .putDate("Account_Created", user.getCreatedAt())
                  .build();
              Pushwoosh.getInstance().setTags(profileTags);

              // Show confirmation to user
              Toast.makeText(this, "Account setup complete!", Toast.LENGTH_SHORT).show();
          } else {
              Log.e("App", "Failed to link account: " + result.getException().getMessage());
              // Show error but allow user to continue
              Toast.makeText(this, "Account created. Email sync pending.", Toast.LENGTH_SHORT).show();
          }
      });
  }

```

#### Parameters

main

| | |
|---|---|
| userId | user identifier |
| emails | user's emails array list |
| callback | setUser operation callback |
