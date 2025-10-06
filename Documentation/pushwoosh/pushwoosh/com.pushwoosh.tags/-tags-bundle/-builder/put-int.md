//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[putInt](put-int.md)

# putInt

[main]\
open fun [putInt](put-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TagsBundle.Builder](index.md)

Adds a tag with an integer value. 

 Use for demographics (age, zip code), counters (items in cart, articles read), scores (loyalty points, game level), or other numeric attributes that fit within the integer range (-2,147,483,648 to 2,147,483,647). 

Example (E-commerce App):

```kotlin

		new TagsBundle.Builder()
		    .putInt("age", 35)
		    .putInt("cart_items", 3)
		    .putInt("loyalty_points", 1250)
		    .putInt("items_viewed_today", 12)
		    .build();
		
```

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;age&quot;, &quot;loyalty_points&quot;, &quot;cart_items&quot;) |
| value | integer value to store |
