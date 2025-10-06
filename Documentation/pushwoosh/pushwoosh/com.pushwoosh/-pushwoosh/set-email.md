//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setEmail](set-email.md)

# setEmail

[main]\
open fun [setEmail](set-email.md)(emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;)

Registers a list of email addresses for the current user without a callback. 

 This is a convenience method that calls [setEmail](set-email.md) with a null callback. Email addresses are used for email-based targeting in multichannel campaigns.  Example: 

```kotlin

  List<String> emails = new ArrayList<>();
  emails.add("user@example.com");
  emails.add("user.work@company.com");

  Pushwoosh.getInstance().setEmail(emails);

```

#### Parameters

main

| | |
|---|---|
| emails | user's emails array list |

#### See also

| |
|---|
| [setEmail(List, Callback)](set-email.md) |

[main]\
open fun [setEmail](set-email.md)(emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), [SetEmailException](../../com.pushwoosh.exception/-set-email-exception/index.md)&gt;)

Registers a list of email addresses for the current user with a callback. 

 This method associates one or more email addresses with the current user/device, enabling email-based targeting and multichannel campaigns through Pushwoosh.  Example: 

```kotlin

  List<String> emails = new ArrayList<>();
  emails.add("user@example.com");
  emails.add("user.work@company.com");

  Pushwoosh.getInstance().setEmail(emails, (result) -> {
      if (result.isSuccess()) {
          Log.d("Pushwoosh", "Emails registered successfully");
      } else {
          Exception exception = result.getException();
          Log.e("Pushwoosh", "Failed to register emails: " + exception.getMessage());
      }
  });

```

#### Parameters

main

| | |
|---|---|
| emails | user's emails array list |
| callback | setEmail operation callback |

[main]\
open fun [setEmail](set-email.md)(email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Registers a single email address for the current user without a callback. 

 This is a convenience method that calls [setEmail](set-email.md) with a null callback. The email address is used for email-based targeting in multichannel campaigns.  Example: 

```kotlin

  Pushwoosh.getInstance().setEmail("user@example.com");

```

#### Parameters

main

| | |
|---|---|
| email | user's email string |

#### See also

| |
|---|
| [setEmail(String, Callback)](set-email.md) |

[main]\
open fun [setEmail](set-email.md)(email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), [SetEmailException](../../com.pushwoosh.exception/-set-email-exception/index.md)&gt;)

Registers a single email address for the current user with a callback. 

 This method associates an email address with the current user/device, enabling email-based targeting and multichannel campaigns through Pushwoosh.  Example: 

```kotlin

  // Register user email after profile update
  private void updateUserEmail(String newEmail) {
      // Validate email first
      if (!isValidEmail(newEmail)) {
          Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
          return;
      }

      Pushwoosh.getInstance().setEmail(newEmail, (result) -> {
          if (result.isSuccess()) {
              Log.d("App", "Email registered for marketing campaigns: " + newEmail);

              // Update local user profile
              userProfile.setEmail(newEmail);
              saveUserProfile(userProfile);

              // Also update in tags
              TagsBundle emailTag = new TagsBundle.Builder()
                  .putString("Email", newEmail)
                  .putBoolean("Email_Verified", false)
                  .build();
              Pushwoosh.getInstance().setTags(emailTag);

              // Show success and send verification
              Toast.makeText(this, "Email updated successfully", Toast.LENGTH_SHORT).show();
              sendEmailVerification(newEmail);
          } else {
              Log.e("App", "Failed to register email: " + result.getException().getMessage());
              Toast.makeText(this, "Failed to update email. Please try again.", Toast.LENGTH_SHORT).show();
          }
      });
  }

```

#### Parameters

main

| | |
|---|---|
| email | user's email string |
| callback | setEmail operation callback |
