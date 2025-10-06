//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[removeFromList](remove-from-list.md)

# removeFromList

[main]\
open fun [removeFromList](remove-from-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), list: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle that removes values from an existing list tag. 

 This method removes specified items from an existing list tag without replacing the entire list. The operation is performed server-side, so you don't need to retrieve the current list first. Only matching values will be removed; non-existent values are silently ignored. 

**Common Use Cases:**

- Shopping behavior - remove purchased items from wishlist, remove from cart
- Content management - unfollow topics, remove bookmarks, unsubscribe from channels
- User preferences - remove interests, deselect categories
- Cleanup operations - remove outdated items, clear temporary selections
- Opt-out scenarios - remove from notification categories, preference lists

**Important:** The tag must already exist as a list tag. Only specified values that exist in the list will be removed.  Example: 

```kotlin

	  // E-commerce: Remove items when purchased
	  private void onItemPurchased(List<String> purchasedProductIds) {
	      // Remove purchased items from wishlist
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Wishlist", purchasedProductIds));
	
	      // Remove from cart
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Cart_Items", purchasedProductIds));
	  }
	
	  private void onWishlistItemRemoved(String productId) {
	      List<String> itemToRemove = Arrays.asList(productId);
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Wishlist", itemToRemove));
	  }
	
	  // News app: Unfollow topics and remove bookmarks
	  private void onTopicUnfollowed(String topic) {
	      List<String> topicToRemove = Arrays.asList(topic);
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Followed_Topics", topicToRemove));
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Interests", topicToRemove));
	  }
	
	  private void onBookmarkRemoved(List<String> articleIds) {
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Bookmarked_Articles", articleIds));
	  }
	
	  // Streaming app: Remove from watchlist
	  private void onShowWatched(String showId) {
	      List<String> watchedShow = Arrays.asList(showId);
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Watchlist", watchedShow));
	
	      // Add to completed list
	      Pushwoosh.getInstance().setTags(Tags.appendList("Completed_Shows", watchedShow));
	  }
	
	  // Notifications: Unsubscribe from categories
	  private void onCategoryUnsubscribed(List<String> categories) {
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Notification_Categories", categories));
	  }
	
	  // Fitness app: Remove completed workout from schedule
	  private void onWorkoutCompleted(String workoutId) {
	      List<String> completedWorkout = Arrays.asList(workoutId);
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Scheduled_Workouts", completedWorkout));
	      Pushwoosh.getInstance().setTags(Tags.appendList("Completed_Workouts", completedWorkout));
	  }
	
	  // Travel app: Remove visited destinations
	  private void onDestinationVisited(String destination) {
	      List<String> visited = Arrays.asList(destination);
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Bucket_List", visited));
	      Pushwoosh.getInstance().setTags(Tags.appendList("Visited_Destinations", visited));
	  }
	
	  // User preferences: Opt out of interests
	  private void onInterestRemoved(List<String> unwantedInterests) {
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Interests", unwantedInterests));
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Content_Preferences", unwantedInterests));
	  }
	
	  // Cleanup: Remove outdated items from recent views
	  private void cleanupOldViews(List<String> oldProductIds) {
	      Pushwoosh.getInstance().setTags(Tags.removeFromList("Recently_Viewed_Products", oldProductIds));
	  }
	
```

#### Return

TagsBundle containing the remove from list operation

#### Parameters

main

| | |
|---|---|
| key | list tag name (e.g., &quot;Wishlist&quot;, &quot;Followed_Topics&quot;, &quot;Notification_Categories&quot;) |
| list | values to remove from the existing list |

#### See also

| |
|---|
| [appendList(String, List)](append-list.md) |
| [listTag(String, List)](list-tag.md) |
| [TagsBundle.Builder](../-tags-bundle/-builder/remove-from-list.md) |
