
#### module: pushwoosh-badge  

#### package: com.pushwoosh.badge  

# <a name="heading"></a>class PushwooshBadge  
PushwooshBadge is a static class responsible for application icon badge number managing. <br/>
 By default pushwoosh-badge library automatically adds following permissions:<br/>
 com.sec.android.provider.badge.permission.READ<br/>
 com.sec.android.provider.badge.permission.WRITE<br/><br/>com.htc.launcher.permission.READ\_SETTINGS<br/>
 com.htc.launcher.permission.UPDATE\_SHORTCUT<br/><br/>com.sonyericsson.home.permission.BROADCAST\_BADGE<br/>
 com.sonymobile.home.permission.PROVIDER\_INSERT\_BADGE<br/><br/>com.anddoes.launcher.permission.UPDATE\_COUNT<br/><br/>com.majeur.launcher.permission.UPDATE\_BADGE<br/><br/>com.huawei.android.launcher.permission.CHANGE\_BADGE<br/>
 com.huawei.android.launcher.permission.READ\_SETTINGS<br/>
 com.huawei.android.launcher.permission.WRITE\_SETTINGS<br/><br/>android.permission.READ\_APP\_BADGE<br/><br/>com.oppo.launcher.permission.READ\_SETTINGS<br/>
 com.oppo.launcher.permission.WRITE\_SETTINGS<br/><br/>me.everything.badger.permission.BADGE\_COUNT\_READ<br/>
 me.everything.badger.permission.BADGE\_COUNT\_WRITE<br/>
## Members  

<table>
	<tr>
		<td><a href="#1adfa0ac3f01e86a0b7666ea8d1bb15f30">public static void setBadgeNumber(int newBadge)</a></td>
	</tr>
	<tr>
		<td><a href="#1adccc14d470d68243809d3a03ab380b9c">public static int getBadgeNumber()</a></td>
	</tr>
	<tr>
		<td><a href="#1a0d278ad3e661b0e8a76437bc9f18b0de">public static void addBadgeNumber(int deltaBadge)</a></td>
	</tr>
</table>


----------  
  

#### <a name="1adfa0ac3f01e86a0b7666ea8d1bb15f30"></a>public static void setBadgeNumber(int newBadge)  
Set application icon badge number and synchronize this value with pushwoosh backend. 0 value can be used to clear badges<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>newBadge</strong></td>
		<td>icon badge number </td>
	</tr>
</table>


----------  
  

#### <a name="1adccc14d470d68243809d3a03ab380b9c"></a>public static int getBadgeNumber()  
<strong>Returns</strong> current application icon badge number 

----------  
  

#### <a name="1a0d278ad3e661b0e8a76437bc9f18b0de"></a>public static void addBadgeNumber(int deltaBadge)  
Increment current icon badge number<br/><br/><br/><strong>Parameters</strong><br/>
<table>
	<tr>
		<td><strong>deltaBadge</strong></td>
		<td>application icon badge number addition </td>
	</tr>
</table>
