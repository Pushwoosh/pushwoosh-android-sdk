//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[TagsBundle](index.md)/[getInt](get-int.md)

# getInt

[main]\
open fun [getInt](get-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), defaultValue: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Retrieves an integer tag value by name, with a fallback default value. 

 Returns the integer value associated with the given key. If the tag doesn't exist or cannot be converted to an integer, returns the provided default value. This method safely handles type conversion from any Number type. 

Example (Reading User Profile):

```kotlin

	TagsBundle tags = getUserTags(); // Get from somewhere
	
	int age = tags.getInt("age", 0); // Returns age or 0 if not set
	int loyaltyPoints = tags.getInt("loyalty_points", 0);
	int cartItems = tags.getInt("cart_items", 0);
	
	// Use in business logic
	if (age >= 18 && loyaltyPoints > 1000) {
	    showPremiumOffer();
	}
	
```

#### Return

tag value as integer, or defaultValue if tag is not found or not a number

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;age&quot;, &quot;loyalty_points&quot;, &quot;level&quot;) |
| defaultValue | value to return if tag doesn't exist or cannot be converted to int |
