package com.github.dedis.popstellar.ui.lao.event

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator

/**
 * Heavily inspired by
 * https://betterprogramming.pub/animated-fab-button-with-more-options-2dcf7118fff6
 */
object LaoDetailAnimation {
  @JvmStatic
  fun rotateFab(v: View, rotate: Boolean): Boolean {
    v.animate().setDuration(200).rotation(if (rotate) 135f else 0f)
    return rotate
  }

  @JvmStatic
  fun rotateExpand(view: View, rotate: Boolean) {
    view.animate().setDuration(300).rotation(if (rotate) 180f else 0f)
  }

  @JvmStatic
  fun showIn(v: View) {
    v.visibility = View.VISIBLE
    v.alpha = 0f
    v.translationY = v.height.toFloat()

    v.animate()
        .setDuration(200)
        .translationY(0f)
        .setListener(
            object : AnimatorListenerAdapter() {
              override fun onAnimationEnd(animation: Animator) {
                v.alpha = 1f
                super.onAnimationEnd(animation)
              }
            })
        .alpha(1f)
        .start()
  }

  @JvmStatic
  fun showOut(v: View) {
    v.visibility = View.VISIBLE
    v.alpha = 1f
    v.translationY = 0f

    v.animate()
        .setDuration(200)
        .translationY(v.height.toFloat())
        .setListener(
            object : AnimatorListenerAdapter() {
              override fun onAnimationEnd(animation: Animator) {
                v.visibility = View.GONE
                v.alpha = 0f
                super.onAnimationEnd(animation)
              }
            })
        .alpha(0f)
        .start()
  }

  @JvmStatic
  fun fadeIn(v: View, from: Float, to: Float, duration: Long) {
    val animation: Animation = AlphaAnimation(from, to)
    animation.interpolator = DecelerateInterpolator()
    animation.duration = duration
    animation.setAnimationListener(fadeListener(v, to))

    v.startAnimation(animation)
  }

  @JvmStatic
  fun fadeOut(v: View, from: Float, to: Float, duration: Long) {
    val animation: Animation = AlphaAnimation(from, to)
    animation.interpolator = AccelerateInterpolator()
    animation.duration = duration
    animation.setAnimationListener(fadeListener(v, to))

    v.startAnimation(animation)
  }

  private fun fadeListener(v: View, to: Float): Animation.AnimationListener {
    return object : Animation.AnimationListener {
      override fun onAnimationStart(animation: Animation) {
        // Do nothing
      }

      override fun onAnimationEnd(animation: Animation) {
        v.alpha = to
        v.visibility = if (to == 1.0f) View.VISIBLE else View.GONE
      }

      override fun onAnimationRepeat(animation: Animation) {
        // Do nothing
      }
    }
  }
}
