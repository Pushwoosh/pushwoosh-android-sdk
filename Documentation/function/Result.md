
#### module: pushwoosh  

#### package: com.pushwoosh.function  

# <a name="heading"></a>class Result  
Result class encapsulates result of an asynchronous operation<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>&lt;T&gt;</strong></td>
		<td>Data Class </td>
	</tr>
	<tr>
		<td><strong>&lt;E&gt;</strong></td>
		<td>Exception Class </td>
	</tr>
</table>

## Members  

<table>
	<tr>
		<td><a href="#1ac166279618ed86ca804154e4fa013e55">public static static&lt;T, E extends PushwooshException&gt; Result&lt;T, E&gt; fromData(T data)</a></td>
	</tr>
	<tr>
		<td><a href="#1a4093efdf50a12770aa23594811f027e0">public static static&lt;T, E extends PushwooshException&gt; Result&lt;T, E&gt; fromException(E exception)</a></td>
	</tr>
	<tr>
		<td><a href="#1aa9ae53a27f357b67e69aa46e58a55c6f">public static static&lt;T, E extends PushwooshException&gt; Result&lt;T, E&gt; from(T data, E exception)</a></td>
	</tr>
	<tr>
		<td><a href="#1a47ff5ec4dbbb6077efc002eafffe4d28">public boolean isSuccess()</a></td>
	</tr>
	<tr>
		<td><a href="#1a5af431bda21751c568ca225e446d44a1">public T getData()</a></td>
	</tr>
	<tr>
		<td><a href="#1a800e3d195f6145c12a9731bd8847a0e6">public E getException()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1ac166279618ed86ca804154e4fa013e55"></a>public static static&lt;T, E extends PushwooshException&gt; <a href="#heading">Result</a>&lt;T, E&gt; fromData(T data)  
Factory method that constructs successful result with given data<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>data</strong></td>
		<td>result data </td>
	</tr>
	<tr>
		<td><strong>&lt;T&gt;</strong></td>
		<td>result data class </td>
	</tr>
	<tr>
		<td><strong>&lt;E&gt;</strong></td>
		<td>result exception class </td>
	</tr>
</table>
<strong>Returns</strong> result for given data 

----------  
  

#### <a name="1a4093efdf50a12770aa23594811f027e0"></a>public static static&lt;T, E extends PushwooshException&gt; <a href="#heading">Result</a>&lt;T, E&gt; fromException(E exception)  
Factory method that constructs unsuccessful result with given exception<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>exception</strong></td>
		<td>result exception </td>
	</tr>
	<tr>
		<td><strong>&lt;T&gt;</strong></td>
		<td>result data class </td>
	</tr>
	<tr>
		<td><strong>&lt;E&gt;</strong></td>
		<td>result exception class </td>
	</tr>
</table>
<strong>Returns</strong> result for given exception 

----------  
  

#### <a name="1aa9ae53a27f357b67e69aa46e58a55c6f"></a>public static static&lt;T, E extends PushwooshException&gt; <a href="#heading">Result</a>&lt;T, E&gt; from(T data, E exception)  


----------  
  

#### <a name="1a47ff5ec4dbbb6077efc002eafffe4d28"></a>public boolean isSuccess()  
<strong>Returns</strong> true if operation was successful 

----------  
  

#### <a name="1a5af431bda21751c568ca225e446d44a1"></a>public T getData()  
<strong>Returns</strong> result data 

----------  
  

#### <a name="1a800e3d195f6145c12a9731bd8847a0e6"></a>public E getException()  
<strong>Returns</strong> result exception 