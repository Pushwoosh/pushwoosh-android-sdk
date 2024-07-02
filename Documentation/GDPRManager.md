
#### module: pushwoosh  

#### package: com.pushwoosh  

# <a name="heading"></a>class GDPRManager  

## Members  

<table>
	<tr>
		<td><a href="#1a2904507fdc9550b183af3c0037858e8f">public static final String TAG</a></td>
	</tr>
	<tr>
		<td><a href="#1a00e074f60fa9ed08f903eeca1cd46c84">public static GDPRManager getInstance()</a></td>
	</tr>
	<tr>
		<td><a href="#1a608dfada11c20f8fa0dfc04e9d7477d6">public void setCommunicationEnabled(boolean enable, Callback&lt;Void, PushwooshException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1af683680da86cc15ee1b8f7594d9fa110">public void removeAllDeviceData(Callback&lt;Void, PushwooshException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a1e0dde82dd0625ba52b48abd2ab55ecd">public boolean isDeviceDataRemoved()</a></td>
	</tr>
	<tr>
		<td><a href="#1a895e51c1633087fd584305ff36b80315">public boolean isCommunicationEnabled()</a></td>
	</tr>
	<tr>
		<td><a href="#1ac6e57f38323963ac2bb57913bc643e22">public boolean isAvailable()</a></td>
	</tr>
	<tr>
		<td><a href="#1a2a10bf1b157b5bab7ace3f7a8d3f07bc">public void showGDPRDeletionUI()</a></td>
	</tr>
	<tr>
		<td><a href="#1a087cbe4f23fe04750a97463cceeac5c7">public void showGDPRConsentUI()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a2904507fdc9550b183af3c0037858e8f"></a>public static final String TAG  


----------  
  

#### <a name="1a00e074f60fa9ed08f903eeca1cd46c84"></a>public static GDPRManager getInstance()  


----------  
  

#### <a name="1a608dfada11c20f8fa0dfc04e9d7477d6"></a>public void setCommunicationEnabled(boolean enable, <a href="function/Callback.md">Callback</a>&lt;Void, PushwooshException&gt; callback)  
Enable/disable all communication with Pushwoosh. Enabled by default. 

----------  
  

#### <a name="1af683680da86cc15ee1b8f7594d9fa110"></a>public void removeAllDeviceData(<a href="function/Callback.md">Callback</a>&lt;Void, PushwooshException&gt; callback)  
Removes all device data from Pushwoosh and stops all interactions and communication permanently. 

----------  
  

#### <a name="1a1e0dde82dd0625ba52b48abd2ab55ecd"></a>public boolean isDeviceDataRemoved()  
Indicates availability of the GDPR compliance solution. 

----------  
  

#### <a name="1a895e51c1633087fd584305ff36b80315"></a>public boolean isCommunicationEnabled()  
Return flag is enable communication with server 

----------  
  

#### <a name="1ac6e57f38323963ac2bb57913bc643e22"></a>public boolean isAvailable()  
Return flag is enabled GDPR on server 

----------  
  

#### <a name="1a2a10bf1b157b5bab7ace3f7a8d3f07bc"></a>public void showGDPRDeletionUI()  
Show inApp for all device data from Pushwoosh and stops all interactions and communication permanently. 

----------  
  

#### <a name="1a087cbe4f23fe04750a97463cceeac5c7"></a>public void showGDPRConsentUI()  
Show inApp for change setting Enable/disable all communication with Pushwoosh 