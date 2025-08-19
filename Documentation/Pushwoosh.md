
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
		<td><a href="#1a4f87e96ce1ec822da344e4134b0716b2">public static final int PUSH_HISTORY_CAPACITY</a></td>
	</tr>
	<tr>
		<td><a href="#1a867222b601ed840a4840665c1228ce8c">public static Pushwoosh getInstance()</a></td>
	</tr>
	<tr>
		<td><a href="#1ab29ddbf5c22a4109a81817acec156fff">public String getApplicationCode()</a></td>
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
		<td><a href="#1abd6aa3a0c6a874cb877600e70a7c2098">public String getPushToken()</a></td>
	</tr>
	<tr>
		<td><a href="#1a97df509ae9b3c72e228a59e725b27f0d">public void setLanguage(@Nullable String language)</a></td>
	</tr>
	<tr>
		<td><a href="#1a62dc601a6839e11a4ce4660d0cca89e4">public String getLanguage()</a></td>
	</tr>
	<tr>
		<td><a href="#1a3c59030402df507a9d46593511d84116">public void requestNotificationPermission()</a></td>
	</tr>
	<tr>
		<td><a href="#1a4c9265e7a9f23819245ec2c6f26336bd">public void registerForPushNotifications()</a></td>
	</tr>
	<tr>
		<td><a href="#1a0378811f23de9a873609e3294e152f7e">public void registerForPushNotificationsWithTags(TagsBundle tags)</a></td>
	</tr>
	<tr>
		<td><a href="#1a0a0723da091876cb5cca9883ac122aff">public void registerForPushNotifications(Callback&lt;RegisterForPushNotificationsResultData, RegisterForPushNotificationsException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1abf4c8159d3997a7d990930a0b4b57d09">public void registerForPushNotificationsWithTags(Callback&lt;RegisterForPushNotificationsResultData, RegisterForPushNotificationsException&gt; callback, TagsBundle tags)</a></td>
	</tr>
	<tr>
		<td><a href="#1ae5778ea8ff76e92c8f76d0b321ded5b8">public void registerForPushNotificationsWithoutPermission(Callback&lt;RegisterForPushNotificationsResultData, RegisterForPushNotificationsException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a83bb611e9123928bd78ed08e1af1396f">public void registerForPushNotificationsWithTagsWithoutPermission(Callback&lt;RegisterForPushNotificationsResultData, RegisterForPushNotificationsException&gt; callback, TagsBundle tagsBundle)</a></td>
	</tr>
	<tr>
		<td><a href="#1a5a36f3f0301f6483f58c7c83351a2711">public void registerExistingToken(@NonNull String token, Callback&lt;RegisterForPushNotificationsResultData, RegisterForPushNotificationsException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1aea45f4c233897028e053ccb0e559ae8b">public void addAlternativeAppCode(String appCode)</a></td>
	</tr>
	<tr>
		<td><a href="#1aa0a7960136d28e20ccfbe92c2dbcab1e">public void resetAlternativeAppCodes()</a></td>
	</tr>
	<tr>
		<td><a href="#1ab87303f51d7244e54e24cc62419a38c6">public void registerWhatsappNumber(String number)</a></td>
	</tr>
	<tr>
		<td><a href="#1adf8a82319c80e4af9c62af406179470f">public void registerSMSNumber(@NonNull String number)</a></td>
	</tr>
	<tr>
		<td><a href="#1a4796ea2a1eddb6b308479e13261732db">public void setShowPushnotificationAlert(boolean showAlert)</a></td>
	</tr>
	<tr>
		<td><a href="#1a67be4c3344c977df624976efa36e4fd0">public void unregisterForPushNotifications()</a></td>
	</tr>
	<tr>
		<td><a href="#1a6b88d79ec0c1fc16e733edd8ddd93e9d">public void unregisterForPushNotifications(Callback&lt;String, UnregisterForPushNotificationException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1ad4e8648b2affb329eea670b4230021c9">public void setTags(@NonNull TagsBundle tags)</a></td>
	</tr>
	<tr>
		<td><a href="#1abbfeb592420ad0c1e30e8efa65d5def5">public void setEmailTags(@NonNull TagsBundle emailTags, @NonNull String email)</a></td>
	</tr>
	<tr>
		<td><a href="#1a7bbdd590f2317cddf5fc5af10b149cbc">public void setTags(@NonNull TagsBundle tags, Callback&lt;Void, PushwooshException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1aff91835283066d70941036a06480cdf8">public void setEmailTags(@NonNull TagsBundle emailTags, String email, Callback&lt;Void, PushwooshException&gt; callback)</a></td>
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
		<td><a href="#1aa891a323a83f70e3ad7037e70eb44291">public void sendAppOpen()</a></td>
	</tr>
	<tr>
		<td><a href="#1a670eed66131fb27da00fc4add0b1b32b">public void setUserId(@NonNull String userId)</a></td>
	</tr>
	<tr>
		<td><a href="#1a489a9a44909debd37bdeae14bcd17e77">public void setUserId(@NonNull String userId, Callback&lt;Boolean, SetUserIdException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a2787f977e44c92eb06e905298323efeb">public void setUser(@NonNull String userId, @NonNull List&lt;String&gt; emails)</a></td>
	</tr>
	<tr>
		<td><a href="#1ae7e252a7ee9da7555db03dd5bc07c8b3">public void setUser(@NonNull String userId, @NonNull List&lt;String&gt; emails, Callback&lt;Boolean, SetUserException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1abad0b23935d21e27c67429ac83a88a77">public void setEmail(@NonNull List&lt;String&gt; emails)</a></td>
	</tr>
	<tr>
		<td><a href="#1add3e32d9e5f7782bfc7077902ab5b0cd">public void setEmail(@NonNull List&lt;String&gt; emails, Callback&lt;Boolean, SetEmailException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a2f71f41b56251b41c781ce554c141de7">public void setEmail(@NonNull String email)</a></td>
	</tr>
	<tr>
		<td><a href="#1a5ed108f297858a423c33a00143c58e64">public void setEmail(@NonNull String email, Callback&lt;Boolean, SetEmailException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1ac8cdcf327ed561fbedb8e3b1aac914e7">public void mergeUserId(@NonNull String oldUserId, @NonNull String newUserId, boolean doMerge, @Nullable Callback&lt;Void, MergeUserException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a395f1493d70983045a8bc1c7cfc12005">public String getUserId()</a></td>
	</tr>
	<tr>
		<td><a href="#1a74f93e8d3d1f1bc555aa3c0ad4012f89">public void enableHuaweiPushNotifications()</a></td>
	</tr>
	<tr>
		<td><a href="#1a37fedb306dd638f5f46575f4bec57698">public void startServerCommunication()</a></td>
	</tr>
	<tr>
		<td><a href="#1a8895e1576c5614744f45f217e7166ef0">public void stopServerCommunication()</a></td>
	</tr>
	<tr>
		<td><a href="#1affb3246b61e4a3ce804c67506672d881">public boolean isServerCommunicationAllowed()</a></td>
	</tr>
	<tr>
		<td><a href="#1a54109e499c49ba95133ff67546586374">public void setAllowedExternalHosts(ArrayList&lt;String&gt; allowedExternalHosts)</a></td>
	</tr>
	<tr>
		<td><a href="#1a76ec2d5fd3bb77a0675ee06b870f796d">public void setApiToken(String token)</a></td>
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
  

#### <a name="1a4f87e96ce1ec822da344e4134b0716b2"></a>public static final int PUSH_HISTORY_CAPACITY  
Maximum number of notifications returned by <a href="Pushwoosh.md#1ab8760015873348250d3f888a5c49ffa6">Pushwoosh#getPushHistory()</a>

----------  
  

#### <a name="1a867222b601ed840a4840665c1228ce8c"></a>public static <a href="#heading">Pushwoosh</a> getInstance()  
<strong>Returns</strong> Pushwoosh shared instance 

----------  
  

#### <a name="1ab29ddbf5c22a4109a81817acec156fff"></a>public String getApplicationCode()  
<strong>Returns</strong> Current Pushwoosh application code 

----------  
  

#### <a name="1a3d17309c3dbbee8c93897fb37ce830e6"></a>public String getAppId()  
<strong>Returns</strong> Current Pushwoosh application code <em>See also:</em> getApplicationCode() 

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
  

#### <a name="1abd6aa3a0c6a874cb877600e70a7c2098"></a>public String getPushToken()  
<strong>Returns</strong> Push notification token or null if device is not registered yet. 

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
  

#### <a name="1a3c59030402df507a9d46593511d84116"></a>public void requestNotificationPermission()  


----------  
  

#### <a name="1a4c9265e7a9f23819245ec2c6f26336bd"></a>public void registerForPushNotifications()  
<em>See also:</em> <a href="#registerForPushNotifications(Callback)">registerForPushNotifications(Callback)</a>

----------  
  

#### <a name="1a0378811f23de9a873609e3294e152f7e"></a>public void registerForPushNotificationsWithTags(<a href="tags/TagsBundle.md">TagsBundle</a> tags)  
<em>See also:</em> <a href="#registerForPushNotificationsWithTags(Callback, TagsBundle)">registerForPushNotifications(Callback, TagsBundle)</a>

----------  
  

#### <a name="1a0a0723da091876cb5cca9883ac122aff"></a>public void registerForPushNotifications(<a href="function/Callback.md">Callback</a>&lt;RegisterForPushNotificationsResultData, RegisterForPushNotificationsException&gt; callback)  
Registers device for push notifications<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>push registration callback </td>
	</tr>
</table>


----------  
  

#### <a name="1abf4c8159d3997a7d990930a0b4b57d09"></a>public void registerForPushNotificationsWithTags(<a href="function/Callback.md">Callback</a>&lt;RegisterForPushNotificationsResultData, RegisterForPushNotificationsException&gt; callback, <a href="tags/TagsBundle.md">TagsBundle</a> tags)  
Registers device for push notifications<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>push registration callback </td>
	</tr>
	<tr>
		<td><strong>tags</strong></td>
		<td>tags to be set when registering for pushes </td>
	</tr>
</table>


----------  
  

#### <a name="1ae5778ea8ff76e92c8f76d0b321ded5b8"></a>public void registerForPushNotificationsWithoutPermission(<a href="function/Callback.md">Callback</a>&lt;RegisterForPushNotificationsResultData, RegisterForPushNotificationsException&gt; callback)  


----------  
  

#### <a name="1a83bb611e9123928bd78ed08e1af1396f"></a>public void registerForPushNotificationsWithTagsWithoutPermission(<a href="function/Callback.md">Callback</a>&lt;RegisterForPushNotificationsResultData, RegisterForPushNotificationsException&gt; callback, <a href="tags/TagsBundle.md">TagsBundle</a> tagsBundle)  


----------  
  

#### <a name="1a5a36f3f0301f6483f58c7c83351a2711"></a>public void registerExistingToken(@NonNull String token, <a href="function/Callback.md">Callback</a>&lt;RegisterForPushNotificationsResultData, RegisterForPushNotificationsException&gt; callback)  


----------  
  

#### <a name="1aea45f4c233897028e053ccb0e559ae8b"></a>public void addAlternativeAppCode(String appCode)  


----------  
  

#### <a name="1aa0a7960136d28e20ccfbe92c2dbcab1e"></a>public void resetAlternativeAppCodes()  


----------  
  

#### <a name="1ab87303f51d7244e54e24cc62419a38c6"></a>public void registerWhatsappNumber(String number)  


----------  
  

#### <a name="1adf8a82319c80e4af9c62af406179470f"></a>public void registerSMSNumber(@NonNull String number)  


----------  
  

#### <a name="1a4796ea2a1eddb6b308479e13261732db"></a>public void setShowPushnotificationAlert(boolean showAlert)  


----------  
  

#### <a name="1a67be4c3344c977df624976efa36e4fd0"></a>public void unregisterForPushNotifications()  
<em>See also:</em> <a href="#unregisterForPushNotifications(Callback)">unregisterForPushNotifications(Callback)</a>

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
  

#### <a name="1ad4e8648b2affb329eea670b4230021c9"></a>public void setTags(@NonNull <a href="tags/TagsBundle.md">TagsBundle</a> tags)  
Associates device with given tags. If setTags request fails tags will be resent on the next application launch. <br/><br/>
 Example: 
```Java
pushwoosh.setTags(Tags.intTag("intTag", 42));
```
<br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>tags</strong></td>
		<td><a href="tags/TagsBundle.md">application tags bundle</a></td>
	</tr>
</table>


----------  
  

#### <a name="1abbfeb592420ad0c1e30e8efa65d5def5"></a>public void setEmailTags(@NonNull <a href="tags/TagsBundle.md">TagsBundle</a> emailTags, @NonNull String email)  
Associates device with given email tags. If setEmailTags request fails email tags will be resent on the next application launch. <br/><br/>
 Example: 
```Java
pushwoosh.setEmailTags(Tags.intTag("intTag", 42), "my@email.com");
```
<br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>emailTags</strong></td>
		<td><a href="tags/TagsBundle.md">application tags bundle</a></td>
	</tr>
	<tr>
		<td><strong>email</strong></td>
		<td>user email </td>
	</tr>
</table>


----------  
  

#### <a name="1a7bbdd590f2317cddf5fc5af10b149cbc"></a>public void setTags(@NonNull <a href="tags/TagsBundle.md">TagsBundle</a> tags, <a href="function/Callback.md">Callback</a>&lt;Void, PushwooshException&gt; callback)  
Associates device with given tags. If setTags request fails tags will be resent on the next application launch. <br/><br/>
 Example: 
```Java
pushwoosh.setTags(Tags.intTag("intTag", 42), (result) -> {
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
  

#### <a name="1aff91835283066d70941036a06480cdf8"></a>public void setEmailTags(@NonNull <a href="tags/TagsBundle.md">TagsBundle</a> emailTags, String email, <a href="function/Callback.md">Callback</a>&lt;Void, PushwooshException&gt; callback)  
Associates device with given email tags. If setEmailTags request fails email tags will be resent on the next application launch. <br/><br/>
 Example: 
```Java
List<String> emails = new ArrayList<>();
emails.add("my@email.com");
pushwoosh.setEmailTags(Tags.intTag("intTag", 42), emails, (result) -> {
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
		<td><strong>emailTags</strong></td>
		<td><a href="tags/TagsBundle.md">application tags bundle</a></td>
	</tr>
	<tr>
		<td><strong>email</strong></td>
		<td>user email </td>
	</tr>
	<tr>
		<td><strong>callback</strong></td>
		<td>sendEmailTags operation callback </td>
	</tr>
</table>


----------  
  

#### <a name="1af3d33d80b8723dd57644ae358ceb04c8"></a>public void sendTags(@NonNull <a href="tags/TagsBundle.md">TagsBundle</a> tags)  
<em>See also:</em> setTags(TagsBundle) 

----------  
  

#### <a name="1a7cde620227bb15a4ec260eec0b6d3feb"></a>public void sendTags(@NonNull <a href="tags/TagsBundle.md">TagsBundle</a> tags, <a href="function/Callback.md">Callback</a>&lt;Void, PushwooshException&gt; callback)  
<strong>Parameters</strong><br/>
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
<em>See also:</em> setTags(TagsBundle, Callback) 

----------  
  

#### <a name="1aeac385210813e08a3c2a90e512755957"></a>public void getTags(@NonNull <a href="function/Callback.md">Callback</a>&lt;<a href="tags/TagsBundle.md">TagsBundle</a>, GetTagsException&gt; callback)  
Gets tags associated with current device <br/><br/>
 Example: 
```Java
pushwoosh.getTags((result) -> {
    if (result.isSuccess()) {
         // tags successfully received
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
  

#### <a name="1aa891a323a83f70e3ad7037e70eb44291"></a>public void sendAppOpen()  
Informs the Pushwoosh about the app being launched. Usually called internally by SDK. 

----------  
  

#### <a name="1a670eed66131fb27da00fc4add0b1b32b"></a>public void setUserId(@NonNull String userId)  
Set User identifier. This could be Facebook ID, username or email, or any other user ID. This allows data and events to be matched across multiple user devices.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>userId</strong></td>
		<td>user identifier </td>
	</tr>
</table>


----------  
  

#### <a name="1a489a9a44909debd37bdeae14bcd17e77"></a>public void setUserId(@NonNull String userId, <a href="function/Callback.md">Callback</a>&lt;Boolean, SetUserIdException&gt; callback)  
Set User identifier. This could be Facebook ID, username or email, or any other user ID. This allows data and events to be matched across multiple user devices.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>userId</strong></td>
		<td>user identifier </td>
	</tr>
	<tr>
		<td><strong>callback</strong></td>
		<td>setUserId operation callback </td>
	</tr>
</table>


----------  
  

#### <a name="1a2787f977e44c92eb06e905298323efeb"></a>public void setUser(@NonNull String userId, @NonNull List&lt;String&gt; emails)  
Set User identifier and register emails associated to the user. UserId could be Facebook ID or any other user ID. This allows data and events to be matched across multiple user devices.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>userId</strong></td>
		<td>user identifier </td>
	</tr>
	<tr>
		<td><strong>emails</strong></td>
		<td>user's emails array list </td>
	</tr>
</table>


----------  
  

#### <a name="1ae7e252a7ee9da7555db03dd5bc07c8b3"></a>public void setUser(@NonNull String userId, @NonNull List&lt;String&gt; emails, <a href="function/Callback.md">Callback</a>&lt;Boolean, SetUserException&gt; callback)  
Set User identifier and register emails associated to the user. UserId could be Facebook ID or any other user ID. This allows data and events to be matched across multiple user devices.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>userId</strong></td>
		<td>user identifier </td>
	</tr>
	<tr>
		<td><strong>emails</strong></td>
		<td>user's emails array list </td>
	</tr>
	<tr>
		<td><strong>callback</strong></td>
		<td>setUser operation callback </td>
	</tr>
</table>


----------  
  

#### <a name="1abad0b23935d21e27c67429ac83a88a77"></a>public void setEmail(@NonNull List&lt;String&gt; emails)  
Register emails list associated to the current user.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>emails</strong></td>
		<td>user's emails array list </td>
	</tr>
</table>


----------  
  

#### <a name="1add3e32d9e5f7782bfc7077902ab5b0cd"></a>public void setEmail(@NonNull List&lt;String&gt; emails, <a href="function/Callback.md">Callback</a>&lt;Boolean, SetEmailException&gt; callback)  
Register emails list associated to the current user.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>emails</strong></td>
		<td>user's emails array list </td>
	</tr>
	<tr>
		<td><strong>callback</strong></td>
		<td>setEmail operation callback </td>
	</tr>
</table>


----------  
  

#### <a name="1a2f71f41b56251b41c781ce554c141de7"></a>public void setEmail(@NonNull String email)  
Register email associated to the current user. Email should be a string and could not be null or empty.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>email</strong></td>
		<td>user's email string </td>
	</tr>
</table>


----------  
  

#### <a name="1a5ed108f297858a423c33a00143c58e64"></a>public void setEmail(@NonNull String email, <a href="function/Callback.md">Callback</a>&lt;Boolean, SetEmailException&gt; callback)  
Register email associated to the current user. Email should be a string and could not be null or empty.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>email</strong></td>
		<td>user's email string </td>
	</tr>
	<tr>
		<td><strong>callback</strong></td>
		<td>setEmail operation callback </td>
	</tr>
</table>


----------  
  

#### <a name="1ac8cdcf327ed561fbedb8e3b1aac914e7"></a>public void mergeUserId(@NonNull String oldUserId, @NonNull String newUserId, boolean doMerge, @Nullable <a href="function/Callback.md">Callback</a>&lt;Void, MergeUserException&gt; callback)  
Move all event statistics from oldUserId to newUserId if doMerge is true. If doMerge is false all events for oldUserId are removed.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>oldUserId</strong></td>
		<td>source user identifier </td>
	</tr>
	<tr>
		<td><strong>newUserId</strong></td>
		<td>destination user identifier </td>
	</tr>
	<tr>
		<td><strong>doMerge</strong></td>
		<td>merge/remove events for source user identifier </td>
	</tr>
	<tr>
		<td><strong>callback</strong></td>
		<td>method completion callback </td>
	</tr>
</table>


----------  
  

#### <a name="1a395f1493d70983045a8bc1c7cfc12005"></a>public String getUserId()  
<strong>Returns</strong> current user id <em>See also:</em> setUserId(String) 

----------  
  

#### <a name="1a74f93e8d3d1f1bc555aa3c0ad4012f89"></a>public void enableHuaweiPushNotifications()  
Enables Huawei push messaging in plugin-based applications. This method gives no effect if it is called in a native application. 

----------  
  

#### <a name="1a37fedb306dd638f5f46575f4bec57698"></a>public void startServerCommunication()  
Starts communication with Pushwoosh server. 

----------  
  

#### <a name="1a8895e1576c5614744f45f217e7166ef0"></a>public void stopServerCommunication()  
Stops communication with Pushwoosh server. 

----------  
  

#### <a name="1affb3246b61e4a3ce804c67506672d881"></a>public boolean isServerCommunicationAllowed()  
Check if communication with Pushwoosh server is allowed.<br/><br/><br/><strong>Returns</strong> true if communication with Pushwoosh server is allowed 

----------  
  

#### <a name="1a54109e499c49ba95133ff67546586374"></a>public void setAllowedExternalHosts(ArrayList&lt;String&gt; allowedExternalHosts)  


----------  
  

#### <a name="1a76ec2d5fd3bb77a0675ee06b870f796d"></a>public void setApiToken(String token)  
