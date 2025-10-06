//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[putStringIfNotEmpty](put-string-if-not-empty.md)

# putStringIfNotEmpty

[main]\
open fun [putStringIfNotEmpty](put-string-if-not-empty.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle.Builder](index.md)

Adds a tag with a string value only if the value is not null or empty. 

 This is a convenience method that validates the string value before adding it to the bundle. If the value is null or empty (zero-length or whitespace-only), the tag is not added. Use this when you want to avoid setting tags with empty or meaningless values. 

Example (Form Validation):

```kotlin

		// Safely add optional profile fields from user input
		String phoneNumber = editTextPhone.getText().toString().trim();
		String company = editTextCompany.getText().toString().trim();
		String referralCode = editTextReferral.getText().toString().trim();
		
		new TagsBundle.Builder()
		    .putString("email", email) // Required field, always set
		    .putStringIfNotEmpty("phone", phoneNumber) // Optional, only if provided
		    .putStringIfNotEmpty("company", company) // Optional, only if provided
		    .putStringIfNotEmpty("referral_code", referralCode) // Optional, only if provided
		    .build();
		
```

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;phone&quot;, &quot;company&quot;, &quot;optional_field&quot;) |
| value | string value to store (will only be added if not null or empty) |
