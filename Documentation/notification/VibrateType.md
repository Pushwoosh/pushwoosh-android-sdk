
#### module: pushwoosh  

#### package: com.pushwoosh.notification  

# <a name="heading"></a>enum VibrateType  
Push notification vibration setting. Application must use <a href="https://developer.android.com/reference/android/Manifest.permission.html#VIBRATE">VIBRATE</a> permission in order for vibration to work.<br/><em>See also:</em> com.pushwoosh.notification.PushwooshNotificationSettings::setVibrateNotificationType(VibrateType) 
## Members  

<table>
	<tr>
		<td><a href="#1a91350b5c19ed11521a029d0f397ea078">public  DEFAULT_MODE</a></td>
	</tr>
	<tr>
		<td><a href="#1aa691a58a4e56bea5a748142f5d5229d4">public  NO_VIBRATE</a></td>
	</tr>
	<tr>
		<td><a href="#1aedf0a04472adde3baaa880689d0ef99e">public  ALWAYS</a></td>
	</tr>
	<tr>
		<td><a href="#1a6e6336a85609ef75d85b0358994dca8b">public static VibrateType fromInt(int x)</a></td>
	</tr>
	<tr>
		<td><a href="#1a18c06bcbda828bb68dfd9db1b046c90b">public  VibrateType(int value)</a></td>
	</tr>
	<tr>
		<td><a href="#1ab8b710ac19d41b5fa32bc38b302fd25e">public int getValue()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a91350b5c19ed11521a029d0f397ea078"></a>public  DEFAULT_MODE  
Notification causes vibration if AudioManager ringer mode is <a href="https://developer.android.com/reference/android/media/AudioManager.html#RINGER_MODE_VIBRATE">RINGER_MODE_VIBRATE</a>. 

----------  
  

#### <a name="1aa691a58a4e56bea5a748142f5d5229d4"></a>public  NO_VIBRATE  
Notification will not cause vibration. 

----------  
  

#### <a name="1aedf0a04472adde3baaa880689d0ef99e"></a>public  ALWAYS  
Notification will always cause vibration. 

----------  
  

#### <a name="1a6e6336a85609ef75d85b0358994dca8b"></a>public static <a href="#heading">VibrateType</a> fromInt(int x)  


----------  
  

#### <a name="1a18c06bcbda828bb68dfd9db1b046c90b"></a>public  VibrateType(int value)  


----------  
  

#### <a name="1ab8b710ac19d41b5fa32bc38b302fd25e"></a>public int getValue()  
