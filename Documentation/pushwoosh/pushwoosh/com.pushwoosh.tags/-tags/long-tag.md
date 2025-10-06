//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[longTag](long-tag.md)

# longTag

[main]\
open fun [longTag](long-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle with a single long integer tag. 

 Long tags are used for storing large numeric values that exceed the integer range, such as timestamps in milliseconds, large user IDs, or big numeric identifiers. 

**Common Use Cases:**

- Timestamps - Unix timestamps in milliseconds, event times
- Large identifiers - user IDs, order numbers, transaction IDs
- Big numeric values - total revenue in cents, large counters
- Database IDs - primary keys from backend systems

 Example: ```kotlin

	  // Store Unix timestamp in milliseconds
	  long lastLoginTime = System.currentTimeMillis();
	  Pushwoosh.getInstance().setTags(Tags.longTag("Last_Login_Timestamp", lastLoginTime));
	
	  // Store large user ID from backend
	  long userId = 9876543210123L;
	  Pushwoosh.getInstance().setTags(Tags.longTag("Backend_User_ID", userId));
	
	  // E-commerce: Store order number
	  long orderNumber = 2024031500000123L;
	  Pushwoosh.getInstance().setTags(Tags.longTag("Last_Order_Number", orderNumber));
	
	  // Banking app: Store transaction ID
	  long transactionId = 8765432109876543L;
	  Pushwoosh.getInstance().setTags(Tags.longTag("Last_Transaction_ID", transactionId));
	
	  // Track total lifetime revenue in cents
	  long lifetimeRevenueCents = 12450000L; // $124,500.00
	  Pushwoosh.getInstance().setTags(Tags.longTag("Lifetime_Revenue_Cents", lifetimeRevenueCents));
	
```

#### Return

TagsBundle containing the single long tag

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;Last_Login_Timestamp&quot;, &quot;Backend_User_ID&quot;, &quot;Last_Order_Number&quot;) |
| value | long integer tag value |

#### See also

| |
|---|
| [TagsBundle.Builder](../-tags-bundle/-builder/put-long.md) |
