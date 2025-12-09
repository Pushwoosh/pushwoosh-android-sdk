package com.pushwoosh.inapp.view.utils;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.view.ModalRichMediaWindow;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.internal.platform.AndroidPlatformModule;

import java.util.Set;

public class ModalRichMediaWindowUtils {
    private static final float SWIPE_THRESHOLD_FACTOR = 0.5f;

    static int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    static int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
    static int statusBarInset = 0;

    @Nullable
    public static View getParentView() {
        Activity activity = PushwooshPlatform.getInstance().getTopActivity();
        if (activity == null) {
            return null;
        }

        Window window = activity.getWindow();
        if (window == null) {
            return null;
        }

        View decorView = window.getDecorView();

        return decorView.findViewById(android.R.id.content);
    }


    public static void getParentViewAsync(Consumer<View> callback) {
        Activity topActivity = PushwooshPlatform.getInstance().getTopActivity();
        if (topActivity != null) {
            Window window = topActivity.getWindow();
            if (window != null) {
                window.getDecorView().post(() -> {
                    View parentView = window.getDecorView().findViewById(android.R.id.content);
                    callback.accept(parentView);
                });
            } else {
                callback.accept(null);
            }
        } else {
            callback.accept(null);
        }
    }


    public static ValueAnimator dismissWindowWithFadeOutAnimation(ModalRichMediaWindow window) {
        ValueAnimator fadeInAnimator = ValueAnimator.ofFloat(1f, 0f);
        fadeInAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            window.getContentView().setAlpha(animatedValue);
        });
        return fadeInAnimator;
    }

    public static ValueAnimator dismissWindowToLeftAnimation(ModalRichMediaWindow window, int screenWidth) {
        ValueAnimator animator = ValueAnimator.ofInt(0, screenWidth);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            window.update(-animatedValue, window.getTopInset() + window.getBottomInset(), -1 ,-1);
        });
        return animator;
    }

    public static ValueAnimator dismissWindowToRightAnimation(ModalRichMediaWindow window, int screenWidth) {
        ValueAnimator animator = ValueAnimator.ofInt(0, screenWidth);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            window.update(animatedValue, window.getTopInset() + window.getBottomInset(), -1 ,-1);
        });
        return animator;
    }

    public static ValueAnimator dismissWindowToTopAnimation(ModalRichMediaWindow window, int screenHeight, ModalRichmediaConfig config) {
        ValueAnimator animator = ValueAnimator.ofInt(getStatusBarInset(), - screenHeight);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            if (config.getViewPosition() == ModalRichMediaViewPosition.BOTTOM) {
                animatedValue = -animatedValue; //for bottom coordinates are inverted
            }
            window.update(0, animatedValue + window.getTopInset() + window.getBottomInset(), -1 ,-1);
        });
        return animator;
    }

    public static ValueAnimator dismissWindowToBottomAnimation(ModalRichMediaWindow window, int screenHeight, ModalRichmediaConfig config) {
        ValueAnimator animator = ValueAnimator.ofInt(getStatusBarInset(), screenHeight);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            if (config.getViewPosition() == ModalRichMediaViewPosition.BOTTOM) {
                animatedValue = -animatedValue; //for bottom coordinates are inverted
            }
            window.update(0, animatedValue + window.getTopInset() + window.getBottomInset(), -1 ,-1);
        });
        return animator;
    }

    public static ValueAnimator presentWindowFromLeftAnimation(ModalRichMediaWindow window, int screenWidth) {
        ValueAnimator animator = ValueAnimator.ofInt(-screenWidth, 0);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            window.update(animatedValue, window.getBottomInset() + window.getTopInset(), -1 ,-1);
        });
        return animator;
    }

    public static ValueAnimator presentWindowFromRightAnimation(ModalRichMediaWindow window, int screenWidth) {
        ValueAnimator animator = ValueAnimator.ofInt(screenWidth, 0);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            window.update(animatedValue, window.getBottomInset() + window.getTopInset(), -1 ,-1);
        });
        return animator;
    }

    public static ValueAnimator presentWindowFromTopAnimation(ModalRichMediaWindow window, int screenHeight, ModalRichmediaConfig config) {
        ValueAnimator animator = ValueAnimator.ofInt(-screenHeight, getStatusBarInset());
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            if (config.getViewPosition() == ModalRichMediaViewPosition.BOTTOM) {
                animatedValue = -animatedValue; //for bottom coordinates are inverted
            }
            window.update(0, animatedValue + window.getTopInset() + window.getBottomInset(), -1 ,-1);
        });
        return animator;
    }

    public static ValueAnimator presentWindowFromBottomAnimation(ModalRichMediaWindow window, int screenHeight, ModalRichmediaConfig config) {
        ValueAnimator animator = ValueAnimator.ofInt(screenHeight, getStatusBarInset());
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            if (config.getViewPosition() == ModalRichMediaViewPosition.BOTTOM) {
                animatedValue = -animatedValue; //for bottom coordinates are inverted
            }
            window.update(0, animatedValue + window.getTopInset() + window.getBottomInset(), -1 ,-1);
        });
        return animator;
    }

    public static ValueAnimator presentWindowWithFadeInAnimation(ModalRichMediaWindow window) {
        ValueAnimator fadeInAnimator = ValueAnimator.ofFloat(0f, 1f);
        fadeInAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            window.getContentView().setAlpha(animatedValue);
        });

        return fadeInAnimator;
    }

    public static ValueAnimator getDismissValueAnimatorForWindow(ModalRichMediaWindow window, ModalRichmediaConfig config) {
        ModalRichMediaDismissAnimationType type = config.getDismissAnimationType();
        ValueAnimator animator;
        switch (type) {
            case FADE_OUT:
                animator = dismissWindowWithFadeOutAnimation(window);
                break;
            case SLIDE_LEFT:
                animator = dismissWindowToLeftAnimation(window, screenWidth);
                break;
            case SLIDE_RIGHT:
                animator = dismissWindowToRightAnimation(window, screenWidth);
                break;
            case SLIDE_DOWN:
                animator = dismissWindowToBottomAnimation(window, screenHeight, config);
                break;
            case SLIDE_UP:
                animator = dismissWindowToTopAnimation(window, screenHeight, config);
                break;
            default:
                animator = null;
                break;
        }

        return animator;
    }

    public static ValueAnimator getPresentValueAnimatorForWindow(ModalRichMediaWindow window, ModalRichmediaConfig config) {
        ModalRichMediaPresentAnimationType type = config.getPresentAnimationType();
        ValueAnimator animator;
        switch (type) {
            case FADE_IN:
                animator = presentWindowWithFadeInAnimation(window);
                break;
            case SLIDE_FROM_LEFT:
                animator = presentWindowFromLeftAnimation(window, screenWidth);
                break;
            case SLIDE_FROM_RIGHT:
                animator = presentWindowFromRightAnimation(window, screenWidth);
                break;
            case SLIDE_UP:
                animator = presentWindowFromBottomAnimation(window, screenHeight, config);
                break;
            case DROP_DOWN:
                animator = presentWindowFromTopAnimation(window, screenHeight, config);
                break;
            default:
                animator = null;
                break;
        }

        return animator;
    }

    public static int getModalRichMediaWindowShowPositionX(ModalRichmediaConfig config) {
        ModalRichMediaPresentAnimationType type = config.getPresentAnimationType();
        int width;
        switch (type) {
            case SLIDE_FROM_LEFT:
                width = -screenWidth;
                break;
            case DROP_DOWN:
                width = screenWidth;
                break;
            default:
                width = 0;
                break;
        }

        return width;
    }

    public static int getModalRichMediaWindowShowPositionY(ModalRichmediaConfig config) {
        ModalRichMediaPresentAnimationType type = config.getPresentAnimationType();
        int height;
        switch (type) {
            case SLIDE_UP:
                height = screenHeight;
                break;
            case DROP_DOWN:
                height = -screenHeight;
                break;
            case FADE_IN:
                height = config.getViewPosition() == ModalRichMediaViewPosition.TOP ? getSystemWindowInsetTop() : 0;
                break;
            default:
                height = 0;
                break;
        }

        return height;
    }

    public static int getModalRichMediaWindowGravity(ModalRichmediaConfig config) {
        ModalRichMediaViewPosition viewPosition = config.getViewPosition();
        int gravity;
        switch (viewPosition) {
            case BOTTOM:
                gravity = Gravity.BOTTOM;
                break;
            case TOP:
                gravity = Gravity.TOP;
                break;
            default:
                gravity = Gravity.CENTER;
                break;
        }
        return gravity;
    }

    //Swiping related methods
    public static boolean isTouchInsidePopupWindow(ModalRichMediaWindow window, MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();

        // Get the location of the PopupWindow on the screen
        int[] location = new int[2];
        window.getContentView().getLocationOnScreen(location);

        // Get the width and height of the PopupWindow (considering WRAP_CONTENT)
        int width = window.getContentView().getWidth();
        int height = window.getContentView().getHeight();

        // Check if the touch event is within the bounds of the PopupWindow
        return x >= location[0] && x <= (location[0] + width) &&
                y >= location[1] && y <= (location[1] + height);
    }

    public static boolean dismissOnSwipeThreshold(ModalRichMediaWindow window, float deltaX, float deltaY, ModalRichmediaConfig config) {
        // Get the dimensions of the screen
        float screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        float screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        // Get the PopupWindow size (width and height)
        View contentView = window.getContentView();
        int[] windowLocation = new int[2];
        contentView.getLocationOnScreen(windowLocation); // Get the window's position on the screen

        int windowWidth = contentView.getWidth();
        int windowHeight = contentView.getHeight();

        // Variables to track how much of the window is off-screen
        boolean isDismiss = false;

        // Check horizontal swipe (left or right)
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            Set<ModalRichMediaSwipeGesture> gestures = config.getSwipeGestures();
            if (gestures.contains(ModalRichMediaSwipeGesture.LEFT) && (windowLocation[0] + windowWidth * SWIPE_THRESHOLD_FACTOR) < 0) {
                isDismiss = true;
            }
            if (gestures.contains(ModalRichMediaSwipeGesture.RIGHT) && (windowLocation[0] + windowWidth * SWIPE_THRESHOLD_FACTOR) > screenWidth) {
                isDismiss = true;
            }
        }

        // Check vertical swipe (up or down)
        if (Math.abs(deltaY) > Math.abs(deltaX)) {
            Set<ModalRichMediaSwipeGesture> gestures = config.getSwipeGestures();
            if (gestures.contains(ModalRichMediaSwipeGesture.UP) && (windowLocation[1] + windowHeight * SWIPE_THRESHOLD_FACTOR) < 0) {
                isDismiss = true;
            }
            if (gestures.contains(ModalRichMediaSwipeGesture.DOWN) && (windowLocation[1] + windowHeight * SWIPE_THRESHOLD_FACTOR) > screenHeight + getSystemWindowInsetTop() + getSystemWindowInsetBottom()) {
                isDismiss = true;
            }
        }

        // If the window should be dismissed, dismiss it and return true
        if (isDismiss) {
            window.dismiss();
            return true;
        }

        return false;
    }

    public static void movePopupOnDragEvent(ModalRichMediaWindow window, int dx, int dy, ModalRichmediaConfig config) {
        //if window has Gravity.BOTTOM, window's "anchor point" is the bottom of the screen,
        // so positive values move it upwards, and we need to invert the value
        if (config.getViewPosition() == ModalRichMediaViewPosition.BOTTOM) {
            dy = -dy;
        }
        Set<ModalRichMediaSwipeGesture> gestures = config.getSwipeGestures();
        if (!gestures.isEmpty()) {
            boolean verticalAllowed = gestures.contains(ModalRichMediaSwipeGesture.UP) || gestures.contains(ModalRichMediaSwipeGesture.DOWN);
            boolean horizontalAllowed = gestures.contains(ModalRichMediaSwipeGesture.LEFT) || gestures.contains(ModalRichMediaSwipeGesture.RIGHT);
            dx = horizontalAllowed ? dx : 0;
            dy = verticalAllowed ? dy : 0;

            window.update(dx, dy + window.getTopInset() + window.getBottomInset(), -1, -1, true);
        }
    }

    // status bar related utils
    public static void setStatusBarInset(Activity topActivity, boolean isStatusBarCovered) {
        if (!isStatusBarCovered) {
            Resources resources = topActivity.getResources();
            int statusBarResId = resources.getIdentifier("status_bar_height", "dimen", "android");
            if (statusBarResId > 0) {
                statusBarInset = resources.getDimensionPixelSize(statusBarResId);
            }
        }
    }

    public static int getStatusBarInset() {
        return statusBarInset;
    }

    private static int getStatusBarHeight() {
        Context context = AndroidPlatformModule.getApplicationContext();
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    /**
     * Returns status bar height in pixels. Used to position TOP popups below the status bar.
     * @return status bar height or 0 if activity is unavailable
     */
    public static int getSystemWindowInsetTop() {
        Activity topActivity = PushwooshPlatform.getInstance().getTopActivity();
        if (topActivity == null || topActivity.getWindow() == null) {
            return 0;
        }
        WindowInsets insets = topActivity.getWindow().getDecorView().getRootWindowInsets();
        if (insets == null) {
            return 0;
        }
        return insets.getSystemWindowInsetTop();
    }

    /**
     * Returns navigation bar height in pixels. Used for swipe gesture boundary calculations.
     * @return navigation bar height or 0 if activity is unavailable
     */
    public static int getSystemWindowInsetBottom() {
        Activity topActivity = PushwooshPlatform.getInstance().getTopActivity();
        if (topActivity == null || topActivity.getWindow() == null) {
            return 0;
        }
        WindowInsets insets = topActivity.getWindow().getDecorView().getRootWindowInsets();
        if (insets == null) {
            return 0;
        }
        return insets.getSystemWindowInsetBottom();
    }

    /**
     * Returns navigation bar height in pixels. Used to position BOTTOM popups above the nav bar.
     * On Android 15+ returns 0 if edge-to-edge layout is enabled.
     * @param config popup config to check edge-to-edge setting
     * @return navigation bar height or 0 if activity is unavailable or edge-to-edge is enabled
     */
    public static int getSystemWindowInsetBottom(ModalRichmediaConfig config) {
        Activity topActivity = PushwooshPlatform.getInstance().getTopActivity();
        if (topActivity == null || topActivity.getWindow() == null) {
            return 0;
        }
        WindowInsets insets = topActivity.getWindow().getDecorView().getRootWindowInsets();
        if (insets == null) {
            return 0;
        }
        int insetBottom = insets.getSystemWindowInsetBottom();

        // For Android 15+ (API 35+), check edge-to-edge layout config
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            config != null && Boolean.TRUE.equals(config.shouldRespectEdgeToEdgeLayout())) {
            return 0; // respect edge-to-edge = extend to very bottom
        }

        return insetBottom;
    }
}
