
#### module: pushwoosh  

#### package: com.pushwoosh.notification  

# <a name="heading"></a>class LocalNotificationRequest  
Manages local notification schedule. 
## Members  

<table>
	<tr>
		<td><a href="#1ac639bdb714188d149a6d9ac5273fba49">public int getRequestId()</a></td>
	</tr>
	<tr>
		<td><a href="#1a09b604bf065a591b7ccfc0af8c0a8c6d">public  LocalNotificationRequest(int requestId)</a></td>
	</tr>
	<tr>
		<td><a href="#1ae0ca90203214f7386f6ca5ff8067eb3d">public void cancel()</a></td>
	</tr>
	<tr>
		<td><a href="#1aa09eaa92a2a14aa543bdb92f81790310">public void unschedule()</a></td>
	</tr>
</table>


----------  
  

#### <a name="1ac639bdb714188d149a6d9ac5273fba49"></a>public int getRequestId()  


----------  
  

#### <a name="1a09b604bf065a591b7ccfc0af8c0a8c6d"></a>public  LocalNotificationRequest(int requestId)  


----------  
  

#### <a name="1ae0ca90203214f7386f6ca5ff8067eb3d"></a>public void cancel()  
Cancels local notification associated with this request and unschedules notification if it was not displayed yet. 

----------  
  

#### <a name="1aa09eaa92a2a14aa543bdb92f81790310"></a>public void unschedule()  
Undo <a href="../Pushwoosh.md#1a3e7942de9d01dc94dd8364db20f9c65a">com.pushwoosh.Pushwoosh#scheduleLocalNotification(LocalNotification)</a>. If notification has been displayed it will not be deleted. 