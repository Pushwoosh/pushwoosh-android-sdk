//[pushwoosh](../../../index.md)/[com.pushwoosh.function](../index.md)/[Result](index.md)/[fromData](from-data.md)

# fromData

[main]\
open fun &lt;[T](from-data.md), [E](from-data.md) : [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)?&gt; [fromData](from-data.md)(data: [T](from-data.md)): [Result](index.md)&lt;[T](from-data.md), [E](from-data.md)&gt;

Creates a successful result containing the specified data. 

 This factory method constructs a Result instance representing a successful operation. The created result will have [isSuccess](is-success.md) return true, getData return the provided data, and getException return null. 

**Note:** This is an internal SDK method. Application developers typically don't need to create Result instances directly as they are provided by SDK callback methods.

#### Return

a Result instance representing a successful operation with the given data

#### Parameters

main

| | |
|---|---|
| data | the data to wrap in a successful result (may be null for Void operations) |
| &lt;T&gt; | the type of success data |
| &lt;E&gt; | the type of exception (not used in successful results) |
