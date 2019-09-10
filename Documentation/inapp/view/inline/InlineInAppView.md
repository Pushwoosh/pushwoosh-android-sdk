
#### module: pushwoosh  

#### package: com.pushwoosh.inapp.view.inline  

# <a name="heading"></a>class InlineInAppView  

## Members  

<table>
	<tr>
		<td><a href="#1a8a9904a409eff8a84cb395290a12f479">public  InlineInAppView(@NonNull Context context)</a></td>
	</tr>
	<tr>
		<td><a href="#1a48be19995be7037c63ef5760b1e747cb">public  InlineInAppView(@NonNull Context context, @Nullable AttributeSet attrs)</a></td>
	</tr>
	<tr>
		<td><a href="#1a8a81f45f8bc61871069063c25244d101">public String getIdentifier()</a></td>
	</tr>
	<tr>
		<td><a href="#1a0fd5a40f27b5ff31e724f466dad38141">public void setIdentifier(String identifier)</a></td>
	</tr>
	<tr>
		<td><a href="#1a0b30d2aa09139ead92a04ac0b9fee7a4">public boolean isLayoutAnimationDisabled()</a></td>
	</tr>
	<tr>
		<td><a href="#1aa576b749bb6546a719aa4e64d9a12cad">public void setDisableLayoutAnimation(boolean disableLayoutAnimation)</a></td>
	</tr>
	<tr>
		<td><a href="#1a1a5653cadfa64ce3b9be0a90e2905878">public void addInlineInAppViewListener(InlineInAppViewListener listener)</a></td>
	</tr>
	<tr>
		<td><a href="#1a9c2737d7af5b97cf4339e1fb5903a98d">public void removeInlineInAppViewListener(InlineInAppViewListener listener)</a></td>
	</tr>
	<tr>
		<td><a href="#1a03a6449b5b4db5199657034a6396fe37">protected void onSizeChanged(int w, int h, int oldw, int oldh)</a></td>
	</tr>
	<tr>
		<td><a href="#1af13441c05bd761270ec4eda3dc2a3a33">protected WebView createWebView()</a></td>
	</tr>
	<tr>
		<td><a href="#1a4032809cb4608f8ebeac027331e098f8">protected void initWebView()</a></td>
	</tr>
	<tr>
		<td><a href="#1aab72b94da295e9c19d703094ce21463b">protected void animateOpen()</a></td>
	</tr>
	<tr>
		<td><a href="#1a6e7cad3b1c26c2f2de6b5cd5db7691bd">protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)</a></td>
	</tr>
	<tr>
		<td><a href="#1abf42a447a9738407fa16317d8bee5eab">protected LayoutParams createWebViewParams(InAppLayout mode, int topMargin)</a></td>
	</tr>
	<tr>
		<td><a href="#1a94664cbd8c1cfb4e1349f9555eeaa95d">protected void onConfigurationChanged(Configuration newConfig)</a></td>
	</tr>
	<tr>
		<td><a href="#1af4c2b10c76e8418dda443c456254e7aa">protected Parcelable onSaveInstanceState()</a></td>
	</tr>
	<tr>
		<td><a href="#1a7bdb7d291c8bb4b55410c3fd5be0c0dd">protected void onRestoreInstanceState(Parcelable state)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a8a9904a409eff8a84cb395290a12f479"></a>public  InlineInAppView(@NonNull Context context)  


----------  
  

#### <a name="1a48be19995be7037c63ef5760b1e747cb"></a>public  InlineInAppView(@NonNull Context context, @Nullable AttributeSet attrs)  


----------  
  

#### <a name="1a8a81f45f8bc61871069063c25244d101"></a>public String getIdentifier()  


----------  
  

#### <a name="1a0fd5a40f27b5ff31e724f466dad38141"></a>public void setIdentifier(String identifier)  


----------  
  

#### <a name="1a0b30d2aa09139ead92a04ac0b9fee7a4"></a>public boolean isLayoutAnimationDisabled()  


----------  
  

#### <a name="1aa576b749bb6546a719aa4e64d9a12cad"></a>public void setDisableLayoutAnimation(boolean disableLayoutAnimation)  
Disable layout animation<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>disableLayoutAnimation</strong></td>
		<td>flag </td>
	</tr>
</table>


----------  
  

#### <a name="1a1a5653cadfa64ce3b9be0a90e2905878"></a>public void addInlineInAppViewListener(InlineInAppViewListener listener)  
Add a listener that will be called when state or bounds of the view change.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>listener</strong></td>
		<td>The listener for state and bounds change. </td>
	</tr>
</table>


----------  
  

#### <a name="1a9c2737d7af5b97cf4339e1fb5903a98d"></a>public void removeInlineInAppViewListener(InlineInAppViewListener listener)  
Remove a listener for state or bounds changes.<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>listener</strong></td>
		<td>The listener for state and bounds change. </td>
	</tr>
</table>


----------  
  

#### <a name="1a03a6449b5b4db5199657034a6396fe37"></a>protected void onSizeChanged(int w, int h, int oldw, int oldh)  


----------  
  

#### <a name="1af13441c05bd761270ec4eda3dc2a3a33"></a>protected WebView createWebView()  


----------  
  

#### <a name="1a4032809cb4608f8ebeac027331e098f8"></a>protected void initWebView()  


----------  
  

#### <a name="1aab72b94da295e9c19d703094ce21463b"></a>protected void animateOpen()  


----------  
  

#### <a name="1a6e7cad3b1c26c2f2de6b5cd5db7691bd"></a>protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)  


----------  
  

#### <a name="1abf42a447a9738407fa16317d8bee5eab"></a>protected LayoutParams createWebViewParams(InAppLayout mode, int topMargin)  


----------  
  

#### <a name="1a94664cbd8c1cfb4e1349f9555eeaa95d"></a>protected void onConfigurationChanged(Configuration newConfig)  


----------  
  

#### <a name="1af4c2b10c76e8418dda443c456254e7aa"></a>protected Parcelable onSaveInstanceState()  


----------  
  

#### <a name="1a7bdb7d291c8bb4b55410c3fd5be0c0dd"></a>protected void onRestoreInstanceState(Parcelable state)  
