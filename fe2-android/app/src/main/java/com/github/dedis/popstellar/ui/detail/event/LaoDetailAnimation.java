package com.github.dedis.popstellar.ui.detail.event;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.animation.*;

/**
 * Heavily inspired by
 * https://betterprogramming.pub/animated-fab-button-with-more-options-2dcf7118fff6
 */
public class LaoDetailAnimation {

  private LaoDetailAnimation() {}

  public static boolean rotateFab(final View v, boolean rotate) {
    v.animate().setDuration(200).rotation(rotate ? 135f : 0f);
    return rotate;
  }

  public static void rotateExpand(final View view, boolean rotate) {
    view.animate().setDuration(300).rotation(rotate ? 180f : 0f);
  }

  public static void showIn(final View v) {
    v.setVisibility(View.VISIBLE);
    v.setAlpha(0f);
    v.setTranslationY(v.getHeight());
    v.animate()
        .setDuration(200)
        .translationY(0)
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                v.setAlpha(1f);
                super.onAnimationEnd(animation);
              }
            })
        .alpha(1f)
        .start();
  }

  public static void showOut(final View v) {
    v.setVisibility(View.VISIBLE);
    v.setAlpha(1f);
    v.setTranslationY(0);
    v.animate()
        .setDuration(200)
        .translationY(v.getHeight())
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                v.setVisibility(View.GONE);
                v.setAlpha(0f);
                super.onAnimationEnd(animation);
              }
            })
        .alpha(0f)
        .start();
  }

  public static void fadeIn(final View v, float from, float to, long duration) {
    Animation animation = new AlphaAnimation(from, to);
    animation.setInterpolator(new DecelerateInterpolator());
    animation.setDuration(duration);
    animation.setAnimationListener(fadeListener(v, to));
    v.startAnimation(animation);
  }

  public static void fadeOut(final View v, float from, float to, long duration) {
    Animation animation = new AlphaAnimation(from, to);
    animation.setInterpolator(new AccelerateInterpolator());
    animation.setDuration(duration);
    animation.setAnimationListener(fadeListener(v, to));
    v.startAnimation(animation);
  }

  private static Animation.AnimationListener fadeListener(View v, float to) {
    return new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
        // Do nothing
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        v.setAlpha(to);
        v.setVisibility(to == 1.0f ? View.VISIBLE : View.GONE);
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
        // Do nothing
      }
    };
  }
}
