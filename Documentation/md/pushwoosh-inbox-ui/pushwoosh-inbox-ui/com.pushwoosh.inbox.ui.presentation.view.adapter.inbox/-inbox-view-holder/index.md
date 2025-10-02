//[pushwoosh-inbox-ui](../../../index.md)/[com.pushwoosh.inbox.ui.presentation.view.adapter.inbox](../index.md)/[InboxViewHolder](index.md)

# InboxViewHolder

[androidJvm]\
class [InboxViewHolder](index.md)(adapter: [InboxAdapter](../-inbox-adapter/index.md), itemView: [View](https://developer.android.com/reference/kotlin/android/view/View.html), colorSchemeProvider: [ColorSchemeProvider](../../com.pushwoosh.inbox.ui.presentation.view.style/-color-scheme-provider/index.md), attachmentClickListener: ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [View](https://developer.android.com/reference/kotlin/android/view/View.html)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)) : [BaseRecyclerAdapter.ViewHolder](../../com.pushwoosh.inbox.ui.presentation.view.adapter/-base-recycler-adapter/-view-holder/index.md)&lt;InboxMessage&gt;

## Constructors

| | |
|---|---|
| [InboxViewHolder](-inbox-view-holder.md) | [androidJvm]<br>constructor(adapter: [InboxAdapter](../-inbox-adapter/index.md), itemView: [View](https://developer.android.com/reference/kotlin/android/view/View.html), colorSchemeProvider: [ColorSchemeProvider](../../com.pushwoosh.inbox.ui.presentation.view.style/-color-scheme-provider/index.md), attachmentClickListener: ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [View](https://developer.android.com/reference/kotlin/android/view/View.html)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [attachmentClickListener](attachment-click-listener.md) | [androidJvm]<br>var [attachmentClickListener](attachment-click-listener.md): ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [View](https://developer.android.com/reference/kotlin/android/view/View.html)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html) |
| [itemView](index.md#29975211%2FProperties%2F1408892949) | [androidJvm]<br>@[NonNull](https://developer.android.com/reference/kotlin/androidx/annotation/NonNull.html)<br>val [itemView](index.md#29975211%2FProperties%2F1408892949): [View](https://developer.android.com/reference/kotlin/android/view/View.html) |

## Functions

| Name | Summary |
|---|---|
| [bindView](../../com.pushwoosh.inbox.ui.presentation.view.adapter/-base-recycler-adapter/-view-holder/bind-view.md) | [androidJvm]<br>fun [bindView](../../com.pushwoosh.inbox.ui.presentation.view.adapter/-base-recycler-adapter/-view-holder/bind-view.md)() |
| [fillView](fill-view.md) | [androidJvm]<br>open override fun [fillView](fill-view.md)(model: InboxMessage?, position: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) |
| [getAdapterPosition](index.md#644519777%2FFunctions%2F1408892949) | [androidJvm]<br>fun [getAdapterPosition](index.md#644519777%2FFunctions%2F1408892949)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getItemId](index.md#1378485811%2FFunctions%2F1408892949) | [androidJvm]<br>fun [getItemId](index.md#1378485811%2FFunctions%2F1408892949)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [getItemViewType](index.md#-1649344625%2FFunctions%2F1408892949) | [androidJvm]<br>fun [getItemViewType](index.md#-1649344625%2FFunctions%2F1408892949)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getLayoutPosition](index.md#-1407255826%2FFunctions%2F1408892949) | [androidJvm]<br>fun [getLayoutPosition](index.md#-1407255826%2FFunctions%2F1408892949)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getOldPosition](index.md#-1203059319%2FFunctions%2F1408892949) | [androidJvm]<br>fun [getOldPosition](index.md#-1203059319%2FFunctions%2F1408892949)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [isRecyclable](index.md#-1703443315%2FFunctions%2F1408892949) | [androidJvm]<br>fun [isRecyclable](index.md#-1703443315%2FFunctions%2F1408892949)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [onClick](../../com.pushwoosh.inbox.ui.presentation.view.adapter/-base-recycler-adapter/-view-holder/on-click.md) | [androidJvm]<br>open override fun [onClick](../../com.pushwoosh.inbox.ui.presentation.view.adapter/-base-recycler-adapter/-view-holder/on-click.md)(v: [View](https://developer.android.com/reference/kotlin/android/view/View.html)) |
| [onCreate](../../com.pushwoosh.inbox.ui.presentation.view.adapter/-base-recycler-adapter/-view-holder/on-create.md) | [androidJvm]<br>fun [onCreate](../../com.pushwoosh.inbox.ui.presentation.view.adapter/-base-recycler-adapter/-view-holder/on-create.md)() |
| [setIsRecyclable](index.md#-1860912636%2FFunctions%2F1408892949) | [androidJvm]<br>fun [setIsRecyclable](index.md#-1860912636%2FFunctions%2F1408892949)(p0: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)) |
| [toString](index.md#-1200015593%2FFunctions%2F1408892949) | [androidJvm]<br>open override fun [toString](index.md#-1200015593%2FFunctions%2F1408892949)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
