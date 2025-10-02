//[pushwoosh-inbox-ui](../../../../index.md)/[com.pushwoosh.inbox.ui.presentation.view.adapter](../../index.md)/[BaseRecyclerAdapter](../index.md)/[ViewHolder](index.md)

# ViewHolder

abstract class [ViewHolder](index.md)&lt;[Model](index.md)&gt;(view: [View](https://developer.android.com/reference/kotlin/android/view/View.html), adapter: [BaseRecyclerAdapter](../index.md)&lt;*, *&gt;) : [RecyclerView.ViewHolder](https://developer.android.com/reference/kotlin/androidx/recyclerview/widget/RecyclerView.ViewHolder.html), [View.OnClickListener](https://developer.android.com/reference/kotlin/android/view/View.OnClickListener.html)

#### Inheritors

| |
|---|
| [InboxViewHolder](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md) |

## Constructors

| | |
|---|---|
| [ViewHolder](-view-holder.md) | [androidJvm]<br>constructor(view: [View](https://developer.android.com/reference/kotlin/android/view/View.html), adapter: [BaseRecyclerAdapter](../index.md)&lt;*, *&gt;) |

## Properties

| Name | Summary |
|---|---|
| [itemView](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#29975211%2FProperties%2F1408892949) | [androidJvm]<br>@[NonNull](https://developer.android.com/reference/kotlin/androidx/annotation/NonNull.html)<br>val [itemView](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#29975211%2FProperties%2F1408892949): [View](https://developer.android.com/reference/kotlin/android/view/View.html) |

## Functions

| Name | Summary |
|---|---|
| [bindView](bind-view.md) | [androidJvm]<br>fun [bindView](bind-view.md)() |
| [fillView](fill-view.md) | [androidJvm]<br>abstract fun [fillView](fill-view.md)(model: [Model](index.md)?, position: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) |
| [getAdapterPosition](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#644519777%2FFunctions%2F1408892949) | [androidJvm]<br>fun [getAdapterPosition](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#644519777%2FFunctions%2F1408892949)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getItemId](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#1378485811%2FFunctions%2F1408892949) | [androidJvm]<br>fun [getItemId](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#1378485811%2FFunctions%2F1408892949)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [getItemViewType](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1649344625%2FFunctions%2F1408892949) | [androidJvm]<br>fun [getItemViewType](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1649344625%2FFunctions%2F1408892949)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getLayoutPosition](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1407255826%2FFunctions%2F1408892949) | [androidJvm]<br>fun [getLayoutPosition](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1407255826%2FFunctions%2F1408892949)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getOldPosition](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1203059319%2FFunctions%2F1408892949) | [androidJvm]<br>fun [getOldPosition](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1203059319%2FFunctions%2F1408892949)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [isRecyclable](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1703443315%2FFunctions%2F1408892949) | [androidJvm]<br>fun [isRecyclable](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1703443315%2FFunctions%2F1408892949)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [onClick](on-click.md) | [androidJvm]<br>open override fun [onClick](on-click.md)(v: [View](https://developer.android.com/reference/kotlin/android/view/View.html)) |
| [onCreate](on-create.md) | [androidJvm]<br>fun [onCreate](on-create.md)() |
| [setIsRecyclable](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1860912636%2FFunctions%2F1408892949) | [androidJvm]<br>fun [setIsRecyclable](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1860912636%2FFunctions%2F1408892949)(p0: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)) |
| [toString](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1200015593%2FFunctions%2F1408892949) | [androidJvm]<br>open override fun [toString](../../../com.pushwoosh.inbox.ui.presentation.view.adapter.inbox/-inbox-view-holder/index.md#-1200015593%2FFunctions%2F1408892949)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
