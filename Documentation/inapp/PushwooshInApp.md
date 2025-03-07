
#### module: pushwoosh  

#### package: com.pushwoosh.inapp  

# <a name="heading"></a>class PushwooshInApp  
PushwooshInApp is responsible for In-App messages functionality.<br/><em>See also:</em> InAppManager 
## Members  

<table>
	<tr>
		<td><a href="#1ac886f1082fb2462f15a94729a6d442f0">public static PushwooshInApp getInstance()</a></td>
	</tr>
	<tr>
		<td><a href="#1a46ceeb2bdfa70f1556ae9d1755089f4a">public void postEvent(@NonNull String event)</a></td>
	</tr>
	<tr>
		<td><a href="#1ac4f379b76b87440194b3f2025e6619e5">public void postEvent(@NonNull String event, TagsBundle attributes)</a></td>
	</tr>
	<tr>
		<td><a href="#1af2304aece7a98931d1140f803d064f36">public void postEvent(@NonNull String event, @Nullable TagsBundle attributes, Callback&lt;Void, PostEventException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a69cdb1592ad7640a49ba8bac8a14d54d">public void setUserId(@NonNull String userId)</a></td>
	</tr>
	<tr>
		<td><a href="#1a0b6e5f6e94e5fc09262da3fdb6fa82af">public String getUserId()</a></td>
	</tr>
	<tr>
		<td><a href="#1ab7eba48be74fc3b9626d4bdfe2e87f82">public void mergeUserId(@NonNull String oldUserId, @NonNull String newUserId, boolean doMerge, @Nullable Callback&lt;Void, MergeUserException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a07b59127b358b800ab586e1dcf7c46b3">public void addJavascriptInterface(@NonNull Object object, @NonNull String name)</a></td>
	</tr>
	<tr>
		<td><a href="#1a109af9272f4013f83d7cf79578695c53">public void removeJavascriptInterface(@NonNull String name)</a></td>
	</tr>
	<tr>
		<td><a href="#1adbdc7ed0b2b8fb49ace8a54eac9be681">public void registerJavascriptInterface(@NonNull String className, @NonNull String name)</a></td>
	</tr>
	<tr>
		<td><a href="#1a88e86887934438f4d4b1cc71a8171046">public void resetBusinessCasesFrequencyCapping()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1ac886f1082fb2462f15a94729a6d442f0"></a>public static <a href="#heading">PushwooshInApp</a> getInstance()  
<strong>Returns</strong> PushwooshInApp shared instance. 

----------  
  

#### <a name="1a46ceeb2bdfa70f1556ae9d1755089f4a"></a>public void postEvent(@NonNull String event)  
<em>See also:</em> InAppManager::postEvent(String) 

----------  
  

#### <a name="1ac4f379b76b87440194b3f2025e6619e5"></a>public void postEvent(@NonNull String event, <a href="../tags/TagsBundle.md">TagsBundle</a> attributes)  
<em>See also:</em> InAppManager::postEvent(String, TagsBundle, boolean) 

----------  
  

#### <a name="1af2304aece7a98931d1140f803d064f36"></a>public void postEvent(@NonNull String event, @Nullable <a href="../tags/TagsBundle.md">TagsBundle</a> attributes, <a href="../function/Callback.md">Callback</a>&lt;Void, PostEventException&gt; callback)  
<em>See also:</em> InAppManager::postEvent(String, TagsBundle, Callback) 

----------  
  

#### <a name="1a69cdb1592ad7640a49ba8bac8a14d54d"></a>public void setUserId(@NonNull String userId)  
<em>See also:</em> com.pushwoosh.Pushwoosh::setUserId(String) 

----------  
  

#### <a name="1a0b6e5f6e94e5fc09262da3fdb6fa82af"></a>public String getUserId()  
<em>See also:</em> Pushwoosh::getUserId() 

----------  
  

#### <a name="1ab7eba48be74fc3b9626d4bdfe2e87f82"></a>public void mergeUserId(@NonNull String oldUserId, @NonNull String newUserId, boolean doMerge, @Nullable <a href="../function/Callback.md">Callback</a>&lt;Void, MergeUserException&gt; callback)  
<em>See also:</em> com.pushwoosh.Pushwoosh::mergeUserId(String, String, boolean, Callback) 

----------  
  

#### <a name="1a07b59127b358b800ab586e1dcf7c46b3"></a>public void addJavascriptInterface(@NonNull Object object, @NonNull String name)  
<em>See also:</em> InAppManager::addJavascriptInterface(Object, String) 

----------  
  

#### <a name="1a109af9272f4013f83d7cf79578695c53"></a>public void removeJavascriptInterface(@NonNull String name)  
<em>See also:</em> InAppManager::removeJavascriptInterface(String) 

----------  
  

#### <a name="1adbdc7ed0b2b8fb49ace8a54eac9be681"></a>public void registerJavascriptInterface(@NonNull String className, @NonNull String name)  
<em>See also:</em> InAppManager::registerJavascriptInterface(String, String) 

----------  
  

#### <a name="1a88e86887934438f4d4b1cc71a8171046"></a>public void resetBusinessCasesFrequencyCapping()  
<em>See also:</em> InAppManager::resetBusinessCasesFrequencyCapping() 