
#### module: pushwoosh  

#### package: com.pushwoosh.notification  

# <a name="heading"></a>class PushwooshNotificationFactory  
Default Pushwoosh implementation of NotificationFactory 
## Members  

<table>
	<tr>
		<td><a href="#1ac14f9db9ace91f9c767368052c4bc872">public Notification onGenerateNotification(@NonNull PushMessage data)</a></td>
	</tr>
	<tr>
		<td><a href="#1a37798c7ba4b41768d7ff63028a59e93d">protected Bitmap getBigPicture(final PushMessage pushData)</a></td>
	</tr>
	<tr>
		<td><a href="#1ac9bba3b82c3fafa48be62672b0fc33a2">protected Bitmap getLargeIcon(final PushMessage pushData)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1ac14f9db9ace91f9c767368052c4bc872"></a>public Notification onGenerateNotification(@NonNull <a href="PushMessage.md">PushMessage</a> data)  
Generates notification using PushMessage data.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>data</strong></td>
		<td>notification data </td>
	</tr>
</table>
<strong>Returns</strong> Notification to show 

----------  
  

#### <a name="1a37798c7ba4b41768d7ff63028a59e93d"></a>protected Bitmap getBigPicture(final <a href="PushMessage.md">PushMessage</a> pushData)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>pushData</strong></td>
		<td>push notification data </td>
	</tr>
</table>
<strong>Returns</strong> Big picture bitmap image for given notification 

----------  
  

#### <a name="1ac9bba3b82c3fafa48be62672b0fc33a2"></a>protected Bitmap getLargeIcon(final <a href="PushMessage.md">PushMessage</a> pushData)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>pushData</strong></td>
		<td>push notification data </td>
	</tr>
</table>
<strong>Returns</strong> Large icon bitmap image for given notification 