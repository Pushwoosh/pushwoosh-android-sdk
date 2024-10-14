package com.pushwoosh.inapp.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.event.ActivityBroughtOnTopEvent;
import com.pushwoosh.inapp.event.RichMediaCloseEvent;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;
import com.pushwoosh.inapp.view.js.PushwooshJSInterface;
import com.pushwoosh.inapp.view.utils.ModalRichMediaWindowUtils;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

import java.lang.ref.WeakReference;

public class ModalRichMediaWindow extends PopupWindow implements InAppView, DownloadHtmlTask.DownloadListener, OnRichMediaListener {
    private final String TAG = "[InApp] ModalRichMediaWindow";
    private int mode;
    protected Resource resource;
    private ModalRichmediaConfig config;

    private int topInset = 0;
    private int bottomInset = 0;


    private HtmlData htmlData;
    private AsyncTask<Void, Void, Result<HtmlData, ResourceParseException>> downloadHtmlDataTask;
    private WeakReference<OnRichMediaListener> onRichMediaListener = new WeakReference<>(null);

    @Nullable
    private ResourceWebView resourceWebView;

    @SuppressLint("ClickableViewAccessibility")
    public ModalRichMediaWindow(Context context, Resource resource, ModalRichmediaConfig config) {

        super(context);
        this.resource = resource;
        this.config = config;
        if (config.getWindowWidth() == ModalRichMediaWindowWidth.FULL_SCREEN) {
            this.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        } else if (config.getWindowWidth() == ModalRichMediaWindowWidth.WRAP_CONTENT) {
            this.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);
        this.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        this.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        this.setOnDismissListener(()-> {
            EventBus.sendEvent(new RichMediaCloseEvent(resource));
        });

        //set vertical insets depending on gravity
        if (config.getViewPosition() == ModalRichMediaViewPosition.TOP) {
            topInset = ModalRichMediaWindowUtils.getSystemWindowInsetTop();
        }
        if (config.getViewPosition() == ModalRichMediaViewPosition.BOTTOM) {
            bottomInset = ModalRichMediaWindowUtils.getSystemWindowInsetBottom();
        }


        //Dragging and swiping
        this.setClippingEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.setTouchModal(false);
        }
        this.setTouchInterceptor(new View.OnTouchListener() {
            private float startX;
            private float startY;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        startX = motionEvent.getRawX();
                        startY = motionEvent.getRawY();

                        if (!ModalRichMediaWindowUtils.isTouchInsidePopupWindow(ModalRichMediaWindow.this, motionEvent)) {
                            return true;
                        }
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        float dx = motionEvent.getRawX() - startX;
                        float dy = motionEvent.getRawY() - startY;
                        ModalRichMediaWindowUtils.movePopupOnDragEvent(ModalRichMediaWindow.this, (int) dx, (int) dy, config);
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        float endX = motionEvent.getRawX();
                        float endY = motionEvent.getRawY();
                        if (!ModalRichMediaWindowUtils.dismissOnSwipeThreshold(ModalRichMediaWindow.this, endX-startX, endY - startY, config)) {
                            ModalRichMediaWindowUtils.movePopupOnDragEvent(ModalRichMediaWindow.this, 0,0, config);
                        }
                        break;
                    }
                }
                return false;

            }
        });

        this.onRichMediaListener = new WeakReference<>(this);

        // Initialize resourceWebView
        resourceWebView = new ResourceWebView(context);
        resourceWebView.setWebViewClient(new WebClient(this, resource));
        downloadHtmlData(resource);
        setContentView(resourceWebView);

    }

    public static void showModalRichMediaWindow(Resource resource, ModalRichmediaConfig config) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(()->{
            if (PushwooshPlatform.getInstance().getTopActivity() == null) {
                EventBus.subscribe(ActivityBroughtOnTopEvent.class, new EventListener<ActivityBroughtOnTopEvent>() {
                    @Override
                    public void onReceive(ActivityBroughtOnTopEvent event) {
                        if (event.count.get() <= 1) {
                            createPopupWindow(resource, config);
                        }
                        EventBus.unsubscribe(ActivityBroughtOnTopEvent.class, this);
                        ActivityBroughtOnTopEvent.resetCount();
                    }
                });
            } else {
                createPopupWindow(resource, config);
            }
        }, 1000);
    }

    private static void createPopupWindow(Resource resource, ModalRichmediaConfig config) {
        Activity topActivity = PushwooshPlatform.getInstance().getTopActivity();
        View parentView = ModalRichMediaWindowUtils.getParentView();
        if (parentView.getWindowToken() != null) {
            //window itself will be shown later when resource web view is loaded
            ModalRichMediaWindow popupWindow = new ModalRichMediaWindow(topActivity, resource, config);
        }
    }

    public void downloadHtmlData(final Resource inApp) {
        downloadHtmlDataTask = new DownloadHtmlTask(inApp, this);
        downloadHtmlDataTask.execute();
    }

    //InAppView override methods
    @Override
    public void close() {
        if (resourceWebView != null) {
            ModalRichMediaWindow window = this;
            ValueAnimator animator = ModalRichMediaWindowUtils.getDismissValueAnimatorForWindow(window, config);
            animator.setDuration(config.getAnimationDuration());
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animator) {

                }

                @Override
                public void onAnimationEnd(@NonNull Animator animator) {
                    PWLog.debug("animation ended");
                    window.dismiss();
                }

                @Override
                public void onAnimationCancel(@NonNull Animator animator) {

                }

                @Override
                public void onAnimationRepeat(@NonNull Animator animator) {

                }
            });
            animator.start();
            resourceWebView.clear();
            resourceWebView = null;
        } else {
            this.dismiss();
        }
    }

    @Override
    public void onPageLoaded() {
        if (resourceWebView != null) {
            resourceWebView.hideProgress();

            int xCoordinate = ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionX(config);
            int yCoordinate = ModalRichMediaWindowUtils.getModalRichMediaWindowShowPositionY(config);
            int gravity = ModalRichMediaWindowUtils.getModalRichMediaWindowGravity(config);

            this.showAtLocation(ModalRichMediaWindowUtils.getParentView(), gravity, xCoordinate, yCoordinate);
            ValueAnimator animator = ModalRichMediaWindowUtils.getPresentValueAnimatorForWindow(this, config);
            animator.setDuration(config.getAnimationDuration());
            animator.start();
            EventBus.sendEvent(new InAppViewEvent(resource));
        }
    }

    @Override
    public int getMode() {
            return mode;
    }

    //DownloadHtmlTask.DownloadListener override methods
    @Override
    public void startLoading() { final com.pushwoosh.inapp.view.OnRichMediaListener onRichMediaListener = this.onRichMediaListener.get();
        if (onRichMediaListener != null) {
            onRichMediaListener.startLoadingRichMedia();
        }
    }

    @Override
    public void sendResult(Result<HtmlData, ResourceParseException> result) {
        notifyListener(result);
    }

    private void notifyListener(Result<HtmlData, ResourceParseException> result) {
        OnRichMediaListener onRichMediaListener = this.onRichMediaListener.get();
        if (onRichMediaListener == null) {
            return;
        }

        if (result.isSuccess()) {
            if (!onRichMediaListener.successLoadingHtmlData(result.getData())) {
                onRichMediaListener.finishLoadingRichMedia();
            }
        } else {
            onRichMediaListener.finishLoadingRichMedia();
            onRichMediaListener.failedLoadingHtmlData(result.getException());
        }
    }

    // OnRichMediaListener override methods

    @Override
    public void startLoadingRichMedia() {
        if (resourceWebView != null) {
            resourceWebView.showProgress();
        }
    }

    @Override
    public void finishLoadingRichMedia() {
        if (resourceWebView != null) {
            resourceWebView.hideProgress();
        }
    }

    @Override
    public boolean successLoadingHtmlData(HtmlData htmlData) {
        if (resourceWebView == null || htmlData.equals(this.htmlData)) {
            return false;
        }

        this.htmlData = htmlData;

        String htmlContent = htmlData.getHtmlContent();
        String baseUrl = htmlData.getUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        resourceWebView.loadDataWithBaseURL(baseUrl, htmlContentWithPushwooshInterface(htmlContent), "text/html", "UTF-8", null);
        return true;
    }

    @Override
    public void failedLoadingHtmlData(ResourceParseException exception) {
        PWLog.error(TAG, "Failed loading html data", exception);
        if (resource.isInApp()) {
            EventBus.sendEvent(new InAppViewFailedEvent(resource));
        }
        close();
    }

    public int getTopInset() {
        return topInset;
    }

    public int getBottomInset() {
        return bottomInset;
    }

    private String htmlContentWithPushwooshInterface(String content) {
        String messageHash = RepositoryModule.getNotificationPreferences().messageHash().get();
        String jsInterface = String.format(PushwooshJSInterface.PUSHWOOSH_JS,
                Pushwoosh.getInstance().getHwid(),
                GeneralUtils.SDK_VERSION,
                Pushwoosh.getInstance().getApplicationCode(),
                Pushwoosh.getInstance().getUserId(),
                !resource.isInApp() ? resource.getCode().substring(2) : "",
                DeviceSpecificProvider.getInstance().deviceType(),
                messageHash != null ? messageHash : "",
                resource.isInApp() ? resource.getCode() : ""
        );
        return content.replace("<head>", "<head>\n<script type=\"text/javascript\">" + jsInterface + "</script>");
    }
}
