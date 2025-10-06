//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[dateTag](date-tag.md)

# dateTag

[main]\
open fun [dateTag](date-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Date](https://developer.android.com/reference/kotlin/java/util/Date.html)): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle with a single date tag. 

 Date tags store timestamp values for tracking when events occurred. They are automatically formatted as &quot;yyyy-MM-dd HH:mm&quot; strings. Date tags are essential for time-based segmentation and re-engagement campaigns. 

**Common Use Cases:**

- User activity - last login, last purchase, last app open
- Account milestones - registration date, subscription start, first purchase
- Content interaction - last article read, last video watched
- Re-engagement - last session date, last notification opened
- Time-sensitive events - trial expiration, subscription renewal date

 Example: ```kotlin

	  // E-commerce: Track user activity and milestones
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Login", new Date()));
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Purchase", new Date()));
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Registration_Date", user.getCreatedAt()));
	  Pushwoosh.getInstance().setTags(Tags.dateTag("First_Purchase_Date", order.getCreatedAt()));
	
	  // Subscription management
	  Date subscriptionStart = new Date();
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Subscription_Start_Date", subscriptionStart));
	
	  Calendar calendar = Calendar.getInstance();
	  calendar.add(Calendar.MONTH, 1);
	  Date renewalDate = calendar.getTime();
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Subscription_Renewal_Date", renewalDate));
	
	  // News app: Content engagement tracking
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Article_Read", new Date()));
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Notification_Opened", new Date()));
	
	  // Banking app: Transaction and activity tracking
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Transaction_Date", new Date()));
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Bill_Payment", new Date()));
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Account_Created", accountDate));
	
	  // Fitness app: Workout and progress tracking
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Workout", new Date()));
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Goal_Start_Date", goalStartDate));
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Weight_Log", new Date()));
	
	  // Re-engagement campaigns: Track inactivity
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_App_Open", new Date()));
	  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Feature_Used", new Date()));
	
```

#### Return

TagsBundle containing the single date tag

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;Last_Login&quot;, &quot;Registration_Date&quot;, &quot;Last_Purchase&quot;) |
| value | Date object to be formatted as &quot;yyyy-MM-dd HH:mm&quot; |

#### See also

| |
|---|
| [TagsBundle.Builder](../-tags-bundle/-builder/put-date.md) |
