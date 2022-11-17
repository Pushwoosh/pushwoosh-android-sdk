package com.pushwoosh.inbox.ui.utils;

import android.widget.ImageView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class is used for backward compatibility of com.bumptech.glide library.
 */
public class GlideUtils {
    public static void applyInto(RequestBuilder requestBuilder, ImageView imageView) {
        try {
            Class<RequestBuilder> requestBuilderClass = RequestBuilder.class;
            Method applyMethod;
            try {
                applyMethod = requestBuilderClass.getMethod("apply", Class.forName("com.bumptech.glide.request.BaseRequestOptions"));
            } catch (Throwable throwable) {
                applyMethod = requestBuilderClass.getMethod("apply", Class.forName("com.bumptech.glide.request.RequestOptions"));
            }
            RequestBuilder applyRequestBuilder = (RequestBuilder) applyMethod.invoke(requestBuilder, getCircleCropRequestOptions());
            applyRequestBuilder.into(imageView);
        } catch (Throwable throwable) {
            requestBuilder.into(imageView);
        }
    }

    public static Object getCircleCropRequestOptions()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        try {
            Class<RequestOptions> requestOptionsClass = RequestOptions.class;
            Object requestOptionsInstance = requestOptionsClass.newInstance();
            Class transformationClass = com.bumptech.glide.load.Transformation.class;
            Class circleCropClass = com.bumptech.glide.load.resource.bitmap.CircleCrop.class;
            Object circleCropInstance = circleCropClass.newInstance();
            Method transformMethod = requestOptionsClass.getMethod("transform", transformationClass);
            return transformMethod.invoke(requestOptionsInstance, circleCropInstance);
        } catch (Throwable throwable) {
            return getCircleCropBaseRequestOptions();
        }
    }

    private static Object getCircleCropBaseRequestOptions()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<RequestOptions> requestOptionsClass = RequestOptions.class;
        Method circleCropTransformMethod = requestOptionsClass.getMethod("circleCropTransform");
        return circleCropTransformMethod.invoke(null);
    }
}
