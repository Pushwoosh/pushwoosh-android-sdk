
#### module: pushwoosh  

#### package: com.pushwoosh.notification  

# <a name="heading"></a>class PushMessage  
Push message data class. 
## Members  

<table>
	<tr>
		<td><a href="#1a89e2a91baf70d8d77289d3087b67ae63">public  PushMessage(@NonNull Bundle extras)</a></td>
	</tr>
	<tr>
		<td><a href="#1a2692abead156cad99e72925aeeff94cd">public String getLargeIconUrl()</a></td>
	</tr>
	<tr>
		<td><a href="#1a4f96b2805f0c6b8f1bd24cd380d8b650">public String getBigPictureUrl()</a></td>
	</tr>
	<tr>
		<td><a href="#1a6840c1f1282b7d714dbc85fd34f325aa">public String getHeader()</a></td>
	</tr>
	<tr>
		<td><a href="#1ae36476eefb9b67df72babc730c020578">public String getMessage()</a></td>
	</tr>
	<tr>
		<td><a href="#1a15733fd7534f81194e5fb19ab1af28bf">public String getPushHash()</a></td>
	</tr>
	<tr>
		<td><a href="#1a6620bee567039ef83355cf9193aef4e0">public String getPushMetaData()</a></td>
	</tr>
	<tr>
		<td><a href="#1ab1092848689a9735eb8920110c8a8c47">public long getPushwooshNotificationId()</a></td>
	</tr>
	<tr>
		<td><a href="#1a3021a5693dea68f6260048c154d212bc">public boolean isSilent()</a></td>
	</tr>
	<tr>
		<td><a href="#1a1367c5dc973fe988fa3610929a5387ef">public boolean isLocal()</a></td>
	</tr>
	<tr>
		<td><a href="#1a68e55d9159f161efd1e8b4e5853eef34">public Integer getIconBackgroundColor()</a></td>
	</tr>
	<tr>
		<td><a href="#1a78279065e6719908fe68f322dad38d24">public Integer getLed()</a></td>
	</tr>
	<tr>
		<td><a href="#1a0a0475a9f77d145c659810132fc1d0b9">public String getSound()</a></td>
	</tr>
	<tr>
		<td><a href="#1a1ce51d171c4aa13366fa2842453726d9">public boolean getVibration()</a></td>
	</tr>
	<tr>
		<td><a href="#1aeebd3cc8f8bdcbb5aa6b87925f4f5dc5">public String getTicker()</a></td>
	</tr>
	<tr>
		<td><a href="#1a92859a33807b64cb890458d8684ef455">public int getSmallIcon()</a></td>
	</tr>
	<tr>
		<td><a href="#1a156900aad01f2b8bf1144cd79c2ecaa4">public int getPriority()</a></td>
	</tr>
	<tr>
		<td><a href="#1ad1319721cc6d8899850b9188d39f3174">public int getBadges()</a></td>
	</tr>
	<tr>
		<td><a href="#1aa844f51d6752bbd6be58122c53c912ac">public boolean isBadgesAdditive()</a></td>
	</tr>
	<tr>
		<td><a href="#1a8d484dff7cc08f41cb694d9d65ff910c">public int getVisibility()</a></td>
	</tr>
	<tr>
		<td><a href="#1a3e1fdaa96ec446a62f8529a65cbee67a">public int getLedOnMS()</a></td>
	</tr>
	<tr>
		<td><a href="#1a15f60a5461ad235352233882c31b9f64">public int getLedOffMS()</a></td>
	</tr>
	<tr>
		<td><a href="#1a1680596d3bf8f915749870d1a67366e0">public List&lt;Action&gt; getActions()</a></td>
	</tr>
	<tr>
		<td><a href="#1a5c604d271c3dfee3f976b010b7c95654">public String getTag()</a></td>
	</tr>
	<tr>
		<td><a href="#1a8da1964ed9b18bbc99b7c81ac4f26450">public boolean isLockScreen()</a></td>
	</tr>
	<tr>
		<td><a href="#1a7bdfd7ec12174071956a35e0f8b3a3b8">public String getCustomData()</a></td>
	</tr>
	<tr>
		<td><a href="#1a8e77aca1068d0c6ac62b9c0c51710f0c">public String getGroupId()</a></td>
	</tr>
	<tr>
		<td><a href="#1acf6890db68d99c8251c169ab90c86a53">public Bundle toBundle()</a></td>
	</tr>
	<tr>
		<td><a href="#1a6d692864571ea73719d610499fb7ee4c">public JSONObject toJson()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a89e2a91baf70d8d77289d3087b67ae63"></a>public  PushMessage(@NonNull Bundle extras)  


----------  
  

#### <a name="1a2692abead156cad99e72925aeeff94cd"></a>public String getLargeIconUrl()  
<strong>Returns</strong> Notification large icon url. <em>See also:</em> <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setLargeIcon(android.graphics.Bitmap)">Notification.Builder.setLargeIcon</a>

----------  
  

#### <a name="1a4f96b2805f0c6b8f1bd24cd380d8b650"></a>public String getBigPictureUrl()  
<strong>Returns</strong> Notification big picture url. <em>See also:</em> <a href="https://developer.android.com/reference/android/app/Notification.BigPictureStyle.html#bigPicture(android.graphics.Bitmap)">Notification.BigPictureStyle.bigPicture</a>

----------  
  

#### <a name="1a6840c1f1282b7d714dbc85fd34f325aa"></a>public String getHeader()  
<strong>Returns</strong> Notification title. <em>See also:</em> <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setContentTitle(java.lang.CharSequence)">Notification.Builder.setContentTitle</a>

