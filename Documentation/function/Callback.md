
#### module: pushwoosh  

#### package: com.pushwoosh.function  

# <a name="heading"></a>interface Callback  
Asynchronous operation handler<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>&lt;T&gt;</strong></td>
		<td>Data class </td>
	</tr>
	<tr>
		<td><strong>&lt;E&gt;</strong></td>
		<td>Exception class </td>
	</tr>
</table>

## Members  

<table>
	<tr>
		<td><a href="#1a2075f19d59d1779050f5e9112f88205b">public void process(@NonNull Result&lt;T, E&gt; result)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a2075f19d59d1779050f5e9112f88205b"></a>public void process(@NonNull <a href="Result.md">Result</a>&lt;T, E&gt; result)  
Asynchronous operation handler method. Method is called on main thread.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>result</strong></td>
		<td>Asynchronous operation result </td>
	</tr>
</table>
