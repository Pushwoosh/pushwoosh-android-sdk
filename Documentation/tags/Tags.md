
#### module: pushwoosh  

#### package: com.pushwoosh.tags  

# <a name="heading"></a>class Tags  
Static utility class for tags creation 
## Members  

<table>
	<tr>
		<td><a href="#1a88e97275c9405b4b86688d463e03655e">public static TagsBundle intTag(String key, int value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a536656d2528fd99bf60f4b0966fe7a0f">public static TagsBundle removeTag(String key)</a></td>
	</tr>
	<tr>
		<td><a href="#1a9627acfbab6f0023445b8f123b73ae14">public static TagsBundle booleanTag(String key, boolean value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a2b210291b5e7571e0affafd0c4a50db7">public static TagsBundle stringTag(String key, String value)</a></td>
	</tr>
	<tr>
		<td><a href="#1acb912a6db47532fd49d8e63de839ae5f">public static TagsBundle listTag(String key, List&lt;String&gt; value)</a></td>
	</tr>
	<tr>
		<td><a href="#1adba5f31f59273b8d792b611156d0db35">public static TagsBundle dateTag(String key, Date value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a96b12060853f6789d9430a7eac9fea1d">public static TagsBundle longTag(String key, long value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a9054d5661c27310ca0552aed35ef8fae">public static TagsBundle fromJson(JSONObject json)</a></td>
	</tr>
	<tr>
		<td><a href="#1af0df6b2c0a6375e7c63835c5c61c971f">public static TagsBundle incrementInt(String key, int delta)</a></td>
	</tr>
	<tr>
		<td><a href="#1a6a90971b224f31f94bf40790b30f1ae7">public static TagsBundle appendList(String key, List&lt;String&gt; list)</a></td>
	</tr>
	<tr>
		<td><a href="#1a483f7762842c02d8b4eb54fc52ee53fd">public static TagsBundle empty()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a88e97275c9405b4b86688d463e03655e"></a>public static <a href="TagsBundle.md">TagsBundle</a> intTag(String key, int value)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>value</strong></td>
		<td>tag value </td>
	</tr>
</table>
<strong>Returns</strong> TagsBundle with single tag of int type 

----------  
  

#### <a name="1a536656d2528fd99bf60f4b0966fe7a0f"></a>public static <a href="TagsBundle.md">TagsBundle</a> removeTag(String key)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
</table>
<strong>Returns</strong> TagsBundle for tag removal 

----------  
  

#### <a name="1a9627acfbab6f0023445b8f123b73ae14"></a>public static <a href="TagsBundle.md">TagsBundle</a> booleanTag(String key, boolean value)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>value</strong></td>
		<td>tag value </td>
	</tr>
</table>
<strong>Returns</strong> TagsBundle with single tag of boolean type 

----------  
  

#### <a name="1a2b210291b5e7571e0affafd0c4a50db7"></a>public static <a href="TagsBundle.md">TagsBundle</a> stringTag(String key, String value)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>value</strong></td>
		<td>tag value </td>
	</tr>
</table>
<strong>Returns</strong> TagsBundle with single tag of string type 

----------  
  

#### <a name="1acb912a6db47532fd49d8e63de839ae5f"></a>public static <a href="TagsBundle.md">TagsBundle</a> listTag(String key, List&lt;String&gt; value)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>value</strong></td>
		<td>tag value </td>
	</tr>
</table>
<strong>Returns</strong> TagsBundle with single tag of list type 

----------  
  

#### <a name="1adba5f31f59273b8d792b611156d0db35"></a>public static <a href="TagsBundle.md">TagsBundle</a> dateTag(String key, Date value)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>value</strong></td>
		<td>tag value </td>
	</tr>
</table>
<strong>Returns</strong> TagsBundle with single tag of Date type 

----------  
  

#### <a name="1a96b12060853f6789d9430a7eac9fea1d"></a>public static <a href="TagsBundle.md">TagsBundle</a> longTag(String key, long value)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>value</strong></td>
		<td>tag value </td>
	</tr>
</table>
<strong>Returns</strong> TagsBundle with single tag of long type 

----------  
  

#### <a name="1a9054d5661c27310ca0552aed35ef8fae"></a>public static <a href="TagsBundle.md">TagsBundle</a> fromJson(JSONObject json)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>json</strong></td>
		<td>json object with tag name-value pairs </td>
	</tr>
</table>
<strong>Returns</strong> converted tags 

----------  
  

#### <a name="1af0df6b2c0a6375e7c63835c5c61c971f"></a>public static <a href="TagsBundle.md">TagsBundle</a> incrementInt(String key, int delta)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>delta</strong></td>
		<td>incremental value </td>
	</tr>
</table>
<strong>Returns</strong> TagsBundle for tag increment operation 

----------  
  

#### <a name="1a6a90971b224f31f94bf40790b30f1ae7"></a>public static <a href="TagsBundle.md">TagsBundle</a> appendList(String key, List&lt;String&gt; list)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>list</strong></td>
		<td>append value </td>
	</tr>
</table>
<strong>Returns</strong> TagsBundle for list tag append operation 

----------  
  

#### <a name="1a483f7762842c02d8b4eb54fc52ee53fd"></a>public static <a href="TagsBundle.md">TagsBundle</a> empty()  
<strong>Returns</strong> empty TagsBundle 