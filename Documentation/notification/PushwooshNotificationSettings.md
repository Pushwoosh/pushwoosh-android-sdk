
#### module: pushwoosh  

#### package: com.pushwoosh.notification  

# <a name="heading"></a>class PushwooshNotificationSettings  
PushwooshNotificationSettings class is used to customise push notification appearance. 
## Members  

<table>
	<tr>
		<td><a href="#1a77a3f66d5cd709ed0e2e57449d09acdb">public static void setMultiNotificationMode(boolean on)</a></td>
	</tr>
	<tr>
		<td><a href="#1ac98e3ace09ab7cc9c75c7f5d3b6f5475">public static void setSoundNotificationType(SoundType soundNotificationType)</a></td>
	</tr>
	<tr>
		<td><a href="#1a3435c9af99f28959e87eda77896bb4a2">public static void setVibrateNotificationType(VibrateType vibrateNotificationType)</a></td>
	</tr>
	<tr>
		<td><a href="#1afca3cf323c292700a9f1d426e510804d">public static void setLightScreenOnNotification(boolean on)</a></td>
	</tr>
	<tr>
		<td><a href="#1ab6a9e720cce1b9b53c8ff77559b9adf0">public static void setEnableLED(boolean on)</a></td>
	</tr>
	<tr>
		<td><a href="#1a6a2f0c63f5d400e8607484c5c732b072">public static void setColorLED(@ColorInt int color)</a></td>
	</tr>
	<tr>
		<td><a href="#1a1c966d753531babb6172045ee0baae34">public static void setNotificationIconBackgroundColor(@ColorInt int color)</a></td>
	</tr>
	<tr>
		<td><a href="#1a6228c9b050633e34d6662f93c2856b6f">public static boolean areNotificationsEnabled()</a></td>
	</tr>
	<tr>
		<td><a href="#1af7378863944b37657ccd70da45fec5e5">public static void enableNotifications(boolean on)</a></td>
	</tr>
	<tr>
		<td><a href="#1abadb2a9eee001a9650c773e3c291844b">public static void setNotificationChannelName(String name)</a></td>
	</tr>
	<tr>
		<td><a href="#1acfbd348d805f8aa0f9861a7ed78b4658">public static void lazyInitPushwoosh(Context context)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a77a3f66d5cd709ed0e2e57449d09acdb"></a>public static void setMultiNotificationMode(boolean on)  
Allows multiple notifications to be displayed in notification center. By default SDK uses single notification mode where each notification overrides previously displayed notification.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>on</strong></td>
		<td>enable multi/single notification mode </td>
	</tr>
</table>


----------  
  

#### <a name="1ac98e3ace09ab7cc9c75c7f5d3b6f5475"></a>public static void setSoundNotificationType(<a href="SoundType.md">SoundType</a> soundNotificationType)  
Set whether sound should be played when notification is received.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>soundNotificationType</strong></td>
		<td>sound setting </td>
	</tr>
</table>


----------  
  

#### <a name="1a3435c9af99f28959e87eda77896bb4a2"></a>public static void setVibrateNotificationType(<a href="VibrateType.md">VibrateType</a> vibrateNotificationType)  
Set whether device should vibrate when notification is received. If "Force Vibration" is set in Pushwoosh control panel for remote notification it will cause vibration regardless of this setting.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>vibrateNotificationType</strong></td>
		<td>vibration setting </td>
	</tr>
</table>


----------  
  

#### <a name="1afca3cf323c292700a9f1d426e510804d"></a>public static void setLightScreenOnNotification(boolean on)  
Set whether notification should unlock screen.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>on</strong></td>
		<td>enable screen unlock </td>
	</tr>
</table>


----------  
  

#### <a name="1ab6a9e720cce1b9b53c8ff77559b9adf0"></a>public static void setEnableLED(boolean on)  
Set whether notification should cause LED blinking.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>on</strong></td>
		<td>enable LED blinking </td>
	</tr>
</table>


----------  
  

#### <a name="1a6a2f0c63f5d400e8607484c5c732b072"></a>public static void setColorLED(@ColorInt int color)  
Set LED color. <a href="PushwooshNotificationSettings.md#1ab6a9e720cce1b9b53c8ff77559b9adf0">setEnableLED(boolean)</a> must be set to adjust LED color.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>color</strong></td>
		<td>LED color </td>
	</tr>
</table>


----------  
  

#### <a name="1a1c966d753531babb6172045ee0baae34"></a>public static void setNotificationIconBackgroundColor(@ColorInt int color)  
Set notification icon background color<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>color</strong></td>
		<td>background color </td>
	</tr>
</table>
<em>See also:</em> <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setSmallIcon(int)">Notification.Builder.setSmallIcon</a>

----------  
  

#### <a name="1a6228c9b050633e34d6662f93c2856b6f"></a>public static boolean areNotificationsEnabled()  
<strong>Returns</strong> true if notifications are enabled and will appear in notification center. 

----------  
  

#### <a name="1af7378863944b37657ccd70da45fec5e5"></a>public static void enableNotifications(boolean on)  
Set whether notifications should be enabled<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>on</strong></td>
		<td>enable notifications </td>
	</tr>
</table>


----------  
  

#### <a name="1abadb2a9eee001a9650c773e3c291844b"></a>public static void setNotificationChannelName(String name)  
Set default notification channel name for API 26<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>name</strong></td>
		<td>name of notification channel </td>
	</tr>
</table>


----------  
  

#### <a name="1acfbd348d805f8aa0f9861a7ed78b4658"></a>public static void lazyInitPushwoosh(Context context)  
