//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[incrementInt](increment-int.md)

# incrementInt

[main]\
open fun [incrementInt](increment-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), delta: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle that increments an integer tag value. 

 This method adds the specified delta value to the existing tag value on the server. It's perfect for tracking counters, scores, and accumulative metrics without needing to know the current value. The operation is atomic and handled server-side. 

**Common Use Cases:**

- Purchase tracking - increment total purchases, order count
- Loyalty programs - add points, increase reward balance
- Engagement metrics - increment article reads, videos watched, sessions
- Gaming - add to score, increase level, award coins
- Activity counters - track app opens, feature usage, interactions

**Important:** Use positive values to increment and negative values to decrement. The tag must already exist with an integer value, or it will be created with the delta value.  Example: 

```kotlin

	  // E-commerce: Track purchase activity
	  private void onPurchaseCompleted(Order order) {
	      // Increment total purchase count
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Purchases", 1));
	
	      // Add loyalty points based on order value
	      int pointsEarned = (int) (order.getTotal() * 10); // 10 points per dollar
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Loyalty_Points", pointsEarned));
	
	      // Increment items purchased count
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Items_Purchased", order.getItemCount()));
	  }
	
	  // News app: Track reading activity
	  private void onArticleRead() {
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Articles_Read_Total", 1));
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Articles_Read_Today", 1));
	  }
	
	  // Gaming: Update player stats
	  private void onLevelCompleted(int scoreEarned, int coinsCollected) {
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Score", scoreEarned));
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Coins", coinsCollected));
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Levels_Completed", 1));
	  }
	
	  // Fitness app: Track workout metrics
	  private void onWorkoutCompleted(int caloriesBurned) {
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Workouts", 1));
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Workouts_This_Week", 1));
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Calories_Burned", caloriesBurned));
	  }
	
	  // App engagement: Track feature usage
	  private void onFeatureUsed(String featureName) {
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("App_Opens", 1));
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Feature_" + featureName + "_Used", 1));
	  }
	
	  // Decrement example: Using points or lives
	  private void onPointsRedeemed(int pointsUsed) {
	      // Use negative value to decrement
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Loyalty_Points", -pointsUsed));
	  }
	
	  // Session tracking
	  private void onAppLaunched() {
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Session_Count", 1));
	      Pushwoosh.getInstance().setTags(Tags.incrementInt("Sessions_This_Month", 1));
	  }
	
```

#### Return

TagsBundle containing the increment operation

#### Parameters

main

| | |
|---|---|
| key | tag name to increment (e.g., &quot;Total_Purchases&quot;, &quot;Loyalty_Points&quot;, &quot;Session_Count&quot;) |
| delta | value to add (positive to increment, negative to decrement) |

#### See also

| |
|---|
| [TagsBundle.Builder](../-tags-bundle/-builder/increment-int.md) |
