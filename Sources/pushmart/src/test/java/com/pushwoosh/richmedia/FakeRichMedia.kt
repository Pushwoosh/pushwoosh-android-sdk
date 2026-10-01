package com.pushwoosh.richmedia

import com.pushwoosh.inapp.network.model.Resource
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper

/**
 * Builds a [RichMedia] carrying [code] as its content. Lives in the SDK package because the
 * `RichMedia` constructor is package private and the SDK exposes no other way to make one.
 */
fun fakeRichMedia(code: String): RichMedia =
    RichMedia(ResourceWrapper.Builder().setResource(Resource(code, false)).build())
