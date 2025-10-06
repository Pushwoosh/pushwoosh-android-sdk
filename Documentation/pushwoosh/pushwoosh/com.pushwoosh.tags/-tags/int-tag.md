//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[intTag](int-tag.md)

# intTag

[main]\
open fun [intTag](int-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle with a single integer tag. 

 Integer tags are ideal for storing numeric values like age, counters, version numbers, scores, or any whole number attribute. Use this for quick single-tag updates. 

**Common Use Cases:**

- User demographics - age, family size, number of children
- Counters - total purchases, items in cart, app sessions
- Loyalty metrics - reward points, tier level, achievement count
- App versioning - app version code, feature flags
- Engagement metrics - articles read, videos watched, products viewed

 Example: ```kotlin

	  // E-commerce: Track user demographics and activity
	  Pushwoosh.getInstance().setTags(Tags.intTag("Age", 28));
	  Pushwoosh.getInstance().setTags(Tags.intTag("Items_In_Cart", 3));
	  Pushwoosh.getInstance().setTags(Tags.intTag("Total_Purchases", 12));
	  Pushwoosh.getInstance().setTags(Tags.intTag("Loyalty_Points", 450));
	
	  // News app: Track reading behavior
	  Pushwoosh.getInstance().setTags(Tags.intTag("Articles_Read_Today", 7));
	  Pushwoosh.getInstance().setTags(Tags.intTag("Reading_Streak_Days", 15));
	
	  // Gaming app: Track player progress
	  Pushwoosh.getInstance().setTags(Tags.intTag("Player_Level", 42));
	  Pushwoosh.getInstance().setTags(Tags.intTag("Total_Score", 98750));
	  Pushwoosh.getInstance().setTags(Tags.intTag("Lives_Remaining", 3));
	
	  // Fitness app: Track workout metrics
	  Pushwoosh.getInstance().setTags(Tags.intTag("Workouts_This_Month", 18));
	  Pushwoosh.getInstance().setTags(Tags.intTag("Calories_Burned_Today", 520));
	
	  // App version tracking
	  Pushwoosh.getInstance().setTags(Tags.intTag("App_Version_Code", BuildConfig.VERSION_CODE));
	
```

#### Return

TagsBundle containing the single integer tag

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;Age&quot;, &quot;Total_Purchases&quot;, &quot;Loyalty_Points&quot;) |
| value | integer tag value |

#### See also

| |
|---|
| [incrementInt(String, int)](increment-int.md) |
| [TagsBundle.Builder](../-tags-bundle/-builder/put-int.md) |