----------  
  

#### <a name="1ae36476eefb9b67df72babc730c020578"></a>public String getMessage()  
<strong>Returns</strong> Notification message. <em>See also:</em> <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setContentText(java.lang.CharSequence)">Notification.Builder.setContentText</a>

----------  
  

#### <a name="1a15733fd7534f81194e5fb19ab1af28bf"></a>public String getPushHash()  
<strong>Returns</strong> Pushmessage hash. Pushes triggered using remote API may not have hash. 

----------  
  

#### <a name="1a6620bee567039ef83355cf9193aef4e0"></a>public String getPushMetaData()  
<strong>Returns</strong> Pushmessage metadata. 

----------  
  

#### <a name="1ab1092848689a9735eb8920110c8a8c47"></a>public long getPushwooshNotificationId()  
<strong>Returns</strong> Pushwoosh Notification ID 

----------  
  

#### <a name="1a3021a5693dea68f6260048c154d212bc"></a>public boolean isSilent()  
<strong>Returns</strong> true if push message is "silent" and will not present notification. 

----------  
  

#### <a name="1a1367c5dc973fe988fa3610929a5387ef"></a>public boolean isLocal()  
<strong>Returns</strong> true if push notification is local. 

----------  
  

#### <a name="1a68e55d9159f161efd1e8b4e5853eef34"></a>public Integer getIconBackgroundColor()  
<strong>Returns</strong> notification icon background color. <em>See also:</em> <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setColor(int)">Notification.Builder.setColor</a>

----------  
  

#### <a name="1a78279065e6719908fe68f322dad38d24"></a>public Integer getLed()  
<strong>Returns</strong> Led color for current push message. 

----------  
  

#### <a name="1a0a0475a9f77d145c659810132fc1d0b9"></a>public String getSound()  
<strong>Returns</strong> sound uri for current push message. 

----------  
  

#### <a name="1a1ce51d171c4aa13366fa2842453726d9"></a>public boolean getVibration()  
<strong>Returns</strong> true if device should vibrate in response to notification. 

----------  
  

#### <a name="1aeebd3cc8f8bdcbb5aa6b87925f4f5dc5"></a>public String getTicker()  
<strong>Returns</strong> Ticker. <em>See also:</em> <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setTicker(java.lang.CharSequence)">Notification.Builder.setTicker</a>

----------  
  

#### <a name="1a92859a33807b64cb890458d8684ef455"></a>public int getSmallIcon()  
<strong>Returns</strong> Notification small icon. <em>See also:</em> <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setSmallIcon(int)">Notification.Builder.setSmallIcon</a>

----------  
  

#### <a name="1a156900aad01f2b8bf1144cd79c2ecaa4"></a>public int getPriority()  
<strong>Returns</strong> Notification priority. <em>See also:</em> <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setPriority(int)">Notification.Builder.setPriority</a>

----------  
  

#### <a name="1ad1319721cc6d8899850b9188d39f3174"></a>public int getBadges()  
<strong>Returns</strong> Application icon badge number. 

----------  
  

#### <a name="1aa844f51d6752bbd6be58122c53c912ac"></a>public boolean isBadgesAdditive()  
<strong>Returns</strong> True if there is a sign '+' or '-' at the beginning of the badge number. 

----------  
  

#### <a name="1a8d484dff7cc08f41cb694d9d65ff910c"></a>public int getVisibility()  
<strong>Returns</strong> Notification visibility. <em>See also:</em> <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setVisibility(int)">Notification.Builder.setVisibility</a>

----------  
  

#### <a name="1a3e1fdaa96ec446a62f8529a65cbee67a"></a>public int getLedOnMS()  
<strong>Returns</strong> LED on duration in ms 

----------  
  

#### <a name="1a15f60a5461ad235352233882c31b9f64"></a>public int getLedOffMS()  
<strong>Returns</strong> LED off duration in ms 

----------  
  

#### <a name="1a1680596d3bf8f915749870d1a67366e0"></a>public List&lt;Action&gt; getActions()  
<strong>Returns</strong> Notification actions 

----------  
  

#### <a name="1a5c604d271c3dfee3f976b010b7c95654"></a>public String getTag()  
<strong>Returns</strong> Notification tag. Notifications with different tags will not replace each other. Notifications with same tag will replace each other if multinotification mode is on <a href="PushwooshNotificationSettings.md#1a77a3f66d5cd709ed0e2e57449d09acdb">com.pushwoosh.notification.PushwooshNotificationSettings#setMultiNotificationMode(boolean)</a>

----------  
  

#### <a name="1a8da1964ed9b18bbc99b7c81ac4f26450"></a>public boolean isLockScreen()  
<strong>Returns</strong> true if notification presents Rich Media on lock screen. 

----------  
  

#### <a name="1a7bdfd7ec12174071956a35e0f8b3a3b8"></a>public String getCustomData()  
<strong>Returns</strong> custom push data attached to incoming push message 

----------  
  

#### <a name="1a8e77aca1068d0c6ac62b9c0c51710f0c"></a>public String getGroupId()  
<strong>Returns</strong> notification group id 

----------  
  

#### <a name="1acf6890db68d99c8251c169ab90c86a53"></a>public Bundle toBundle()  
<strong>Returns</strong> Bundle representation of push payload 

----------  
  

#### <a name="1a6d692864571ea73719d610499fb7ee4c"></a>public JSONObject toJson()  
<strong>Returns</strong> JSON representation of push payload 