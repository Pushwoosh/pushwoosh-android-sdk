
#### module: pushwoosh-inbox  

#### package: com.pushwoosh.inbox.data  

# <a name="heading"></a>interface InboxMessage  

## Members  

<table>
	<tr>
		<td><a href="#1a3c4c237e75a65d77daf0abe9a2d2c75b">public String getCode()</a></td>
	</tr>
	<tr>
		<td><a href="#1a860d84fd1e273b09d23d80ccba579753">public String getTitle()</a></td>
	</tr>
	<tr>
		<td><a href="#1a519d716904922c6cf43cd18845cf5438">public String getImageUrl()</a></td>
	</tr>
	<tr>
		<td><a href="#1a0de7439d51a31d500ce5ac089cc5ae7d">public String getMessage()</a></td>
	</tr>
	<tr>
		<td><a href="#1a4a7088640e67c69f05fd0e31823a4972">public Date getSendDate()</a></td>
	</tr>
	<tr>
		<td><a href="#1a0b27893fa0fa49e9494ef2120faaf5d1">public InboxMessageType getType()</a></td>
	</tr>
	<tr>
		<td><a href="#1a43268afb78e5bed111c4c4cabce53899">public String getBannerUrl()</a></td>
	</tr>
	<tr>
		<td><a href="#1a8d122b60de3016560a0cffc41b07676d">public boolean isRead()</a></td>
	</tr>
	<tr>
		<td><a href="#1afcc43711c3f1fe3fc9241bf83c3850c9">public boolean isActionPerformed()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a3c4c237e75a65d77daf0abe9a2d2c75b"></a>public String getCode()  


----------  
  

#### <a name="1a860d84fd1e273b09d23d80ccba579753"></a>public String getTitle()  


----------  
  

#### <a name="1a519d716904922c6cf43cd18845cf5438"></a>public String getImageUrl()  


----------  
  

#### <a name="1a0de7439d51a31d500ce5ac089cc5ae7d"></a>public String getMessage()  


----------  
  

#### <a name="1a4a7088640e67c69f05fd0e31823a4972"></a>public Date getSendDate()  


----------  
  

#### <a name="1a0b27893fa0fa49e9494ef2120faaf5d1"></a>public <a href="InboxMessageType.md">InboxMessageType</a> getType()  


----------  
  

#### <a name="1a43268afb78e5bed111c4c4cabce53899"></a>public String getBannerUrl()  


----------  
  

#### <a name="1a8d122b60de3016560a0cffc41b07676d"></a>public boolean isRead()  
Inbox Message which is read, see com.pushwoosh.inbox.PushwooshInbox#readMessage(String)<br/><br/><strong>Returns</strong> true if read otherwise false 

----------  
  

#### <a name="1afcc43711c3f1fe3fc9241bf83c3850c9"></a>public boolean isActionPerformed()  
Action of the Inbox Message is performed com.pushwoosh.inbox.PushwooshInbox#performAction(String) or an action was performed on the push tap ) <br/><br/><strong>Returns</strong> true if an action was performed in the Inbox 