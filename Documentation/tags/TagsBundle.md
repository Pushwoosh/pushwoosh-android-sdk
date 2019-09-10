
#### module: pushwoosh  

#### package: com.pushwoosh.tags  

# <a name="heading"></a>class TagsBundle  
Immutable collection of tags specific for current device. Tags are used to target different audience selectively when sending push notification.<br/><em>See also:</em> <a href="http://docs.pushwoosh.com/docs/segmentation-tags-and-filters">Segmentation guide</a>
## Members  

<table>
	<tr>
		<td><a href="#1a504df6f6599351605f878527643bcde1">public int getInt(String key, int defaultValue)</a></td>
	</tr>
	<tr>
		<td><a href="#1a996bef4cbf30f47bc5b03a03ebcb2d65">public long getLong(String key, long defaultValue)</a></td>
	</tr>
	<tr>
		<td><a href="#1a2d2cb76509cac3bd57456093bee18290">public boolean getBoolean(String key, boolean defaultValue)</a></td>
	</tr>
	<tr>
		<td><a href="#1a24266ff593607ba19d86e1f256105d74">public String getString(String key)</a></td>
	</tr>
	<tr>
		<td><a href="#1afca4f1bc061e76d198800028fd069843">public List&lt;String&gt; getList(String key)</a></td>
	</tr>
	<tr>
		<td><a href="#1a7cdc1982cb6ed0c11d7d2fe45ee23dfa">public JSONObject toJson()</a></td>
	</tr>
	<tr>
		<td><a href="#1acf9256f4dd7cbed6a277ceafe52f54cf">public Map&lt;String, Object&gt; getMap()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a504df6f6599351605f878527643bcde1"></a>public int getInt(String key, int defaultValue)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>defaultValue</strong></td>
		<td>default tag value </td>
	</tr>
</table>
<strong>Returns</strong> tag value for given name or defaultValue if tag with given name does not exist 

----------  
  

#### <a name="1a996bef4cbf30f47bc5b03a03ebcb2d65"></a>public long getLong(String key, long defaultValue)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>defaultValue</strong></td>
		<td>default tag value </td>
	</tr>
</table>
<strong>Returns</strong> tag value for given name or defaultValue if tag with given name does not exist 

----------  
  

#### <a name="1a2d2cb76509cac3bd57456093bee18290"></a>public boolean getBoolean(String key, boolean defaultValue)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>defaultValue</strong></td>
		<td>default tag value </td>
	</tr>
</table>
<strong>Returns</strong> tag value for given name or defaultValue if tag with given name does not exist 

----------  
  

#### <a name="1a24266ff593607ba19d86e1f256105d74"></a>public String getString(String key)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
</table>
<strong>Returns</strong> tag value for given name or null if tag with given name does not exist 

----------  
  

#### <a name="1afca4f1bc061e76d198800028fd069843"></a>public List&lt;String&gt; getList(String key)  
<strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
</table>
<strong>Returns</strong> tag value for given name or null if tag with given name does not exist 

----------  
  

#### <a name="1a7cdc1982cb6ed0c11d7d2fe45ee23dfa"></a>public JSONObject toJson()  
<strong>Returns</strong> JSON representation of TagsBundle 

----------  
  

#### <a name="1acf9256f4dd7cbed6a277ceafe52f54cf"></a>public Map&lt;String, Object&gt; getMap()  
