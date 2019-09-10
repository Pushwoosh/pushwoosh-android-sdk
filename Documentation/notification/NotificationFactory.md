
#### module: pushwoosh  

#### package: com.pushwoosh.notification  

# <a name="heading"></a>class NotificationFactory  
Abstract class that is used to customize push notification appearance. All NotificationFactory ancestors must be public and must contain public constructor without parameters. Application will crash on startup if this requirement is not met. Custom NotificationFactory should be registered in AndroidManifest.xml metadata as follows: <br/>
```Java
<meta-data
    android:name="com.pushwoosh.notification_factory"
    android:value="com.your.package.YourNotificationFactory" />
```

## Members  

<table>
	<tr>
		<td><a href="#1af95d50a849b91dac604a8f1ad68863e4">protected String addChannel(PushMessage pushMessage)</a></td>
	</tr>
	<tr>
		<td><a href="#1ad87082d7fd501636dc94937bafaa57f2">public  NotificationFactory()</a></td>
	</tr>
	<tr>
		<td><a href="#1a823af14a22bb0fd8dcd8279be81324f8">public abstract Notification onGenerateNotification(@NonNull PushMessage data)</a></td>
	</tr>
	<tr>
		<td><a href="#1a1767c48d63f7a32cc08930096773d2ef">public Intent getNotificationIntent(@NonNull PushMessage data)</a></td>
	</tr>
	<tr>
		<td><a href="#1a079dfab588f87037c6cac8198ffe4864">public String channelName(String channelName)</a></td>
	</tr>
	<tr>
		<td><a href="#1a9c2956db01335c10904ac2f0787ad2e0">public String channelDescription(String channelName)</a></td>
	</tr>
	<tr>
		<td><a href="#1a743f639a41340a9891554b03d79b3c68">protected final void addLED(@NonNull Notification notification, @Nullable Integer color, int ledOnMs, int ledOffMs)</a></td>
	</tr>
	<tr>
		<td><a href="#1a53d90416a543e1b93ac7a6bbdd2b051b">protected final void addVibration(@NonNull Notification notification, boolean vibration)</a></td>
	</tr>
	<tr>
		<td><a href="#1a8e961c3def9b2c8be940a1ffea69a90a">protected final void addSound(@NonNull Notification notification, @Nullable String sound)</a></td>
	</tr>
	<tr>
		<td><a href="#1a946295e7c204e1ea3f2771cd4e31f823">protected final Context getApplicationContext()</a></td>
	</tr>
	<tr>
		<td><a href="#1a349aba228f8127895d03cadcce7627ed">protected final CharSequence getContentFromHtml(String content)</a></td>
	</tr>
	<tr>
		<td><a href="#1aefcb8fe326ff56b81a5eca037426e07c">protected final void addCancel(@NonNull Notification notification)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1af95d50a849b91dac604a8f1ad68863e4"></a>protected String addChannel(<a href="PushMessage.md">PushMessage</a> pushMessage)  
Create, if not exist, new notification channel from pushMessage.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>pushMessage</strong></td>
		<td>- if push message doesn't contain "pw\_channel" attribute, default channel will be created </td>
	</tr>
</table>
<strong>Returns</strong> channel id which connected with channel name. For Api less than 26 it doesn't create anything 

----------  
  

#### <a name="1ad87082d7fd501636dc94937bafaa57f2"></a>public  NotificationFactory()  


----------  
  

#### <a name="1a823af14a22bb0fd8dcd8279be81324f8"></a>public abstract Notification onGenerateNotification(@NonNull <a href="PushMessage.md">PushMessage</a> data)  
Generates notification using PushMessage data.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>data</strong></td>
		<td>notification data </td>
	</tr>
</table>
<strong>Returns</strong> Notification to show 

----------  
  

#### <a name="1a1767c48d63f7a32cc08930096773d2ef"></a>public Intent getNotificationIntent(@NonNull <a href="PushMessage.md">PushMessage</a> data)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>data</strong></td>
		<td>notification data </td>
	</tr>
</table>
<strong>Returns</strong> Intent to start when user clicks on notification 

----------  
  

#### <a name="1a079dfab588f87037c6cac8198ffe4864"></a>public String channelName(String channelName)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>channelName</strong></td>
		<td>name of the channel specified in Android payload as "pw\_channel" attribute. If no attribute was specified, parameter gives default channel name </td>
	</tr>
</table>
<strong>Returns</strong> name that you want to assign to the channel on its creation. Note that empty name will be ignored and default channel name will be assigned to the channel instead 

----------  
  

#### <a name="1a9c2956db01335c10904ac2f0787ad2e0"></a>public String channelDescription(String channelName)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>channelName</strong></td>
		<td>name of the channel specified in Android payload as "pw\_channel" attribute. If no attribute was specified, parameter gives default channel name </td>
	</tr>
</table>
<strong>Returns</strong> description that you want to assign to the channel on its creation 

----------  
  

#### <a name="1a743f639a41340a9891554b03d79b3c68"></a>protected final void addLED(@NonNull Notification notification, @Nullable Integer color, int ledOnMs, int ledOffMs)  
Adds led blinking to notification<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>notification</strong></td>
		<td>push notification </td>
	</tr>
	<tr>
		<td><strong>color</strong></td>
		<td>led color </td>
	</tr>
	<tr>
		<td><strong>ledOnMs</strong></td>
		<td>led on duration in ms </td>
	</tr>
	<tr>
		<td><strong>ledOffMs</strong></td>
		<td>led off duration in ms </td>
	</tr>
</table>


----------  
  

#### <a name="1a53d90416a543e1b93ac7a6bbdd2b051b"></a>protected final void addVibration(@NonNull Notification notification, boolean vibration)  
Adds vibration to notification.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>notification</strong></td>
		<td>push notification </td>
	</tr>
	<tr>
		<td><strong>vibration</strong></td>
		<td>vibration setting </td>
	</tr>
</table>


----------  
  

#### <a name="1a8e961c3def9b2c8be940a1ffea69a90a"></a>protected final void addSound(@NonNull Notification notification, @Nullable String sound)  
Adds sound to notification.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>notification</strong></td>
		<td>push notification </td>
	</tr>
	<tr>
		<td><strong>sound</strong></td>
		<td>resource from res/raw or assets/www/res directory. If parameter is null or does not exists default system sound will be played. If parameter is empty no sound will be played </td>
	</tr>
</table>


----------  
  

#### <a name="1a946295e7c204e1ea3f2771cd4e31f823"></a>protected final Context getApplicationContext()  
<strong>Returns</strong> Application context. 

----------  
  

#### <a name="1a349aba228f8127895d03cadcce7627ed"></a>protected final CharSequence getContentFromHtml(String content)  
Converts string with html formatting to CharSequence.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>content</strong></td>
		<td>push notification message </td>
	</tr>
</table>
<strong>Returns</strong> html formatted notification content 

----------  
  

#### <a name="1aefcb8fe326ff56b81a5eca037426e07c"></a>protected final void addCancel(@NonNull Notification notification)  
Makes notification cancellable<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>notification</strong></td>
		<td>push notification </td>
	</tr>
</table>
