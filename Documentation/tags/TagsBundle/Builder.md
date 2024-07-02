
#### module: pushwoosh  

#### package: com.pushwoosh.tags.TagsBundle  

# <a name="heading"></a>class Builder  
TagsBundle.Builder class is used to generate TagsBundle instances 
## Members  

<table>
	<tr>
		<td><a href="#1aa00c3f9ad2425e44d8ef88e42fd9d9ba">public Builder putInt(String key, int value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a5254c57e22b2fe55b798ffab79733b96">public Builder putLong(String key, long value)</a></td>
	</tr>
	<tr>
		<td><a href="#1ac0293b81a6778f8ef489ee1b3d76b037">public Builder incrementInt(String key, int value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a8084882f94e465edcacdfb2ae1b9aa86">public Builder appendList(String key, List&lt;String&gt; value)</a></td>
	</tr>
	<tr>
		<td><a href="#1acea248fd755c0005eb6fc1802aaf62e9">public Builder removeFromList(String key, List&lt;String&gt; value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a62a3c5400f4f29d1c2a997edfc84d2ee">public Builder putBoolean(String key, boolean value)</a></td>
	</tr>
	<tr>
		<td><a href="#1ad9c2e49ace489e21095f89798f0b6953">public Builder putString(String key, String value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a7ab742ce5288a46eb57421af413f8eac">public Builder putStringIfNotEmpty(String key, String value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a9d4e59c010acab66869f7f7fbf9c7614">public Builder putList(String key, List&lt;String&gt; value)</a></td>
	</tr>
	<tr>
		<td><a href="#1a2184427e2068d50756eda40ad93288a6">public Builder putDate(String key, Date value)</a></td>
	</tr>
	<tr>
		<td><a href="#1af188ba59adfcd3713c7c5c9a1c14f0a3">public Builder remove(String key)</a></td>
	</tr>
	<tr>
		<td><a href="#1ae7ebcd530f35d85b85142dc64912dee4">public Builder putAll(JSONObject json)</a></td>
	</tr>
	<tr>
		<td><a href="#1a5d2f6e6ebf37e6b7fb642f6b8354546e">public TagsBundle build()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1aa00c3f9ad2425e44d8ef88e42fd9d9ba"></a>public <a href="#heading">Builder</a> putInt(String key, int value)  
Adds tag with integer value<br/><br/><br/><strong>Parameters</strong><br/>
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
<strong>Returns</strong> builder 

----------  
  

#### <a name="1a5254c57e22b2fe55b798ffab79733b96"></a>public <a href="#heading">Builder</a> putLong(String key, long value)  
Adds tag with long value<br/><br/><br/><strong>Parameters</strong><br/>
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
<strong>Returns</strong> builder 

----------  
  

#### <a name="1ac0293b81a6778f8ef489ee1b3d76b037"></a>public <a href="#heading">Builder</a> incrementInt(String key, int value)  
Adds increment operation for given tag<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>value</strong></td>
		<td>incremental value </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1a8084882f94e465edcacdfb2ae1b9aa86"></a>public <a href="#heading">Builder</a> appendList(String key, List&lt;String&gt; value)  
Adds append operation for given list tag<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>value</strong></td>
		<td>list to append </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1acea248fd755c0005eb6fc1802aaf62e9"></a>public <a href="#heading">Builder</a> removeFromList(String key, List&lt;String&gt; value)  
Adds remove operation for given list tag<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
	<tr>
		<td><strong>value</strong></td>
		<td>list to remove </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1a62a3c5400f4f29d1c2a997edfc84d2ee"></a>public <a href="#heading">Builder</a> putBoolean(String key, boolean value)  
Adds tag with boolean value<br/><br/><br/><strong>Parameters</strong><br/>
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
<strong>Returns</strong> builder 

----------  
  

#### <a name="1ad9c2e49ace489e21095f89798f0b6953"></a>public <a href="#heading">Builder</a> putString(String key, String value)  
Adds tag with string value<br/><br/><br/><strong>Parameters</strong><br/>
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
<strong>Returns</strong> builder 

----------  
  

#### <a name="1a7ab742ce5288a46eb57421af413f8eac"></a>public <a href="#heading">Builder</a> putStringIfNotEmpty(String key, String value)  


----------  
  

#### <a name="1a9d4e59c010acab66869f7f7fbf9c7614"></a>public <a href="#heading">Builder</a> putList(String key, List&lt;String&gt; value)  
Adds tag with list value<br/><br/><br/><strong>Parameters</strong><br/>
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
<strong>Returns</strong> builder 

----------  
  

#### <a name="1a2184427e2068d50756eda40ad93288a6"></a>public <a href="#heading">Builder</a> putDate(String key, Date value)  
Adds tag with date value<br/><br/><br/><strong>Parameters</strong><br/>
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
<strong>Returns</strong> builder 

----------  
  

#### <a name="1af188ba59adfcd3713c7c5c9a1c14f0a3"></a>public <a href="#heading">Builder</a> remove(String key)  
Removes tag<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>key</strong></td>
		<td>tag name </td>
	</tr>
</table>
<strong>Returns</strong> builder 

----------  
  

#### <a name="1ae7ebcd530f35d85b85142dc64912dee4"></a>public <a href="#heading">Builder</a> putAll(JSONObject json)  
Adds all tags from key-value pairs of given json<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>json</strong></td>
		<td>json object with tag name-value pairs </td>
	</tr>
</table>
<strong>Returns</strong> 

----------  
  

#### <a name="1a5d2f6e6ebf37e6b7fb642f6b8354546e"></a>public <a href="../TagsBundle.md">TagsBundle</a> build()  
Builds and returns TagsBundle.<br/><br/><br/><strong>Returns</strong> TagsBundle 