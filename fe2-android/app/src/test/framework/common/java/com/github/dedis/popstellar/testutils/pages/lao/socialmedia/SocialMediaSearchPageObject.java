package com.github.dedis.popstellar.testutils.pages.lao.socialmedia;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * This is the page object of SocialMediaSearchFragment
 *
 * <p>It makes writing test easier
 */
public class SocialMediaSearchPageObject {

  private SocialMediaSearchPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction getRootView() {
    return onView(isRoot());
  }

  public static ViewInteraction getSocialMediaHomeFragment() {
    return onView(withId(R.id.fragment_social_media_home));
  }
}
