
#### module: pushwoosh  

#### package: com.pushwoosh.notification.LocalNotification  

# <a name="heading"></a>class Builder  
LocalNotification Builder. 
## Members  

<table>
	<tr>
		<td><a href="#1ab88305361eac6bcd9b7d0afd730ea116">public LocalNotification build()</a></td>
	</tr>
	<tr>
		<td><a href="#1a71a5714aac5a4bf6547e4797368abc99">public Builder setTag(String tag)</a></td>
	</tr>
	<tr>
		<td><a href="#1a35df00f83b8387923f5e6f7035539515">public Builder setMessage(String message)</a></td>
	</tr>
	<tr>
		<td><a href="#1a464c00955cb02e740d26ed8f658546c0">public Builder setDelay(int delay)</a></td>
	</tr>
	<tr>
		<td><a href="#1a5f15722515d5b1a0a4498558a6869fd1">public  Builder()</a></td>
	</tr>
	<tr>
		<td><a href="#1aea9c09484d64be40c5269ce6011892a0">public Builder setBanner(String url)</a></td>
	</tr>
	<tr>
		<td><a href="#1ab3f8f4998ab52f72454821ceb5f84d1e">public Builder setSmallIcon(String name)</a></td>
	</tr>
	<tr>
		<td><a href="#1a996522a78ba947300467f1eff7b57188">public Builder setLargeIcon(String url)</a></td>
	</tr>
	<tr>
		<td><a href="#1aabfb69ace19dbba950f0ccffaa007031">public Builder setExtras(Bundle extras)</a></td>
	</tr>
	<tr>
		<td><a href="#1a74c3fd383b3dda93344390a0a46d2a62">public Builder setLink(String url)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1ab88305361eac6bcd9b7d0afd730ea116"></a>public <a href="../LocalNotification.md">LocalNotification</a> build()  
Builds and returns LocalNotification.<br/><br/><br/><strong>Returns</strong> local notification 

----------  
  

#### <a name="1a71a5714aac5a4bf6547e4797368abc99"></a>public <a href="#heading">Builder</a> setTag(String tag)  
Sets notification tag that is used to distinguish different notifications. Notifications with different tags will not replace each other. Notifications with same tag will replace each other if multi notification mode is not set <a href="../PushwooshNotificationSettings.md#1a77a3f66d5cd709ed0e2e57449d09acdb">PushwooshNotificationSettings#setMultiNotificationMode(boolean)</a><br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>tag</strong></td>
		<td>notification tag </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1a35df00f83b8387923f5e6f7035539515"></a>public <a href="#heading">Builder</a> setMessage(String message)  
Sets notification content.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>message</strong></td>
		<td>notififcation text message </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1a464c00955cb02e740d26ed8f658546c0"></a>public <a href="#heading">Builder</a> setDelay(int delay)  
Sets the delay after which notification will be displayed.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>delay</strong></td>
		<td>delay in seconds </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1a5f15722515d5b1a0a4498558a6869fd1"></a>public  Builder()  


----------  
  

#### <a name="1aea9c09484d64be40c5269ce6011892a0"></a>public <a href="#heading">Builder</a> setBanner(String url)  
Sets image for notification <a href="https://developer.android.com/reference/android/app/Notification.BigPictureStyle.html">BigPictureStyle</a><br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>url</strong></td>
		<td>image url link </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1ab3f8f4998ab52f72454821ceb5f84d1e"></a>public <a href="#heading">Builder</a> setSmallIcon(String name)  
Sets small icon image.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>name</strong></td>
		<td>resource name for small icon. </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1a996522a78ba947300467f1eff7b57188"></a>public <a href="#heading">Builder</a> setLargeIcon(String url)  
Sets large icon image.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>url</strong></td>
		<td>image url link. </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1aabfb69ace19dbba950f0ccffaa007031"></a>public <a href="#heading">Builder</a> setExtras(Bundle extras)  
Sets custom notification bundle. Warning: this can replace other settings.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>extras</strong></td>
		<td>notification bundle extras </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1a74c3fd383b3dda93344390a0a46d2a62"></a>public <a href="#heading">Builder</a> setLink(String url)  
Sets url link that will be open in browser instead of default launcher activity after clicking on notification. Deeplink url can be also used as parameter.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>url</strong></td>
		<td>url link </td>
	</tr>
</table>
<strong>Returns</strong> builder 