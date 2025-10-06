//[pushwoosh](../../../index.md)/[com.pushwoosh.function](../index.md)/[Result](index.md)/[from](from.md)

# from

[main]\
open fun &lt;[T](from.md), [E](from.md) : [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)?&gt; [from](from.md)(data: [T](from.md), exception: [E](from.md)): [Result](index.md)&lt;[T](from.md), [E](from.md)&gt;

Creates a result with both data and exception fields. 

 This factory method constructs a Result instance that may contain either data, exception, or both. It provides flexibility for edge cases where both values might be present. 

**Note:** This is an internal SDK method. Application developers typically don't need to create Result instances directly as they are provided by SDK callback methods.

#### Return

a Result instance with the given data and exception

#### Parameters

main

| | |
|---|---|
| data | the success data (may be null) |
| exception | the failure exception (may be null) |
| &lt;T&gt; | the type of success data |
| &lt;E&gt; | the type of exception |
