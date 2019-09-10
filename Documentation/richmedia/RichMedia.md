
#### module: pushwoosh  

#### package: com.pushwoosh.richmedia  

# <a name="heading"></a>class RichMedia  
RichMedia class represents Rich Media page object. 
## Members  

<table>
	<tr>
		<td><a href="#1a7c907edc0fa00d21aed37d535996c546">public int hashCode()</a></td>
	</tr>
	<tr>
		<td><a href="#1a2b6548fb77aabab5cc5bb94a737c5e65">public Source getSource()</a></td>
	</tr>
	<tr>
		<td><a href="#1a4217083564813de6c72a87c160dd67cf">public String getContent()</a></td>
	</tr>
	<tr>
		<td><a href="#1abe3ea7c2e8c41dc37e57b3333154ef2c">public boolean isLockScreen()</a></td>
	</tr>
	<tr>
		<td><a href="#1a26e38e552e05083398c64f3ef5c16c02">public boolean isRequired()</a></td>
	</tr>
	<tr>
		<td><a href="#1a9757ef33b721f92f0e7edd18e1639c8b">public String toString()</a></td>
	</tr>
	<tr>
		<td><a href="#1af8882b4d6067bd015a9bcdfae9f2ccf1">public boolean equals(Object o)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a7c907edc0fa00d21aed37d535996c546"></a>public int hashCode()  


----------  
  

#### <a name="1a2b6548fb77aabab5cc5bb94a737c5e65"></a>public Source getSource()  
Rich Media presenter type. 

----------  
  

#### <a name="1a4217083564813de6c72a87c160dd67cf"></a>public String getContent()  
Content of the Rich Media. For InAppSource it's equal to In-App code, for PushMessageSource it's equal to Rich Media code. 

----------  
  

#### <a name="1abe3ea7c2e8c41dc37e57b3333154ef2c"></a>public boolean isLockScreen()  
Check if the Rich Media will show on a lock screen. 

----------  
  

#### <a name="1a26e38e552e05083398c64f3ef5c16c02"></a>public boolean isRequired()  
Checks if InAppSource is a required In-App. Always returns true for PWRichMediaSourcePush. 

----------  
  

#### <a name="1a9757ef33b721f92f0e7edd18e1639c8b"></a>public String toString()  


----------  
  

#### <a name="1af8882b4d6067bd015a9bcdfae9f2ccf1"></a>public boolean equals(Object o)  
