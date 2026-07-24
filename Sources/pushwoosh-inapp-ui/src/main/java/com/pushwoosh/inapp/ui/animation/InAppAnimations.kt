package com.pushwoosh.inapp.ui.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

/**
 * Entrance/exit animations shared by the templates. Each scale/slide variant takes a
 * reduce-motion flag — when set, the transform is dropped and only opacity animates.
 */
internal object InAppAnimations {

    private const val IN_MS = 300L
    private const val OUT_MS = 220L

    fun fadeIn(view: View) {
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(IN_MS).start()
    }

    fun fadeOut(view: View, onEnd: () -> Unit) {
        view.animate().alpha(0f).setDuration(OUT_MS).setListener(endListener(onEnd)).start()
    }

    fun scaleIn(view: View, reduceMotion: Boolean) {
        view.alpha = 0f
        if (reduceMotion) {
            view.animate().alpha(1f).setDuration(IN_MS).start()
            return
        }
        view.scaleX = 0.88f
        view.scaleY = 0.88f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(IN_MS).start()
    }

    fun scaleOut(view: View, reduceMotion: Boolean, onEnd: () -> Unit) {
        val animator = view.animate().alpha(0f).setDuration(OUT_MS)
        if (!reduceMotion) {
            animator.scaleX(0.92f).scaleY(0.92f)
        }
        animator.setListener(endListener(onEnd)).start()
    }

    fun slideIn(view: View, fromTranslationY: Float, reduceMotion: Boolean) {
        view.alpha = 0f
        if (reduceMotion) {
            view.animate().alpha(1f).setDuration(IN_MS).start()
            return
        }
        view.translationY = fromTranslationY
        view.animate().alpha(1f).translationY(0f).setDuration(IN_MS).start()
    }

    fun slideOut(view: View, toTranslationY: Float, reduceMotion: Boolean, onEnd: () -> Unit) {
        val animator = view.animate().alpha(0f).setDuration(OUT_MS)
        if (!reduceMotion) {
            animator.translationY(toTranslationY)
        }
        animator.setListener(endListener(onEnd)).start()
    }

    private fun endListener(onEnd: () -> Unit) = object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) = onEnd()
    }
}
