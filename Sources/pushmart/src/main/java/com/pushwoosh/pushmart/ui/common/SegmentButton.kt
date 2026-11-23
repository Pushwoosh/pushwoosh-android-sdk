package com.pushwoosh.pushmart.ui.common

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel

/**
 * A segment of a `MaterialButtonToggleGroup` that stays a capsule, as the thumb of the iOS
 * segmented control. The group squares its buttons' inner corners on every measure.
 */
class SegmentButton
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle,
) : MaterialButton(context, attrs, defStyleAttr) {
    override fun setShapeAppearanceModel(shapeAppearanceModel: ShapeAppearanceModel) {
        super.setShapeAppearanceModel(shapeAppearanceModel.withCornerSize(RelativeCornerSize(0.5f)))
    }
}
