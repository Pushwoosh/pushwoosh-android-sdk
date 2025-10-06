//[pushwoosh](../../../index.md)/[com.pushwoosh.function](../index.md)/[Result](index.md)/[fromException](from-exception.md)

# fromException

[main]\
open fun &lt;[T](from-exception.md), [E](from-exception.md) : [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)?&gt; [fromException](from-exception.md)(exception: [E](from-exception.md)): [Result](index.md)&lt;[T](from-exception.md), [E](from-exception.md)&gt;

Creates a failed result containing the specified exception. 

 This factory method constructs a Result instance representing a failed operation. The created result will have [isSuccess](is-success.md) return false, getData return null, and getException return the provided exception. 

**Note:** This is an internal SDK method. Application developers typically don't need to create Result instances directly as they are provided by SDK callback methods.

#### Return

a Result instance representing a failed operation with the given exception

#### Parameters

main

| | |
|---|---|
| exception | the exception describing the failure (must not be null) |
| &lt;T&gt; | the type of success data (not used in failed results) |
| &lt;E&gt; | the type of exception |
