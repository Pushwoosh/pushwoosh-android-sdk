
#### module: pushwoosh-firebase  

#### package: com.pushwoosh.firebase  

# <a name="heading"></a>class PushwooshFcmHelper  

## Members  

<table>
	<tr>
		<td><a href="#1ac4612a82c00c05d6e57ee592dacce9d1">public static void onTokenRefresh(@Nullable String token)</a></td>
	</tr>
	<tr>
		<td><a href="#1a3afbf08a5733de7e6344869b29b6d520">public static boolean onMessageReceived(Context context, RemoteMessage remoteMessage)</a></td>
	</tr>
	<tr>
		<td><a href="#1a8766431ae59d770782d90c308e7c97e9">public static boolean isPushwooshMessage(RemoteMessage remoteMessage)</a></td>
	</tr>
	<tr>
		<td><a href="#1ae3fa62be7b6c45432d961e43e883eb77">public static Bundle messageToBundle(RemoteMessage remoteMessage)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1ac4612a82c00c05d6e57ee592dacce9d1"></a>public static void onTokenRefresh(@Nullable String token)  
if you use custom FirebaseMessagingService call this method when FirebaseMessagingService#onNewToken(String token) is invoked 

----------  
  

#### <a name="1a3afbf08a5733de7e6344869b29b6d520"></a>public static boolean onMessageReceived(Context context, RemoteMessage remoteMessage)  
if you use custom com.google.firebase.messaging.FirebaseMessagingService call this method when com.google.firebase.messaging.FirebaseMessagingService#onMessageReceived(RemoteMessage) is invoked<br/><br/><br/><strong>Returns</strong> true if the remoteMessage was sent via Pushwoosh and was successfully processed; otherwise false 

----------  
  

#### <a name="1a8766431ae59d770782d90c308e7c97e9"></a>public static boolean isPushwooshMessage(RemoteMessage remoteMessage)  
Check if the remoteMessage was sent via Pushwoosh<br/><br/><br/><strong>Returns</strong> true if remoteMessage was sent via Pushwoosh 

----------  
  

#### <a name="1ae3fa62be7b6c45432d961e43e883eb77"></a>public static Bundle messageToBundle(RemoteMessage remoteMessage)  
Convert RemoteMessage to Bundle object<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>remoteMessage</strong></td>
		<td>- message received from Firebase </td>
	</tr>
</table>
<strong>Returns</strong> Bundle created from RemoteMessage 