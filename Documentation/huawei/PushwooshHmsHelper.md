
#### module: pushwoosh-huawei  

#### package: com.pushwoosh.huawei  

# <a name="heading"></a>class PushwooshHmsHelper  

## Members  

<table>
	<tr>
		<td><a href="#1a38510c5a0e6fabfeaff04151e4e14c24">public static void onTokenRefresh(@Nullable String token)</a></td>
	</tr>
	<tr>
		<td><a href="#1a0f4b79ab2e6b60f6df8882b6889c8401">public static boolean onMessageReceived(Context context, RemoteMessage remoteMessage)</a></td>
	</tr>
	<tr>
		<td><a href="#1ab84ce316a45189ed698b78601aeb9040">public static boolean isPushwooshMessage(RemoteMessage remoteMessage)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a38510c5a0e6fabfeaff04151e4e14c24"></a>public static void onTokenRefresh(@Nullable String token)  
if you use custom HmsMessageService call this method when HmsMessageService#onNewToken(String token) is invoked 

----------  
  

#### <a name="1a0f4b79ab2e6b60f6df8882b6889c8401"></a>public static boolean onMessageReceived(Context context, RemoteMessage remoteMessage)  
if you use custom HmsMessageService call this method when HmsMessageService#onMessageReceived(RemoteMessage) is invoked<br/><br/><br/><strong>Returns</strong> true if the remoteMessage was sent via Pushwoosh and was successfully processed; otherwise false 

----------  
  

#### <a name="1ab84ce316a45189ed698b78601aeb9040"></a>public static boolean isPushwooshMessage(RemoteMessage remoteMessage)  
Check if the remoteMessage was sent via Pushwoosh<br/><br/><br/><strong>Returns</strong> true if remoteMessage was sent via Pushwoosh 