//[pushwoosh-badge](../../../index.md)/[com.pushwoosh.badge](../index.md)/[PushwooshBadge](index.md)

# PushwooshBadge

[main]\
open class [PushwooshBadge](index.md)

PushwooshBadge is a static class responsible for application icon badge number managing.  By default pushwoosh-badge library automatically adds following permissions: com.sec.android.provider.badge.permission.READ com.sec.android.provider.badge.permission.WRITE<!--for htc--> com.htc.launcher.permission.READ_SETTINGS com.htc.launcher.permission.UPDATE_SHORTCUT<!--for sony--> com.sonyericsson.home.permission.BROADCAST_BADGE com.sonymobile.home.permission.PROVIDER_INSERT_BADGE<!--for apex--> com.anddoes.launcher.permission.UPDATE_COUNT<!--for solid--> com.majeur.launcher.permission.UPDATE_BADGE<!--for huawei--> com.huawei.android.launcher.permission.CHANGE_BADGE com.huawei.android.launcher.permission.READ_SETTINGS com.huawei.android.launcher.permission.WRITE_SETTINGS<!--for ZUK--> android.permission.READ_APP_BADGE<!--for OPPO--> com.oppo.launcher.permission.READ_SETTINGS com.oppo.launcher.permission.WRITE_SETTINGS<!--for EvMe--> me.everything.badger.permission.BADGE_COUNT_READ me.everything.badger.permission.BADGE_COUNT_WRITE

## Constructors

| | |
|---|---|
| [PushwooshBadge](-pushwoosh-badge.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [addBadgeNumber](add-badge-number.md) | [main]<br>open fun [addBadgeNumber](add-badge-number.md)(deltaBadge: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Increment current icon badge number |
| [getBadgeNumber](get-badge-number.md) | [main]<br>open fun [getBadgeNumber](get-badge-number.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [setBadgeNumber](set-badge-number.md) | [main]<br>open fun [setBadgeNumber](set-badge-number.md)(newBadge: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Set application icon badge number and synchronize this value with pushwoosh backend. |
