//[pushwoosh-inbox-ui](../../../index.md)/[com.pushwoosh.inbox.ui.presentation.presenter](../index.md)/[InboxView](index.md)

# InboxView

interface [InboxView](index.md) : [Lifecycle](../../com.pushwoosh.inbox.ui.presentation.lifecycle/-lifecycle/index.md)

#### Inheritors

| |
|---|
| [InboxFragment](../../com.pushwoosh.inbox.ui.presentation.view.fragment/-inbox-fragment/index.md) |

## Functions

| Name | Summary |
|---|---|
| [addLifecycleListener](../../com.pushwoosh.inbox.ui.presentation.lifecycle/-lifecycle/add-lifecycle-listener.md) | [androidJvm]<br>abstract fun [addLifecycleListener](../../com.pushwoosh.inbox.ui.presentation.lifecycle/-lifecycle/add-lifecycle-listener.md)(lifecycleListener: [LifecycleListener](../../com.pushwoosh.inbox.ui.presentation.lifecycle/-lifecycle-listener/index.md)) |
| [failedLoadingInboxList](failed-loading-inbox-list.md) | [androidJvm]<br>abstract fun [failedLoadingInboxList](failed-loading-inbox-list.md)(userError: [UserError](../../com.pushwoosh.inbox.ui.presentation.data/-user-error/index.md)) |
| [hideProgress](hide-progress.md) | [androidJvm]<br>abstract fun [hideProgress](hide-progress.md)() |
| [removeLifecycleListener](../../com.pushwoosh.inbox.ui.presentation.lifecycle/-lifecycle/remove-lifecycle-listener.md) | [androidJvm]<br>abstract fun [removeLifecycleListener](../../com.pushwoosh.inbox.ui.presentation.lifecycle/-lifecycle/remove-lifecycle-listener.md)(lifecycleListener: [LifecycleListener](../../com.pushwoosh.inbox.ui.presentation.lifecycle/-lifecycle-listener/index.md)) |
| [showEmptyView](show-empty-view.md) | [androidJvm]<br>abstract fun [showEmptyView](show-empty-view.md)() |
| [showList](show-list.md) | [androidJvm]<br>abstract fun [showList](show-list.md)(inboxList: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-collection/index.html)&lt;InboxMessage&gt;) |
| [showSwipeRefreshProgress](show-swipe-refresh-progress.md) | [androidJvm]<br>abstract fun [showSwipeRefreshProgress](show-swipe-refresh-progress.md)() |
| [showTotalProgress](show-total-progress.md) | [androidJvm]<br>abstract fun [showTotalProgress](show-total-progress.md)() |
