
#### module: pushwoosh  

#### package: com.pushwoosh.repository  

# <a name="heading"></a>class PushwooshRepository  

## Members  

<table>
	<tr>
		<td><a href="#1aafdedd1da247f564f1882079f40ac1ad">public  PushwooshRepository(RequestManager requestManager, SendTagsProcessor sendTagsProcessor, RegistrationPrefs registrationPrefs, NotificationPrefs notificationPrefs, RequestStorage requestStorage, ServerCommunicationManager serverCommunicationManager)</a></td>
	</tr>
	<tr>
		<td><a href="#1a1b194302f429af5734db4f8078364550">public String getCurrentSessionHash()</a></td>
	</tr>
	<tr>
		<td><a href="#1aae884241f00a01d61a0faddd7d8123e4">public void setCurrentSessionHash(String currentSessionHash)</a></td>
	</tr>
	<tr>
		<td><a href="#1ac6a6749858487d986c6b3ecd008c09d8">public String getCurrentRichMediaCode()</a></td>
	</tr>
	<tr>
		<td><a href="#1a4d826f2f0c9ff827ad396362736a0469">public void setCurrentRichMediaCode(String currentRichMediaCode)</a></td>
	</tr>
	<tr>
		<td><a href="#1a0d109c5a634aa32eab6835d163f1322f">public String getCurrentInAppCode()</a></td>
	</tr>
	<tr>
		<td><a href="#1ad77a985e495a28fbcc1b586bd53f43ea">public void setCurrentInAppCode(String currentInAppCode)</a></td>
	</tr>
	<tr>
		<td><a href="#1aaafcfc91ddb43175faa7bd11d719f030">public void sendAppOpen()</a></td>
	</tr>
	<tr>
		<td><a href="#1a0d0507a7313fa1a92d9123cfd7dfeae8">public void sendTags(@NonNull TagsBundle tags, Callback&lt;Void, PushwooshException&gt; listener)</a></td>
	</tr>
	<tr>
		<td><a href="#1aa4da28561f9af02ef289a4d792acb7c7">public void sendEmailTags(@NonNull TagsBundle tags, String email, Callback&lt;Void, PushwooshException&gt; listener)</a></td>
	</tr>
	<tr>
		<td><a href="#1a8f2065d37abdb4d20089dfe7ffc939bb">public void getTags(@Nullable final Callback&lt;TagsBundle, GetTagsException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1ab2131522573ed24f8dae1023796f5563">public void sendInappPurchase(String sku, BigDecimal price, String currency, Date purchaseTime)</a></td>
	</tr>
	<tr>
		<td><a href="#1ae1c204e37794340273fbf2face01f6a6">public void sendPushOpened(String hash, String metadata)</a></td>
	</tr>
	<tr>
		<td><a href="#1a2c5a89e011a88934885c5351d9339fa0">public void sendPushDelivered(String hash, String metaData)</a></td>
	</tr>
	<tr>
		<td><a href="#1a8fd5a75d805c3fb4da37780a0a9ec895">public void prefetchTags()</a></td>
	</tr>
	<tr>
		<td><a href="#1a72fb2808d768fd3ee93b34fbcc04166e">public List&lt;PushMessage&gt; getPushHistory()</a></td>
	</tr>
	<tr>
		<td><a href="#1a9be5b56ab39c5f2e5f9e5d3ee41d4878">public void removeAllDeviceData()</a></td>
	</tr>
	<tr>
		<td><a href="#1a53516dac5300b9b5c98778eda60b65ac">public boolean isDeviceDataRemoved()</a></td>
	</tr>
	<tr>
		<td><a href="#1aa8e6e8ce6b363a99140343ebea8b6533">public void communicationEnabled(boolean enable)</a></td>
	</tr>
	<tr>
		<td><a href="#1af4a14f772f56a167a68e3233bb01dbab">public boolean isCommunicationEnabled()</a></td>
	</tr>
	<tr>
		<td><a href="#1ae6a5aaaf47c98cbea18753db35397716">public boolean isGdprEnable()</a></td>
	</tr>
	<tr>
		<td><a href="#1a8f1aa4ca05252b3e56710f77f7c69cca">public String getHwid()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1aafdedd1da247f564f1882079f40ac1ad"></a>public  PushwooshRepository(RequestManager requestManager, SendTagsProcessor sendTagsProcessor, RegistrationPrefs registrationPrefs, NotificationPrefs notificationPrefs, RequestStorage requestStorage, ServerCommunicationManager serverCommunicationManager)  


----------  
  

#### <a name="1a1b194302f429af5734db4f8078364550"></a>public String getCurrentSessionHash()  


----------  
  

#### <a name="1aae884241f00a01d61a0faddd7d8123e4"></a>public void setCurrentSessionHash(String currentSessionHash)  


----------  
  

#### <a name="1ac6a6749858487d986c6b3ecd008c09d8"></a>public String getCurrentRichMediaCode()  


----------  
  

#### <a name="1a4d826f2f0c9ff827ad396362736a0469"></a>public void setCurrentRichMediaCode(String currentRichMediaCode)  


----------  
  

#### <a name="1a0d109c5a634aa32eab6835d163f1322f"></a>public String getCurrentInAppCode()  


----------  
  

#### <a name="1ad77a985e495a28fbcc1b586bd53f43ea"></a>public void setCurrentInAppCode(String currentInAppCode)  


----------  
  

#### <a name="1aaafcfc91ddb43175faa7bd11d719f030"></a>public void sendAppOpen()  


----------  
  

#### <a name="1a0d0507a7313fa1a92d9123cfd7dfeae8"></a>public void sendTags(@NonNull <a href="../tags/TagsBundle.md">TagsBundle</a> tags, <a href="../function/Callback.md">Callback</a>&lt;Void, PushwooshException&gt; listener)  


----------  
  

#### <a name="1aa4da28561f9af02ef289a4d792acb7c7"></a>public void sendEmailTags(@NonNull <a href="../tags/TagsBundle.md">TagsBundle</a> tags, String email, <a href="../function/Callback.md">Callback</a>&lt;Void, PushwooshException&gt; listener)  


----------  
  

#### <a name="1a8f2065d37abdb4d20089dfe7ffc939bb"></a>public void getTags(@Nullable final <a href="../function/Callback.md">Callback</a>&lt;<a href="../tags/TagsBundle.md">TagsBundle</a>, GetTagsException&gt; callback)  


----------  
  

#### <a name="1ab2131522573ed24f8dae1023796f5563"></a>public void sendInappPurchase(String sku, BigDecimal price, String currency, Date purchaseTime)  


----------  
  

#### <a name="1ae1c204e37794340273fbf2face01f6a6"></a>public void sendPushOpened(String hash, String metadata)  


----------  
  

#### <a name="1a2c5a89e011a88934885c5351d9339fa0"></a>public void sendPushDelivered(String hash, String metaData)  


----------  
  

#### <a name="1a8fd5a75d805c3fb4da37780a0a9ec895"></a>public void prefetchTags()  


----------  
  

#### <a name="1a72fb2808d768fd3ee93b34fbcc04166e"></a>public List&lt;<a href="../notification/PushMessage.md">PushMessage</a>&gt; getPushHistory()  


----------  
  

#### <a name="1a9be5b56ab39c5f2e5f9e5d3ee41d4878"></a>public void removeAllDeviceData()  


----------  
  

#### <a name="1a53516dac5300b9b5c98778eda60b65ac"></a>public boolean isDeviceDataRemoved()  


----------  
  

#### <a name="1aa8e6e8ce6b363a99140343ebea8b6533"></a>public void communicationEnabled(boolean enable)  


----------  
  

#### <a name="1af4a14f772f56a167a68e3233bb01dbab"></a>public boolean isCommunicationEnabled()  


----------  
  

#### <a name="1ae6a5aaaf47c98cbea18753db35397716"></a>public boolean isGdprEnable()  


----------  
  

#### <a name="1a8f1aa4ca05252b3e56710f77f7c69cca"></a>public String getHwid()  
