package com.github.dedis.popstellar.testutils;

import android.view.*;

import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.viewpager2.widget.ViewPager2;

import org.hamcrest.*;

public class MatcherUtils {

  private MatcherUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static Matcher<View> childAtPosition(
      final Matcher<View> parentMatcher, final int position) {

    return new TypeSafeMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Child at position " + position + " in parent ");
        parentMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        ViewParent parent = view.getParent();
        return parent instanceof ViewGroup
            && parentMatcher.matches(parent)
            && view.equals(((ViewGroup) parent).getChildAt(position));
      }
    };
  }

  public static Matcher<View> PageMatcher(final int position) {
    return new BoundedMatcher<View, ViewPager2>(ViewPager2.class) {
      @Override
      public void describeTo(Description description) {
        description.appendText("is at position " + position + " in ViewPager");
      }

      @Override
      protected boolean matchesSafely(ViewPager2 viewPager) {
        return viewPager.getCurrentItem() == position;
      }
    };
  }
}
