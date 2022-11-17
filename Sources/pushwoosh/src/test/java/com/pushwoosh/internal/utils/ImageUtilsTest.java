package com.pushwoosh.internal.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ImageUtilsTest {

    public static final int WIDTH = 101;
    public static final int HEIGHT = 102;
    private ImageUtils imageUtils;

    @Mock
    private Drawable drawable;
    @Mock
    private Bitmap bitmap;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        imageUtils = new ImageUtils();
    }

    @Test
    public void drawableToBitmapIfDrawableInstanceBitmapDrawableShouldReturnInternalBitmap() {
        Drawable drawable = new BitmapDrawable(bitmap);
        Bitmap result = imageUtils.drawableToBitmap(drawable);
        Assert.assertEquals(bitmap, result);
    }

    @Test
    public void drawableToBitmapIfDrawableNotInstanceBitmapDrawableShouldReturn() {
        when(drawable.getIntrinsicWidth()).thenReturn(WIDTH);
        when(drawable.getIntrinsicHeight()).thenReturn(HEIGHT);

        Bitmap result = imageUtils.drawableToBitmap(drawable);
        //todo inject canvas to ImageUtils
        verify(drawable).setBounds(0, 0, 0, 0);
        drawable.draw(Mockito.any(Canvas.class));

        Assert.assertEquals(HEIGHT, result.getHeight());
        Assert.assertEquals(WIDTH, result.getWidth());
    }
}