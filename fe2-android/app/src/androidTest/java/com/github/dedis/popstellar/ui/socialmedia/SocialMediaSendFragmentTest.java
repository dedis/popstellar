package com.github.dedis.popstellar.ui.socialmedia;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.pages.socialmedia.SocialMediaSendPageObject.entryBoxChirpText;
import static com.github.dedis.popstellar.pages.socialmedia.SocialMediaSendPageObject.sendChirpButton;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class SocialMediaSendFragmentTest {

  private final FragmentScenarioRule<SocialMediaSendFragment> fragmentRule =
      FragmentScenarioRule.launch(SocialMediaSendFragment.class);

  private final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule public final RuleChain chain = RuleChain.outerRule(hiltRule).around(fragmentRule);

  @Test
  public void writingTextInEditText() {
    entryBoxChirpText().perform(typeText("Testing")).check(matches(withText("Testing")));
  }

  @Test
  public void sendButtonHasCorrectText() {
    sendChirpButton().check(matches(withText(R.string.send)));
  }

  @Test
  public void sendButtonIsDisplayed() {
    sendChirpButton().check(matches(isDisplayed()));
  }

  @Test
  public void sendButtonIsClickable() {
    sendChirpButton().check(matches(isClickable()));
  }

  @Test
  public void sendButtonIsDisabled() {
    entryBoxChirpText()
        .perform(
            typeText(
                "This text should be way over three hundred characters which is the current limit of the"
                    + " text within a chirp, and if I try to set the chirp's text to this, it  should"
                    + " throw and IllegalArgumentException() so I hope it does otherwise I might have"
                    + " screwed something up. But normally it is not that hard to write enough to reach"
                    + " the threshold."));
    sendChirpButton().perform(click());
    sendChirpButton().check(matches(isNotEnabled()));
  }
}
