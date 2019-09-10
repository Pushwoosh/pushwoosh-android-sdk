
#### module: pushwoosh-location  

#### package: com.pushwoosh.location  

# <a name="heading"></a>class PushwooshLocation  
PushwooshLocation is a static class responsible for pushwoosh geolocation tracking. <br/>
 By default pushwoosh-location library automatically adds following permissions: <br/><a href="https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_FINE_LOCATION">permission.ACCESS_FINE_LOCATION</a><br/><a href="https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_COARSE_LOCATION">android.permission.ACCESS_COARSE_LOCATION</a><br/><br/><br/>
 For Android 6 and higher these permissions should be requested dynamically before invoking PushwooshLocation.startLocationTracking() 
## Members  

<table>
	<tr>
		<td><a href="#1a50082f65f094846f2888dce876ebe5ac">public static void startLocationTracking()</a></td>
	</tr>
	<tr>
		<td><a href="#1a9b788404104f3a7007935f86b358e4ac">public static void startLocationTracking(@Nullable Callback&lt;Void, LocationNotAvailableException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a4b316128fcb040cba03a036557a38f0d">public static void stopLocationTracking()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a50082f65f094846f2888dce876ebe5ac"></a>public static void startLocationTracking()  
Starts location tracking for geo push notifications. 

----------  
  

#### <a name="1a9b788404104f3a7007935f86b358e4ac"></a>public static void startLocationTracking(@Nullable Callback&lt;Void, LocationNotAvailableException&gt; callback)  
Starts location tracking for geo push notifications.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>return com.pushwoosh.function.Result#isSuccess() if user accept all needed permissions and enable location </td>
	</tr>
</table>


----------  
  

#### <a name="1a4b316128fcb040cba03a036557a38f0d"></a>public static void stopLocationTracking()  
Stops geolocation tracking. 