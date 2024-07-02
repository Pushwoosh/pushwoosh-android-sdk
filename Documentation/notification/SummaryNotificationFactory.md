
#### module: pushwoosh  

#### package: com.pushwoosh.notification  

# <a name="heading"></a>class SummaryNotificationFactory  

## Members  

<table>
	<tr>
		<td><a href="#1a0e84da70b4b9073c0fae46dff3f31e57">public static String NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID</a></td>
	</tr>
	<tr>
		<td><a href="#1a29b9ef5b68a2587f3de46d0c6123f682">public static Intent getNotificationIntent()</a></td>
	</tr>
	<tr>
		<td><a href="#1a7c635747854cc2085fa6677b3e6ecd00">public abstract String summaryNotificationMessage(int notificationsAmount)</a></td>
	</tr>
	<tr>
		<td><a href="#1a5aabd5bbc36bdcfa4daede06c3d536da">public abstract int summaryNotificationIconResId()</a></td>
	</tr>
	<tr>
		<td><a href="#1a91b28368fb966847f5ee5844dcfa97c2">public abstract int summaryNotificationColor()</a></td>
	</tr>
	<tr>
		<td><a href="#1aaddd6b85b91d255ce3d7a6c722a6e8fd">public boolean autoCancelSummaryNotification()</a></td>
	</tr>
	<tr>
		<td><a href="#1a42a9c13c1c210992043ba419edcb6cd8">public boolean shouldGenerateSummaryNotification()</a></td>
	</tr>
	<tr>
		<td><a href="#1a1565a684d102c9aae4a7145d85de82a3">public final Notification onGenerateSummaryNotification(int notificationsAmount, String notificationChannelId, String groupId)</a></td>
	</tr>
	<tr>
		<td><a href="#1aaff7b4fff3e47bd540469afbecdb2349">protected final Context getApplicationContext()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a0e84da70b4b9073c0fae46dff3f31e57"></a>public static String NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID  


----------  
  

#### <a name="1a29b9ef5b68a2587f3de46d0c6123f682"></a>public static Intent getNotificationIntent()  
<strong>Returns</strong> Intent to start when user clicks on the summary notification 

----------  
  

#### <a name="1a7c635747854cc2085fa6677b3e6ecd00"></a>public abstract String summaryNotificationMessage(int notificationsAmount)  
Override this method to set your custom message of the group summary notification.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>notificationsAmount</strong></td>
		<td>- number of the notifications in the group summary</td>
	</tr>
</table>
<strong>Returns</strong> Group summary notification message. By default returns "{@param notificationsAmount} new messages". 

----------  
  

#### <a name="1a5aabd5bbc36bdcfa4daede06c3d536da"></a>public abstract int summaryNotificationIconResId()  
Override this method to set your drawable as an icon of the group summary notification.<br/><br/><br/><strong>Returns</strong> Drawable resource id which will appear as an icon of the group summary notification. By default returns -1 which is used to set the same icon as a common notification. 

----------  
  

#### <a name="1a91b28368fb966847f5ee5844dcfa97c2"></a>public abstract int summaryNotificationColor()  
Override this method to set the icon color.<br/><br/><br/><strong>Returns</strong> The accent color to use. By default returns -1 which is used to set the same color as a common notification. 

----------  
  

#### <a name="1aaddd6b85b91d255ce3d7a6c722a6e8fd"></a>public boolean autoCancelSummaryNotification()  
Override this method to set whether the summary notification will be dismissed after the user opens it.<br/><br/><br/><strong>Returns</strong> The flag indicating whether the group summary notification would be cancelled automatically. By default returns false. 

----------  
  

#### <a name="1a42a9c13c1c210992043ba419edcb6cd8"></a>public boolean shouldGenerateSummaryNotification()  


----------  
  

#### <a name="1a1565a684d102c9aae4a7145d85de82a3"></a>public final Notification onGenerateSummaryNotification(int notificationsAmount, String notificationChannelId, String groupId)  


----------  
  

#### <a name="1aaff7b4fff3e47bd540469afbecdb2349"></a>protected final Context getApplicationContext()  
<strong>Returns</strong> Application context. 