
#### module: pushwoosh  

#### package: com.pushwoosh  

# <a name="heading"></a>class Pushwoosh  
Pushwoosh class is used to manage push registration, application tags and local notifications.<br/>
 By default Pushwoosh SDK automatically adds following permissions: <br/>
 ${applicationId}.permission.C2D\_MESSAGE <br/>
 ${applicationId}.permission.RECEIVE\_ADM\_MESSAGE <br/><a href="https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_NETWORK_STATE">android.permission.ACCESS_NETWORK_STATE</a><br/><a href="https://developer.android.com/reference/android/Manifest.permission.html#INTERNET">android.permission.INTERNET</a><br/><a href="https://developer.android.com/reference/android/Manifest.permission.html#WAKE_LOCK">android.permission.WAKE_LOCK</a><br/><br/><br/><a href="https://developer.android.com/reference/android/Manifest.permission.html#RECEIVE_BOOT_COMPLETED">android.permission.RECEIVE_BOOT_COMPLETED</a> should be added manually to take advantage of local notification rescheduling after device restart. 
## Members  

<table>
	<tr>
		<td><a href="#1a6acf0ed8b28ccdb90785d74c13df8b72">public static final String PUSH_RECEIVE_EVENT</a></td>
	</tr>
	<tr>
		<td><a href="#1a867222b601ed840a4840665c1228ce8c">public static Pushwoosh getInstance()</a></td>
	</tr>
	<tr>
		<td><a href="#1aa891a323a83f70e3ad7037e70eb44291">public void sendAppOpen()</a></td>
	</tr>
	<tr>
		<td><a href="#1a3d17309c3dbbee8c93897fb37ce830e6">public String getAppId()</a></td>
	</tr>
	<tr>
		<td><a href="#1a705c78bf6793d9d8884abf3bdc4e7e0b">public void setAppId(@NonNull String appId)</a></td>
	</tr>
	<tr>
		<td><a href="#1a88d76e774e4511f9964834df7dcd7e67">public void setSenderId(@NonNull String senderId)</a></td>
	</tr>
	<tr>
		<td><a href="#1a6f4894044bf081f10e21c4360c957f46">public String getSenderId()</a></td>
	</tr>
	<tr>
		<td><a href="#1af3e488cafc849c1db1c54b16074b775c">public String getHwid()</a></td>
	</tr>
	<tr>
		<td><a href="#1a4f87e96ce1ec822da344e4134b0716b2">public static final int PUSH_HISTORY_CAPACITY</a></td>
	</tr>
	<tr>
		<td><a href="#1a97df509ae9b3c72e228a59e725b27f0d">public void setLanguage(@Nullable String language)</a></td>
	</tr>
	<tr>
		<td><a href="#1a62dc601a6839e11a4ce4660d0cca89e4">public String getLanguage()</a></td>
	</tr>
	<tr>
		<td><a href="#1a4c9265e7a9f23819245ec2c6f26336bd">public void registerForPushNotifications()</a></td>
	</tr>
	<tr>
		<td><a href="#1a625ea7e2a5c5137514ddd2ec9cbe8de3">public void registerForPushNotifications(Callback&lt;String, RegisterForPushNotificationsException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a67be4c3344c977df624976efa36e4fd0">public void unregisterForPushNotifications()</a></td>
	</tr>
	<tr>
		<td><a href="#1a6b88d79ec0c1fc16e733edd8ddd93e9d">public void unregisterForPushNotifications(Callback&lt;String, UnregisterForPushNotificationException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1af3d33d80b8723dd57644ae358ceb04c8">public void sendTags(@NonNull TagsBundle tags)</a></td>
	</tr>
	<tr>
		<td><a href="#1a7cde620227bb15a4ec260eec0b6d3feb">public void sendTags(@NonNull TagsBundle tags, Callback&lt;Void, PushwooshException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1aeac385210813e08a3c2a90e512755957">public void getTags(@NonNull Callback&lt;TagsBundle, GetTagsException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a163fe171dc01c43f8b9578663f038a68">public void sendInappPurchase(@NonNull String sku, @NonNull BigDecimal price, @NonNull String currency)</a></td>
	</tr>
	<tr>
		<td><a href="#1a8da037f1b56638bd637998fc10f70be3">public PushMessage getLaunchNotification()</a></td>
	</tr>
	<tr>
		<td><a href="#1a9e7ce0eab18b6cbb46bbc802adee61ee">public void clearLaunchNotification()</a></td>
	</tr>
	<tr>
		<td><a href="#1a3e7942de9d01dc94dd8364db20f9c65a">public LocalNotificationRequest scheduleLocalNotification(LocalNotification notification)</a></td>
	</tr>
	<tr>
		<td><a href="#1ab8760015873348250d3f888a5c49ffa6">public List&lt;PushMessage&gt; getPushHistory()</a></td>
	</tr>
	<tr>
		<td><a href="#1ac3b6fab658c0b07aff0e197e5933b0ab">public void clearPushHistory()</a></td>
	</tr>
	<tr>
		<td><a href="#1abd6aa3a0c6a874cb877600e70a7c2098">public String getPushToken()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a6acf0ed8b28ccdb90785d74c13df8b72"></a>public static final String PUSH_RECEIVE_EVENT  
Intent extra key for push notification payload. Is added to intent that starts Activity when push notification is clicked. <br/><br/>
 Example: 
```Java
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getIntent().hasExtra(Pushwoosh.PUSH_RECEIVE_EVENT)) {
        // Activity was started in response to push notification
        showMessage("Push message is " + getIntent().getExtras().getString(Pushwoosh.PUSH_RECEIVE_EVENT));
    }
}
```


----------  
  

#### <a name="1a867222b601ed840a4840665c1228ce8c"></a>public static <a href="#heading">Pushwoosh</a> getInstance()  
<strong>Returns</strong> Pushwoosh shared instance 

----------  
  

#### <a name="1aa891a323a83f70e3ad7037e70eb44291"></a>public void sendAppOpen()  
Informs the Pushwoosh about the app being launched. Usually called internally by SDK. 

----------  
  

#### <a name="1a3d17309c3dbbee8c93897fb37ce830e6"></a>public String getAppId()  
<strong>Returns</strong> Current Pushwoosh application id 

----------  
  

#### <a name="1a705c78bf6793d9d8884abf3bdc4e7e0b"></a>public void setAppId(@NonNull String appId)  
Associates current applicaton with given pushwoosh application code (Alternative for "com.pushwoosh.appid" metadata in AndroidManifest.xml)<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>appId</strong></td>
		<td>Pushwoosh application code </td>
	</tr>
</table>


----------  
  

#### <a name="1a88d76e774e4511f9964834df7dcd7e67"></a>public void setSenderId(@NonNull String senderId)  
Sets FCM/GCM sender Id (Alternative for "com.pushwoosh.senderid" metadata in AndroidManifest.xml)<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>senderId</strong></td>
		<td>GCM/FCM sender id </td>
	</tr>
</table>


----------  
  

#### <a name="1a6f4894044bf081f10e21c4360c957f46"></a>public String getSenderId()  
<strong>Returns</strong> Current GCM/FCM sender id 

----------  
  

#### <a name="1af3e488cafc849c1db1c54b16074b775c"></a>public String getHwid()  
<strong>Returns</strong> Pushwoosh HWID associated with current device 

----------  
  

#### <a name="1a4f87e96ce1ec822da344e4134b0716b2"></a>public static final int PUSH_HISTORY_CAPACITY  
Maximum number of notifications returned by <a href="Pushwoosh.md#1ab8760015873348250d3f888a5c49ffa6">Pushwoosh#getPushHistory()</a>

----------  
  

#### <a name="1a97df509ae9b3c72e228a59e725b27f0d"></a>public void setLanguage(@Nullable String language)  
Set custom application language. Device language used by default. Set to null if you want to use device language again.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>language</strong></td>
		<td>lowercase two-letter code according to ISO-639-1 standard ("en", "de", "fr", etc.) or null (device language). </td>
	</tr>
</table>


----------  
  

#### <a name="1a62dc601a6839e11a4ce4660d0cca89e4"></a>public String getLanguage()  


----------  
  

#### <a name="1a4c9265e7a9f23819245ec2c6f26336bd"></a>public void registerForPushNotifications()  
<em>See also:</em> <a href="#registerForPushNotifications(Callback)">registerForPushNotifications(Callback)</a>

----------  
  

#### <a name="1a625ea7e2a5c5137514ddd2ec9cbe8de3"></a>public void registerForPushNotifications(<a href="function/Callback.md">Callback</a>&lt;String, RegisterForPushNotificationsException&gt; callback)  
Registers device for push notifications<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>push registration callback </td>
	</tr>
</table>


----------  
  

#### <a name="1a67be4c3344c977df624976efa36e4fd0"></a>public void unregisterForPushNotifications()  
<em>See also:</em> <a href="Pushwoosh.md#1a6b88d79ec0c1fc16e733edd8ddd93e9d">unregisterForPushNotifications(Callback&lt;String, UnregisterForPushNotificationException&gt;)</a>

----------  
  

#### <a name="1a6b88d79ec0c1fc16e733edd8ddd93e9d"></a>public void unregisterForPushNotifications(<a href="function/Callback.md">Callback</a>&lt;String, UnregisterForPushNotificationException&gt; callback)  
Unregisters device from push notifications<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>push unregister callback </td>
	</tr>
</table>


----------  
  

#### <a name="1af3d33d80b8723dd57644ae358ceb04c8"></a>public void sendTags(@NonNull <a href="tags/TagsBundle.md">TagsBundle</a> tags)  
<em>See also:</em> <a href="Pushwoosh.md#1a7cde620227bb15a4ec260eec0b6d3feb">sendTags(@NonNull TagsBundle, Callback&lt;Void, PushwooshException&gt;)</a>

----------  
  

#### <a name="1a7cde620227bb15a4ec260eec0b6d3feb"></a>public void sendTags(@NonNull <a href="tags/TagsBundle.md">TagsBundle</a> tags, <a href="function/Callback.md">Callback</a>&lt;Void, PushwooshException&gt; callback)  
Associates device with given tags. If setTags request fails tags will be resent on the next application launch. <br/><br/>
 Example: 
```Java
pushwoosh.sendTags(Tags.intTag("intTag", 42), (result) -> {
    if (result.isSuccess()) {
        // tags sucessfully sent
    }
    else {
        // failed to send tags
    }
});
```
<br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>tags</strong></td>
		<td><a href="tags/TagsBundle.md">application tags bundle</a></td>
	</tr>
	<tr>
		<td><strong>callback</strong></td>
		<td>sendTags operation callback </td>
	</tr>
</table>


----------  
  

#### <a name="1aeac385210813e08a3c2a90e512755957"></a>public void getTags(@NonNull <a href="function/Callback.md">Callback</a>&lt;<a href="tags/TagsBundle.md">TagsBundle</a>, GetTagsException&gt; callback)  
Gets tags associated with current device <br/><br/>
 Example: 
```Java
pushwoosh.getTags((result) -> {
    if (result.isSuccess()) {
         // tags sucessfully received
         int intTag = result.getInt("intTag");
    }
    else {
        // failed to receive tags
    }
});
```
<br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>callback handler </td>
	</tr>
</table>


----------  
  

#### <a name="1a163fe171dc01c43f8b9578663f038a68"></a>public void sendInappPurchase(@NonNull String sku, @NonNull BigDecimal price, @NonNull String currency)  
Sends In-App purchase statistics. Purchase information is stored in "In-app Product", "In-app Purchase" and "Last In-app Purchase date" default tags.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>sku</strong></td>
		<td>purchased product ID </td>
	</tr>
	<tr>
		<td><strong>price</strong></td>
		<td>price of the product </td>
	</tr>
	<tr>
		<td><strong>currency</strong></td>
		<td>currency of the price (ex: “USD”) </td>
	</tr>
</table>


----------  
  

#### <a name="1a8da037f1b56638bd637998fc10f70be3"></a>public <a href="notification/PushMessage.md">PushMessage</a> getLaunchNotification()  
<strong>Returns</strong> Launch notification data or null. 

----------  
  

#### <a name="1a9e7ce0eab18b6cbb46bbc802adee61ee"></a>public void clearLaunchNotification()  
reset <a href="Pushwoosh.md#1a8da037f1b56638bd637998fc10f70be3">getLaunchNotification()</a> to return null. 

----------  
  

#### <a name="1a3e7942de9d01dc94dd8364db20f9c65a"></a>public <a href="notification/LocalNotificationRequest.md">LocalNotificationRequest</a> scheduleLocalNotification(<a href="notification/LocalNotification.md">LocalNotification</a> notification)  
Schedules local notification. <br/><br/>
 Example: 
```Java
LocalNotification notification = new LocalNotification.Builder().setMessage("Local notification content")
            .setDelay(seconds)
            .build();
LocalNotificationRequest request = Pushwoosh.getInstance().scheduleLocalNotification(notification);
```
<br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>notification</strong></td>
		<td><a href="notification/LocalNotification.md">notification</a> to send </td>
	</tr>
</table>
<strong>Returns</strong> <a href="notification/LocalNotificationRequest.md">local notification request</a>

----------  
  

#### <a name="1ab8760015873348250d3f888a5c49ffa6"></a>public List&lt;<a href="notification/PushMessage.md">PushMessage</a>&gt; getPushHistory()  
Gets push notification history. History contains both remote and local notifications.<br/><br/><br/><strong>Returns</strong> Push history as List of <a href="notification/PushMessage.md">com.pushwoosh.notification.PushMessage</a>. Maximum of <a href="Pushwoosh.md#1a4f87e96ce1ec822da344e4134b0716b2">PUSH_HISTORY_CAPACITY</a> pushes are returned 

----------  
  

#### <a name="1ac3b6fab658c0b07aff0e197e5933b0ab"></a>public void clearPushHistory()  
Clears push history. Usually called after <a href="Pushwoosh.md#1ab8760015873348250d3f888a5c49ffa6">getPushHistory()</a>. 

----------  
  

#### <a name="1abd6aa3a0c6a874cb877600e70a7c2098"></a>public String getPushToken()  
<strong>Returns</strong> Push notification token or null if device is not registered yet. 