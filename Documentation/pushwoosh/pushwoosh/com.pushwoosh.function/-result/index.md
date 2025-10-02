//[pushwoosh](../../../index.md)/[com.pushwoosh.function](../index.md)/[Result](index.md)

# Result

open class [Result](index.md)&lt;[T](index.md), [E](index.md) : PushwooshException?&gt;

Result class encapsulates result of an asynchronous operation

#### Parameters

main

| | |
|---|---|
| &lt;T&gt; | Data Class |
| &lt;E&gt; | Exception Class |

## Properties

| Name | Summary |
|---|---|
| [data](data.md) | [main]<br>val [data](data.md): [T](index.md) |
| [exception](exception.md) | [main]<br>val [exception](exception.md): [E](index.md) |

## Functions

| Name | Summary |
|---|---|
| [from](from.md) | [main]<br>open fun &lt;[T](from.md), [E](from.md) : PushwooshException?&gt; [from](from.md)(data: [T](from.md), exception: [E](from.md)): [Result](index.md)&lt;[T](from.md), [E](from.md)&gt; |
| [fromData](from-data.md) | [main]<br>open fun &lt;[T](from-data.md), [E](from-data.md) : PushwooshException?&gt; [fromData](from-data.md)(data: [T](from-data.md)): [Result](index.md)&lt;[T](from-data.md), [E](from-data.md)&gt;<br>Factory method that constructs successful result with given data |
| [fromException](from-exception.md) | [main]<br>open fun &lt;[T](from-exception.md), [E](from-exception.md) : PushwooshException?&gt; [fromException](from-exception.md)(exception: [E](from-exception.md)): [Result](index.md)&lt;[T](from-exception.md), [E](from-exception.md)&gt;<br>Factory method that constructs unsuccessful result with given exception |
| [isSuccess](is-success.md) | [main]<br>open fun [isSuccess](is-success.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
