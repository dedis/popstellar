package com.github.dedis.popstellar.ui.socialmedia;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.socialmedia.SocialMediaSendFragment;
import com.github.dedis.popstellar.utility.Constants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static com.github.dedis.popstellar.testutils.pages.socialmedia.SocialMediaSendPageObject.entryBoxChirpText;
import static com.github.dedis.popstellar.testutils.pages.socialmedia.SocialMediaSendPageObject.sendChirpButton;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class SocialMediaSendFragmentTest {

  private static final long CREATION_TIME = 1631280815;
  private static final String LAO_NAME = "laoName";

  private static final KeyPair SENDER_KEY_1 = generatePoPToken();
  private static final KeyPair SENDER_KEY_2 = generatePoPToken();

  private static final PublicKey SENDER_1 = SENDER_KEY_1.getPublicKey();
  private static final PublicKey SENDER_2 = SENDER_KEY_2.getPublicKey();
  private static final String LAO_ID = Lao.generateLaoId(SENDER_1, CREATION_TIME, LAO_NAME);

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          hiltRule.inject();
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, SocialMediaSendFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO_ID).build(),
          LaoActivityPageObject.containerId(),
          SocialMediaSendFragment.class,
          SocialMediaSendFragment::new);

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
