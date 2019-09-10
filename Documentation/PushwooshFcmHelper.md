
#### module: pushwoosh  

#### package: com.pushwoosh  

# <a name="heading"></a>class PushwooshFcmHelper  

## Members  

<table>
	<tr>
		<td><a href="#1ab8f2ea27b39641ad7de8967be7f98f4d">public static void onTokenRefresh(Context context, @Nullable String token)</a></td>
	</tr>
	<tr>
		<td><a href="#1a1fa70481f77dede755dd33208b110bc6">public static boolean onMessageReceived(Context context, RemoteMessage remoteMessage)</a></td>
	</tr>
	<tr>
		<td><a href="#1a5ef0a39a0cbb86bf1cc2ccf76bbad80a">public static boolean isPushwooshMessage(RemoteMessage remoteMessage)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1ab8f2ea27b39641ad7de8967be7f98f4d"></a>public static void onTokenRefresh(Context context, @Nullable String token)  
if you use custom FirebaseInstanceIdService call this method when FirebaseInstanceIdService#onTokenRefresh() is invoked 

----------  
  

#### <a name="1a1fa70481f77dede755dd33208b110bc6"></a>public static boolean onMessageReceived(Context context, RemoteMessage remoteMessage)  
if you use custom com.google.firebase.messaging.FirebaseMessagingService call this method when com.google.firebase.messaging.FirebaseMessagingService#onMessageReceived(RemoteMessage) is invoked<br/><br/><br/><strong>Returns</strong> true if the remoteMessage was sent via Pushwoosh and was successfully processed; otherwise false 

----------  
  

#### <a name="1a5ef0a39a0cbb86bf1cc2ccf76bbad80a"></a>public static boolean isPushwooshMessage(RemoteMessage remoteMessage)  
Check if the remoteMessage was sent via Pushwoosh<br/><br/><br/><strong>Returns</strong> true if remoteMessage was sent via Pushwoosh 