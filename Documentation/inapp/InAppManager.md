
#### module: pushwoosh  

#### package: com.pushwoosh.inapp  

# <a name="heading"></a>class InAppManager  
InAppManager is responsible for In-App messaging functionality.<br/><em>See also:</em> <a href="https://docs.pushwoosh.com/platform-docs/automation/behavior-based-messaging/in-app-messaging">In-App Messaging</a>
## Members  

<table>
	<tr>
		<td><a href="#1a967a88de0a2694725d245cb815cac3ba">public static InAppManager getInstance()</a></td>
	</tr>
	<tr>
		<td><a href="#1a5ade44e97fd373900a99145fd37d19b1">public void postEvent(@NonNull String event)</a></td>
	</tr>
	<tr>
		<td><a href="#1a1a8a9722e5e904191c7ce5ae58c29c8a">public void postEvent(@NonNull String event, TagsBundle attributes, boolean isInternal)</a></td>
	</tr>
	<tr>
		<td><a href="#1ad74b43d9963f9d858160da6486268292">public void postEvent(@NonNull String event, @Nullable TagsBundle attributes, Callback&lt;Void, PostEventException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a6b671e483ce5a251147cd4eb7397d09a">public void addJavascriptInterface(@NonNull Object object, @NonNull String name)</a></td>
	</tr>
	<tr>
		<td><a href="#1acf42a9dd72e6f8db1aee936a83bfc7b2">public void removeJavascriptInterface(@NonNull String name)</a></td>
	</tr>
	<tr>
		<td><a href="#1aea84ded35a2122ff7b1a742d60662a6c">public void registerJavascriptInterface(@NonNull String className, @NonNull String name)</a></td>
	</tr>
	<tr>
		<td><a href="#1acebd9eaf8d63b3623dac869283b34894">public void resetBusinessCasesFrequencyCapping()</a></td>
	</tr>
	<tr>
		<td><a href="#1a7a35318e8fbceca7956ec64f16e1e6c4">public void reloadInApps()</a></td>
	</tr>
	<tr>
		<td><a href="#1a3e8a453502386f0962ea0ba977624fff">public void reloadInApps(Callback&lt;Boolean, ReloadInAppsException&gt; callback)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a967a88de0a2694725d245cb815cac3ba"></a>public static <a href="#heading">InAppManager</a> getInstance()  
<strong>Returns</strong> InAppManager shared instance. 

----------  
  

#### <a name="1a5ade44e97fd373900a99145fd37d19b1"></a>public void postEvent(@NonNull String event)  
postEvent(String, TagsBundle, Callback)

----------  
  

#### <a name="1a1a8a9722e5e904191c7ce5ae58c29c8a"></a>public void postEvent(@NonNull String event, <a href="../tags/TagsBundle.md">TagsBundle</a> attributes, boolean isInternal)  
postEvent(String, TagsBundle, Callback)

----------  
  

#### <a name="1ad74b43d9963f9d858160da6486268292"></a>public void postEvent(@NonNull String event, @Nullable <a href="../tags/TagsBundle.md">TagsBundle</a> attributes, <a href="../function/Callback.md">Callback</a>&lt;Void, PostEventException&gt; callback)  
Post events for In-App Messages. This can trigger In-App message HTML as specified in Pushwoosh Control Panel.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>event</strong></td>
		<td>name of the event </td>
	</tr>
	<tr>
		<td><strong>attributes</strong></td>
		<td>additional event attributes </td>
	</tr>
	<tr>
		<td><strong>callback</strong></td>
		<td>method completion callback </td>
	</tr>
</table>


----------  
  

#### <a name="1a6b671e483ce5a251147cd4eb7397d09a"></a>public void addJavascriptInterface(@NonNull Object object, @NonNull String name)  
Add JavaScript interface for In-Apps extension. All exported methods should be marked with @JavascriptInterface annotation.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>object</strong></td>
		<td>java object that will be available inside In-App page </td>
	</tr>
	<tr>
		<td><strong>name</strong></td>
		<td>specified object will be available as window.name</td>
	</tr>
</table>


----------  
  

#### <a name="1acf42a9dd72e6f8db1aee936a83bfc7b2"></a>public void removeJavascriptInterface(@NonNull String name)  
Removes object registered with addJavascriptInterface(Object, String)<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>name</strong></td>
		<td>object name </td>
	</tr>
</table>


----------  
  

#### <a name="1aea84ded35a2122ff7b1a742d60662a6c"></a>public void registerJavascriptInterface(@NonNull String className, @NonNull String name)  
Same as addJavascriptInterface(Object, String) but uses class name instead of object 

----------  
  

#### <a name="1acebd9eaf8d63b3623dac869283b34894"></a>public void resetBusinessCasesFrequencyCapping()  


----------  
  

#### <a name="1a7a35318e8fbceca7956ec64f16e1e6c4"></a>public void reloadInApps()  


----------  
  

#### <a name="1a3e8a453502386f0962ea0ba977624fff"></a>public void reloadInApps(<a href="../function/Callback.md">Callback</a>&lt;Boolean, ReloadInAppsException&gt; callback)  
