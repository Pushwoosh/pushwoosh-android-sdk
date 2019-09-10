
#### module: pushwoosh  

#### package: com.pushwoosh.richmedia  

# <a name="heading"></a>interface RichMediaPresentingDelegate  
Interface for Rich Media presentation managing. 
## Members  

<table>
	<tr>
		<td><a href="#1aab4098d7ae6967adfb002013fb33c9e0">public boolean shouldPresent(RichMedia richMedia)</a></td>
	</tr>
	<tr>
		<td><a href="#1ab71ead1840c78ea96db7f880e3ccb2dd">public void onPresent(RichMedia richMedia)</a></td>
	</tr>
	<tr>
		<td><a href="#1ab92d2eafa28829325e13766e09eb5cf7">public void onError(RichMedia richMedia, PushwooshException pushwooshException)</a></td>
	</tr>
	<tr>
		<td><a href="#1a2da91c2d6e0cd6f8ebe380608297ab2d">public void onClose(RichMedia richMedia)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1aab4098d7ae6967adfb002013fb33c9e0"></a>public boolean shouldPresent(<a href="RichMedia.md">RichMedia</a> richMedia)  
Checks the delegate whether the Rich Media should be displayed. 

----------  
  

#### <a name="1ab71ead1840c78ea96db7f880e3ccb2dd"></a>public void onPresent(<a href="RichMedia.md">RichMedia</a> richMedia)  
Tells the delegate that Rich Media has been displayed. 

----------  
  

#### <a name="1ab92d2eafa28829325e13766e09eb5cf7"></a>public void onError(<a href="RichMedia.md">RichMedia</a> richMedia, PushwooshException pushwooshException)  
Tells the delegate that error during Rich Media presenting has been occured. 

----------  
  

#### <a name="1a2da91c2d6e0cd6f8ebe380608297ab2d"></a>public void onClose(<a href="RichMedia.md">RichMedia</a> richMedia)  
Tells the delegate that Rich Media has been closed. 