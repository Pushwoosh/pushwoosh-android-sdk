package com.pushwoosh.inapp.ui.view

import android.content.Context
import com.pushwoosh.inapp.ui.model.InAppLayout

/** Builds the template view for a layout. Banner is shown via the overlay controller,
 *  not the Activity, so it returns null here. */
internal object InAppViewFactory {

    fun create(
        context: Context,
        layout: InAppLayout,
        listener: InAppTemplateView.Listener
    ): InAppTemplateView? {
        val view: InAppTemplateView = when (layout) {
            is InAppLayout.Modal -> ModalInAppView(context, layout.content)
            is InAppLayout.Fullscreen -> FullscreenInAppView(context, layout.content)
            is InAppLayout.Carousel -> CarouselInAppView(context, layout.content)
            is InAppLayout.Stories -> StoriesInAppView(context, layout.content)
            is InAppLayout.Banner -> return null
        }
        view.listener = listener
        return view
    }
}
