
#### module: pushwoosh  

#### package: com.pushwoosh.notification  

# <a name="heading"></a>class NotificationServiceExtension  
NotificationServiceExtension allows to customize push notification behaviour. All NotificationServiceExtension ancestors must be public and must contain public constructor without parameters. Application will crash on startup if this requirement is not met. Custom NotificationServiceExtension should be registered in AndroidManifest.xml metadata as follows: <br/>
```Java
<meta-data
    android:name="com.pushwoosh.notification_service_extension"
    android:value="com.your.package.YourNotificationServiceExtension" />
```

## Members  

<table>
	<tr>
		<td><a href="#1aabf2366db4c59f0285d64701440f5f20">protected void startActivityForPushMessage(PushMessage message)</a></td>
	</tr>
	<tr>
		<td><a href="#1a5e6593b37d9514bcdfc01fc7358c825f">protected boolean preHandleNotificationsWithUrl()</a></td>
	</tr>
	<tr>
		<td><a href="#1a383ad47d068b3f03068ac94a1693d010">protected boolean isAppOnForeground()</a></td>
	</tr>
	<tr>
		<td><a href="#1a172cc10530180d4b230dd8b12032d7bb">protected final Context getApplicationContext()</a></td>
	</tr>
	<tr>
		<td><a href="#1afe96abfd9e1636c2c90025d0521e092d">public  NotificationServiceExtension()</a></td>
	</tr>
	<tr>
		<td><a href="#1aaf415945f3131a3149fdb46038a7e552">public final void handleMessage(Bundle pushBundle)</a></td>
	</tr>
	<tr>
		<td><a href="#1a9d29c63ac25fd96f34ad3f2ee21f9b1b">public final void handleNotification(Bundle pushBundle)</a></td>
	</tr>
	<tr>
		<td><a href="#1abb13e4825ef4c499790add7f28c5f15d">public final void handleNotificationGroup(List&lt;PushMessage&gt; messages)</a></td>
	</tr>
	<tr>
		<td><a href="#1add147730cdb528e987fd560a59e9468d">public final void handleNotificationCanceled(Bundle pushBundle)</a></td>
	</tr>
	<tr>
		<td><a href="#1a23d001cfc622c6067207f323e2f32419">protected void onMessageOpened(final PushMessage message)</a></td>
	</tr>
	<tr>
		<td><a href="#1a26d02f5d050c8c4d7cbea129eade49ee">protected void onMessageCanceled(final PushMessage message)</a></td>
	</tr>
	<tr>
		<td><a href="#1a810e97415f05ced9abb8f135ece5c4b8">protected void onMessagesGroupOpened(final List&lt;PushMessage&gt; messages)</a></td>
	</tr>
	<tr>
		<td><a href="#1af7643913a39a07b464943576289e6c86">protected boolean onMessageReceived(PushMessage data)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1aabf2366db4c59f0285d64701440f5f20"></a>protected void startActivityForPushMessage(<a href="PushMessage.md">PushMessage</a> message)  
Extension method for push notification open handling. By default starts Launcher Activity or Activity marked withapplicationId}.MESSAGE intent filter.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>message</strong></td>
		<td>notification data </td>
	</tr>
</table>


----------  
  

#### <a name="1a5e6593b37d9514bcdfc01fc7358c825f"></a>protected boolean preHandleNotificationsWithUrl()  
Extension method for push notification open handling.<br/>Pushwoosh is handling notifications containing url or deeplink by default. If there is an activity which can handle url or deeplink provided it will be started and the method <a href="NotificationServiceExtension.md#1aabf2366db4c59f0285d64701440f5f20">startActivityForPushMessage(PushMessage message)</a> will not be called.<br/><br/><br/><strong>Returns</strong> true if you want Pushwoosh to handle notifications containing url or deeplink, false if you want to handle such notifications using <a href="NotificationServiceExtension.md#1aabf2366db4c59f0285d64701440f5f20">startActivityForPushMessage(PushMessage message)</a> method. 

----------  
  

#### <a name="1a383ad47d068b3f03068ac94a1693d010"></a>protected boolean isAppOnForeground()  
<strong>Returns</strong> true if application is currently in focus. 

----------  
  

#### <a name="1a172cc10530180d4b230dd8b12032d7bb"></a>protected final Context getApplicationContext()  
<strong>Returns</strong> Application context. 

----------  
  

#### <a name="1afe96abfd9e1636c2c90025d0521e092d"></a>public  NotificationServiceExtension()  


----------  
  

#### <a name="1aaf415945f3131a3149fdb46038a7e552"></a>public final void handleMessage(Bundle pushBundle)  
Handles push arrival.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>pushBundle</strong></td>
		<td>push notification payload as Bundle </td>
	</tr>
</table>


----------  
  

#### <a name="1a9d29c63ac25fd96f34ad3f2ee21f9b1b"></a>public final void handleNotification(Bundle pushBundle)  
Handles notification open.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>pushBundle</strong></td>
		<td>push notification payload as Bundle </td>
	</tr>
</table>


----------  
  

#### <a name="1abb13e4825ef4c499790add7f28c5f15d"></a>public final void handleNotificationGroup(List&lt;<a href="PushMessage.md">PushMessage</a>&gt; messages)  
Handles notifications group open.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>messages</strong></td>
		<td>list of push messages of the group which was opened </td>
	</tr>
</table>


----------  
  

#### <a name="1add147730cdb528e987fd560a59e9468d"></a>public final void handleNotificationCanceled(Bundle pushBundle)  
Handles notification cancel.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>pushBundle</strong></td>
		<td>push notification payload as Bundle </td>
	</tr>
</table>


----------  
  

#### <a name="1a23d001cfc622c6067207f323e2f32419"></a>protected void onMessageOpened(final <a href="PushMessage.md">PushMessage</a> message)  
Callback method which is fired when single push notification opened<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>message</strong></td>
		<td>push message which was opened </td>
	</tr>
</table>


----------  
  

#### <a name="1a26d02f5d050c8c4d7cbea129eade49ee"></a>protected void onMessageCanceled(final <a href="PushMessage.md">PushMessage</a> message)  
Callback method that is triggered when the user deletes a push notification from the Notification Center.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>message</strong></td>
		<td>push message which was canceled </td>
	</tr>
</table>


----------  
  

#### <a name="1a810e97415f05ced9abb8f135ece5c4b8"></a>protected void onMessagesGroupOpened(final List&lt;<a href="PushMessage.md">PushMessage</a>&gt; messages)  
Callback method which is fired when push notifications group opened<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>messages</strong></td>
		<td>list of push messages of the group which was opened </td>
	</tr>
</table>


----------  
  

#### <a name="1af7643913a39a07b464943576289e6c86"></a>protected boolean onMessageReceived(<a href="PushMessage.md">PushMessage</a> data)  
Extension method for push notification receive handling<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>data</strong></td>
		<td>notification data </td>
	</tr>
</table>
<strong>Returns</strong> false if notification should be generated for this data 