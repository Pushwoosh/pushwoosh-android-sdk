package com.pushwoosh.inapp.view.inline;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.pushwoosh.R;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.InAppView;
import com.pushwoosh.inapp.view.ResourceWebView;
import com.pushwoosh.inapp.view.WebClient;

import java.util.ArrayList;
import java.util.List;


public class InlineInAppView extends ResourceWebView {
    enum State {
        LOADING, //initial state
        LOADED, //when inapp loaded and ready for display
        RENDERED, //inapp displayed
        CLOSED
    }

    private String identifier;
    private boolean disableLayoutAnimation;

    private State state;
    private boolean isFixedSize;

    private InlineInAppViewLayoutHelperBase layoutHelper;
    private InlineInAppViewDataHelper dataHelper;
    private List<InlineInAppViewListener> listeners = new ArrayList<>();

    public InlineInAppView(@NonNull Context context) {
        super(context);
        init();
    }

    public InlineInAppView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.InlineInAppView);
        identifier = ta.getString(R.styleable.InlineInAppView_identifier);
        disableLayoutAnimation = ta.getBoolean(R.styleable.InlineInAppView_disableLayoutAnimation, false);
        ta.recycle();
        init();
    }

    private void init() {
        InlineInAppViewAnimationHelper inlineInAppViewAnimationHelper = new InlineInAppViewAnimationHelper(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            layoutHelper = new InlineInAppViewLayoutHelperApi1(this, inlineInAppViewAnimationHelper);
        } else {
            layoutHelper = new InlineInAppViewLayoutHelperApi19(this, inlineInAppViewAnimationHelper);
        }
        dataHelper = new InlineInAppViewDataHelper(this, InAppModule.getInAppRepository());
        state = State.LOADING;
        dataHelper.requestData(identifier);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!listeners.isEmpty()) {
            for (InlineInAppViewListener listener : listeners) {
                if (listener != null) {
                    listener.onInlineInAppViewChangedSize(w, h);
                }
            }
        }
    }

    @Override
    protected WebView createWebView() {
        //disable scroll
        return new WebView(getContext()) {
            @Override
            public boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY,
                                        int scrollRangeX, int scrollRangeY, int maxOverScrollX,
                                        int maxOverScrollY, boolean isTouchEvent) {
                return false;
            }

            @Override
            public void scrollTo(int x, int y) { }

            @Override
            public void computeScroll() { }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                layoutHelper.onWebViewLayout(changed, l, t, r, b);
            }
        };
    }

    @Override
    protected void initWebView() {
        super.initWebView();
        webView.setScrollContainer(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
    }

    @Override
    protected void animateOpen() {
    }

    class InAppViewImplementation implements InAppView{
        @Override
        public void onPageLoaded() {
            if (state != State.LOADING)
                return;
            setState(State.LOADED);
        }

        @Override
        public int getMode() {
            return 0;
        }

        @Override
        public void close() {
            setState(State.CLOSED);
        }
    }

    void resourceUpdated(Resource resource) {
        WebClient client = new WebClient(new InAppViewImplementation(), resource);
        webView.setWebViewClient(client);
        client.attachMainContainer(this);
        client.attachToWebView(webView);
    }

    void htmlDataLoaded(HtmlData data) {
        loadData(data);
    }

    void setState(State state) {
        if (state != this.state) {
            this.state = state;
            if (state == State.RENDERED) {
                onRendered();
            }
            if (state == State.CLOSED) {
                onClosed();
            }
            layoutHelper.stateChanged(state);
        }
    }

    private void onRendered() {
        dataHelper.sendInAppEvent();
        if (!listeners.isEmpty()) {
            for (InlineInAppViewListener listener : listeners) {
                if (listener != null) {
                    listener.onInlineInAppLoaded();
                }
            }
        }
    }

    private void onClosed() {
        if (!listeners.isEmpty()) {
            for (InlineInAppViewListener listener : listeners) {
                if (listener != null) {
                    listener.onInlineInAppViewClosed();
                }
            }
        }
    }

    State getState() {
        return state;
    }

    WebView getWebView() {
        return webView;
    }

    FrameLayout getContainer() {
        return container;
    }

    boolean isFixedSize() {
        return isFixedSize;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        if (!(this.identifier == null ? identifier == null : this.identifier.equals(identifier))) {
            this.identifier = identifier;
            dataHelper.requestData(identifier);
        }
    }

    public boolean isLayoutAnimationDisabled() {
        return disableLayoutAnimation;
    }

    /**
     * Disable layout animation
     *
     * @param disableLayoutAnimation flag
     */
    public void setDisableLayoutAnimation(boolean disableLayoutAnimation) {
        this.disableLayoutAnimation = disableLayoutAnimation;
    }

    /**
     * Add a listener that will be called when state or bounds of the view
     * change.
     *
     * @param listener The listener for state and bounds change.
     */
    public void addInlineInAppViewListener(InlineInAppViewListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a listener for state or bounds changes.
     *
     * @param listener The listener for state and bounds change.
     */
    public void removeInlineInAppViewListener(InlineInAppViewListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        isFixedSize = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY &&
                MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        layoutHelper.onMeasure(widthMeasureSpec, heightMeasureSpec,
                (width, height) -> setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec)));
    }

    @NonNull
    @Override
    protected LayoutParams createWebViewParams(InAppLayout mode, int topMargin) {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        layoutHelper.onConfigurationChanged(newConfig);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.closed = state == State.CLOSED;
        savedState.dateHelperSavedState = dataHelper.getSaveState();
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState st = (SavedState)state;
            if (st.closed) {
                this.state = State.CLOSED;
            }
            dataHelper.applySavedState(st.dateHelperSavedState);
            super.onRestoreInstanceState(st.getSuperState());
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    static class SavedState extends BaseSavedState {
        boolean closed;
        InlineInAppViewDataHelper.SavedState dateHelperSavedState;

        private SavedState(Parcel source) {
            super(source);
            closed = source.readInt() == 1;
            dateHelperSavedState = new InlineInAppViewDataHelper.SavedState(source);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(closed ? 1 : 0);
            dateHelperSavedState.save(out);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
