
#### module: pushwoosh-inbox  

#### package: com.pushwoosh.inbox  

# <a name="heading"></a>class PushwooshInbox  

## Members  

<table>
	<tr>
		<td><a href="#1a64fb80bfe49d813feb3abb76a6e119c3">public static void messagesWithNoActionPerformedCount(Callback&lt;Integer, InboxMessagesException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a47dc3c5b870dbe2ccc921e31028ac2b5">public static void messagesCount(Callback&lt;Integer, InboxMessagesException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1abc6b8796b44ecca009aaad82ef923c3e">public static void loadMessages(Callback&lt;Collection&lt;InboxMessage&gt;, InboxMessagesException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1a993a82e20859a262d18ee07e1c3034e1">public static Collection&lt;InboxMessage&gt; loadCachedMessages(@Nullable InboxMessage inboxMessage, int limit)</a></td>
	</tr>
	<tr>
		<td><a href="#1a5d973456e4dfd68ad8b7ef79ae9a9f09">public static void loadCachedMessages(Callback&lt;Collection&lt;InboxMessage&gt;, InboxMessagesException&gt; callback, @Nullable InboxMessage inboxMessage, int limit)</a></td>
	</tr>
	<tr>
		<td><a href="#1afa1172501a30a412126134237ab7e9ce">public static void unreadMessagesCount(Callback&lt;Integer, InboxMessagesException&gt; callback)</a></td>
	</tr>
	<tr>
		<td><a href="#1ae23065c20a803b904da72bf7032b2d79">public static void readMessage(String code)</a></td>
	</tr>
	<tr>
		<td><a href="#1a847a09d7defca6305d1e126c38bf3c78">public static void readMessages(Collection&lt;String&gt; codes)</a></td>
	</tr>
	<tr>
		<td><a href="#1a325e52c29534d4e43ffbfedbc227f82f">public static void performAction(String code)</a></td>
	</tr>
	<tr>
		<td><a href="#1a9a48658faa71c5c77abd7dbd985ded5a">public static void deleteMessage(String code)</a></td>
	</tr>
	<tr>
		<td><a href="#1a206f4c3551edcd10019685e94843a3f7">public static void deleteMessages(Collection&lt;String&gt; codes)</a></td>
	</tr>
	<tr>
		<td><a href="#1a49ebfb1cafbc65ce5912606ec20295d9">public static void loadMessages(Callback&lt;Collection&lt;InboxMessage&gt;, InboxMessagesException&gt; callback, @Nullable InboxMessage inboxMessage, int limit)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1a64fb80bfe49d813feb3abb76a6e119c3"></a>public static void messagesWithNoActionPerformedCount(Callback&lt;Integer, InboxMessagesException&gt; callback)  
Get the number of the com.pushwoosh.inbox.data.InboxMessage with no action performed<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>- if successful, return the number of the InboxMessages with no action performed. Otherwise, return error </td>
	</tr>
</table>


----------  
  

#### <a name="1a47dc3c5b870dbe2ccc921e31028ac2b5"></a>public static void messagesCount(Callback&lt;Integer, InboxMessagesException&gt; callback)  
Get the total number of the com.pushwoosh.inbox.data.InboxMessage<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>- if successful, return the total number of the InboxMessages. Otherwise, return error </td>
	</tr>
</table>


----------  
  

#### <a name="1abc6b8796b44ecca009aaad82ef923c3e"></a>public static void loadMessages(Callback&lt;Collection&lt;InboxMessage&gt;, InboxMessagesException&gt; callback)  
Get the collection of the com.pushwoosh.inbox.data.InboxMessage that the user received This method obtains messages from network. In case the network connection is not available messages will be obtained from local database <br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>- if successful, return the collection of the InboxMessages. Otherwise, return error </td>
	</tr>
</table>


----------  
  

#### <a name="1a993a82e20859a262d18ee07e1c3034e1"></a>public static Collection&lt;InboxMessage&gt; loadCachedMessages(@Nullable InboxMessage inboxMessage, int limit)  
Get the collection of the com.pushwoosh.inbox.data.InboxMessage that the user received. This method obtains messages synchronously from local database<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>inboxMessage</strong></td>
		<td>- This parameter provides pagination. Pass the last com.pushwoosh.inbox.data.InboxMessage that is on your current page as a parameter to get previous messages. To get the latest messages or in case the pagination is not necessary, pass null as a parameter. </td>
	</tr>
	<tr>
		<td><strong>limit</strong></td>
		<td>- amount of messages to get. Pass -1 to get all the messages </td>
	</tr>
</table>


----------  
  

#### <a name="1a5d973456e4dfd68ad8b7ef79ae9a9f09"></a>public static void loadCachedMessages(Callback&lt;Collection&lt;InboxMessage&gt;, InboxMessagesException&gt; callback, @Nullable InboxMessage inboxMessage, int limit)  
Get the collection of the com.pushwoosh.inbox.data.InboxMessage that the user received. This method obtains messages asynchronously from local database<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>- if successful, return the collection of the InboxMessages. Otherwise, return error </td>
	</tr>
	<tr>
		<td><strong>inboxMessage</strong></td>
		<td>- This parameter provides pagination. Pass the last com.pushwoosh.inbox.data.InboxMessage that is on your current page as a parameter to get previous messages. To get the latest messages or in case the pagination is not necessary, pass null as a parameter. </td>
	</tr>
	<tr>
		<td><strong>limit</strong></td>
		<td>- amount of messages to get. Pass -1 to get all the messages </td>
	</tr>
</table>


----------  
  

#### <a name="1afa1172501a30a412126134237ab7e9ce"></a>public static void unreadMessagesCount(Callback&lt;Integer, InboxMessagesException&gt; callback)  
Get the number of the unread com.pushwoosh.inbox.data.InboxMessage<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>- if successful, return the number of the unread InboxMessages. Otherwise, return error </td>
	</tr>
</table>


----------  
  

#### <a name="1ae23065c20a803b904da72bf7032b2d79"></a>public static void readMessage(String code)  
Call this method, when the user reads the com.pushwoosh.inbox.data.InboxMessage<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>code</strong></td>
		<td>of the inboxMessage </td>
	</tr>
</table>


----------  
  

#### <a name="1a847a09d7defca6305d1e126c38bf3c78"></a>public static void readMessages(Collection&lt;String&gt; codes)  
Call this method, when the user reads list of InboxMessage<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>codes</strong></td>
		<td>of the inboxMessages </td>
	</tr>
</table>


----------  
  

#### <a name="1a325e52c29534d4e43ffbfedbc227f82f"></a>public static void performAction(String code)  
Call this method, when the user clicks on the com.pushwoosh.inbox.data.InboxMessage and the message's action is performed<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>code</strong></td>
		<td>of the inboxMessage that the user tapped </td>
	</tr>
</table>


----------  
  

#### <a name="1a9a48658faa71c5c77abd7dbd985ded5a"></a>public static void deleteMessage(String code)  
Call this method, when the user deletes the com.pushwoosh.inbox.data.InboxMessage manually<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>code</strong></td>
		<td>of the inboxMessage that the user deleted </td>
	</tr>
</table>


----------  
  

#### <a name="1a206f4c3551edcd10019685e94843a3f7"></a>public static void deleteMessages(Collection&lt;String&gt; codes)  
Call this method, when the user deletes the list of com.pushwoosh.inbox.data.InboxMessage manually<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>codes</strong></td>
		<td>of the list of com.pushwoosh.inbox.data.InboxMessage#getCode() that the user deleted </td>
	</tr>
</table>


----------  
  

#### <a name="1a49ebfb1cafbc65ce5912606ec20295d9"></a>public static void loadMessages(Callback&lt;Collection&lt;InboxMessage&gt;, InboxMessagesException&gt; callback, @Nullable InboxMessage inboxMessage, int limit)  
Get the collection of the com.pushwoosh.inbox.data.InboxMessage that the user received This method obtains messages from network. In case the network connection is not available messages will be obtained from local database<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>callback</strong></td>
		<td>- if successful, return the collection of the InboxMessages. Otherwise, return error </td>
	</tr>
	<tr>
		<td><strong>inboxMessage</strong></td>
		<td>- This parameter provides pagination. Pass the last com.pushwoosh.inbox.data.InboxMessage that is on your current page as a parameter to get previous messages. To get latest messages or in case the pagination is not necessary, pass null as a parameter. </td>
	</tr>
	<tr>
		<td><strong>limit</strong></td>
		<td>- amount of messages to get. Pass -1 to get all the messages </td>
	</tr>
</table>
