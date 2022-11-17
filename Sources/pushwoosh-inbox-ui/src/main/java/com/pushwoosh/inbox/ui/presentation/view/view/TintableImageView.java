/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inbox.ui.presentation.view.view;

import android.content.Context;
import android.content.res.ColorStateList;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

public class TintableImageView extends AppCompatImageView {
 
    private ColorStateList tint;
 
    public TintableImageView(Context context) {
        super(context);
    } 
 
    public TintableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
 
    public TintableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void drawableStateChanged() { 
        super.drawableStateChanged();
		if (tint != null && tint.isStateful()) {
			updateTintColor();
		}
    } 
 
    public void setColorFilter(ColorStateList tint) {
        this.tint = tint;
        super.setColorFilter(tint.getColorForState(getDrawableState(), 0));
    } 
 
    private void updateTintColor() { 
        int color = tint.getColorForState(getDrawableState(), 0);
        setColorFilter(color);
    } 
 
} 