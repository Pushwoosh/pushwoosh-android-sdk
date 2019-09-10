
#### module: pushwoosh  

#### package: com.pushwoosh.notification  

# <a name="heading"></a>enum SoundType  
Push notification sound setting.<br/><em>See also:</em> PushwooshNotificationSettings::setSoundNotificationType(SoundType) 
## Members  

<table>
	<tr>
		<td><a href="#1ae722aa90a2f3cf07929198aed02a1ce1">public  DEFAULT_MODE</a></td>
	</tr>
	<tr>
		<td><a href="#1a92d0cdb9c6e07996523ee97633cc8727">public  NO_SOUND</a></td>
	</tr>
	<tr>
		<td><a href="#1a100f7c82bb1bae08e555bd0a108c661e">public  ALWAYS</a></td>
	</tr>
	<tr>
		<td><a href="#1ad0d65539ea70dde1df9e682587874436">public static SoundType fromInt(int x)</a></td>
	</tr>
	<tr>
		<td><a href="#1a0c184088a84ae6f6eb09c569dbc7714c">public  SoundType(int value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a0fb0f276569c98f475f93a821b39b260">public int getValue()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1ae722aa90a2f3cf07929198aed02a1ce1"></a>public  DEFAULT_MODE  
Sound is played when notification arrives if AudioManager ringer mode is <a href="https://developer.android.com/reference/android/media/AudioManager.html#RINGER_MODE_NORMAL">RINGER_MODE_NORMAL</a>. 

----------  
  

#### <a name="1a92d0cdb9c6e07996523ee97633cc8727"></a>public  NO_SOUND  
Sound is never played when notification arrives. 

----------  
  

#### <a name="1a100f7c82bb1bae08e555bd0a108c661e"></a>public  ALWAYS  
Sound is always played when notification arrives. 

----------  
  

#### <a name="1ad0d65539ea70dde1df9e682587874436"></a>public static <a href="#heading">SoundType</a> fromInt(int x)  


----------  
  

#### <a name="1a0c184088a84ae6f6eb09c569dbc7714c"></a>public  SoundType(int value)  


----------  
  

#### <a name="1a0fb0f276569c98f475f93a821b39b260"></a>public int getValue()  
