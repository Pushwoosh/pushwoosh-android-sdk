package com.pushwoosh.inapp.view.inline;

import android.animation.LayoutTransition;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

class InlineInAppViewAnimationHelper {
    private static final int ANIMATION_DURATION = 300;

    private InlineInAppView inappView;

    public InlineInAppViewAnimationHelper(InlineInAppView view) {
        this.inappView = view;
    }

    public void addTransition() {
        if (inappView.isFixedSize()) {
            addFadeAnimation();
        } else {
            if (!inappView.isLayoutAnimationDisabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && inappView.getParent() instanceof ViewGroup) {
                final ViewGroup parent = (ViewGroup) inappView.getParent();
                if (parent.getLayoutTransition() == null) {
                    final LayoutTransition transition = createTransition();
                    transition.addTransitionListener(new LayoutTransition.TransitionListener() {
                        @Override
                        public void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
                            if (view == inappView) {
                                addFadeAnimation();
                            }
                        }

                        @Override
                        public void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
                            if (view == inappView) {
                                parent.setLayoutTransition(null);
                                removeAllTransitions();
                            }
                        }
                    });
                    parent.setLayoutTransition(transition);

                    ViewParent mainParent = parent;
                    while (mainParent.getParent() instanceof ViewGroup)
                        mainParent = mainParent.getParent();

                    recursiveAddTransition((ViewGroup)mainParent);
                }
            }
        }
    }

    private ArrayList<WeakReference<ViewGroup>> transitionViewGroups = new ArrayList<>();

    private void removeAllTransitions() {
        for (WeakReference<ViewGroup> viewGroup : transitionViewGroups) {
            if (viewGroup.get() != null) {
                LayoutTransition transition = viewGroup.get().getLayoutTransition();
                if (transition instanceof InlineInAppViewLayoutTransition) {
                    viewGroup.get().setLayoutTransition(null);
                }
            }
        }
        transitionViewGroups.clear();
    }

    private static class InlineInAppViewLayoutTransition extends LayoutTransition {}

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private LayoutTransition createTransition() {
        LayoutTransition transition = new InlineInAppViewLayoutTransition();
        transition.setDuration(ANIMATION_DURATION);
        transition.enableTransitionType(LayoutTransition.CHANGING);
        return transition;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void recursiveAddTransition(ViewGroup target) {
        for (int i = 0; i < target.getChildCount(); ++i) {
            View child = target.getChildAt(i);
            if (child != inappView && child instanceof ViewGroup && ((ViewGroup) child).getLayoutTransition() == null) {
                ((ViewGroup)child).setLayoutTransition(createTransition());
                transitionViewGroups.add(new WeakReference<>((ViewGroup)child));
                recursiveAddTransition((ViewGroup)child);
            }
        }
    }

    private void addFadeAnimation() {
        inappView.getContainer().setAlpha(inappView.getState() == InlineInAppView.State.CLOSED ? 1 : 0);
        inappView.getContainer().animate().alpha(inappView.getState() == InlineInAppView.State.CLOSED ? 0 : 1).setDuration(ANIMATION_DURATION).start();
    }
}
