//[pushwoosh-inbox-ui](../../../index.md)/[com.pushwoosh.inbox.ui.presentation.presenter](../index.md)/[InboxPresenter](index.md)

# InboxPresenter

[androidJvm]\
class [InboxPresenter](index.md)(inboxView: [InboxView](../-inbox-view/index.md)) : [BasePresenter](../-base-presenter/index.md)

## Constructors

| | |
|---|---|
| [InboxPresenter](-inbox-presenter.md) | [androidJvm]<br>constructor(inboxView: [InboxView](../-inbox-view/index.md)) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [onCreate](on-create.md) | [androidJvm]<br>open override fun [onCreate](on-create.md)(bundle: [Bundle](https://developer.android.com/reference/kotlin/android/os/Bundle.html)?) |
| [onDestroy](on-destroy.md) | [androidJvm]<br>open override fun [onDestroy](on-destroy.md)(isFinished: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)) |
| [onItemClick](on-item-click.md) | [androidJvm]<br>fun [onItemClick](on-item-click.md)(inboxMessage: InboxMessage) |
| [onSaveInstanceState](on-save-instance-state.md) | [androidJvm]<br>open override fun [onSaveInstanceState](on-save-instance-state.md)(out: [Bundle](https://developer.android.com/reference/kotlin/android/os/Bundle.html)) |
| [onStart](../-base-presenter/on-start.md) | [androidJvm]<br>open override fun [onStart](../-base-presenter/on-start.md)() |
| [onStop](../-base-presenter/on-stop.md) | [androidJvm]<br>open override fun [onStop](../-base-presenter/on-stop.md)() |
| [onViewCreated](on-view-created.md) | [androidJvm]<br>open override fun [onViewCreated](on-view-created.md)() |
| [onViewDestroy](../-base-presenter/on-view-destroy.md) | [androidJvm]<br>open override fun [onViewDestroy](../-base-presenter/on-view-destroy.md)() |
| [refreshItems](refresh-items.md) | [androidJvm]<br>fun [refreshItems](refresh-items.md)() |
| [removeItem](remove-item.md) | [androidJvm]<br>fun [removeItem](remove-item.md)(inboxMessage: InboxMessage?) |
