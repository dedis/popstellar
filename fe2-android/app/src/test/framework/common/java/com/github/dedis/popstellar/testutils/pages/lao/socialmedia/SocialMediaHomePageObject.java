package com.github.dedis.popstellar.testutils.pages.lao.socialmedia;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * This is the page object of SocialMediaHomeFragment
 *
 * <p>It makes writing test easier
 */
public class SocialMediaHomePageObject {

  private SocialMediaHomePageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction getRootView() {
    return onView(isRoot());
  }

  public static ViewInteraction getEventListFragment() {
    return onView(withId(R.id.fragment_event_list));
  }

  public static ViewInteraction getHomeFeedFragment() {
    return onView(withId(R.id.fragment_social_media_home));
  }

  public static ViewInteraction getFollowingFragment() {
    return onView(withId(R.id.fragment_social_media_following));
  }

    public static ViewInteraction getProfileFragment() {
        return onView(withId(R.id.fragment_social_media_profile));
    }

    public static ViewInteraction getSearchFragment() {
        return onView(withId(R.id.fragment_social_media_search));
    }

    public static ViewInteraction getSocialMediaFragment() {
        return onView(withId(R.id.main_menu_social_media));
    }

  public static ViewInteraction getAddChirpButton() {
    return onView(withId(R.id.social_media_send_fragment_button));
  }

  public static ViewInteraction getProfileButton() {
    return onView(withId(R.id.social_media_profile_menu));
  }

    public static ViewInteraction getSearchButton() {
        return onView(withId(R.id.social_media_search_menu));
    }

    public static ViewInteraction getFollowingButton() {
        return onView(withId(R.id.social_media_following_menu));
    }

    public static ViewInteraction getHomeButton() {
        return onView(withId(R.id.social_media_home_menu));
    }
}
