package com.github.dedis.popstellar.testutils.pages.lao.socialmedia;

import androidx.annotation.StringRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * This is the page object of SocialMediaSendFragment
 *
 * <p>It makes writing test easier
 */
public class SocialMediaSendPageObject {

  private SocialMediaSendPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction sendChirpButton() {
    return onView(withId(R.id.send_chirp_button));
  }

  public static ViewInteraction entryBoxChirpText() {
    return onView(withId(R.id.entry_box_chirp));
  }

  @StringRes
  public static int nullLaoIdToastText() {
    return R.string.error_no_lao;
  }
}
