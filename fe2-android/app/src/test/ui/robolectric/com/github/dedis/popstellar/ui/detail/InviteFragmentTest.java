package com.github.dedis.popstellar.ui.detail;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.lao.InviteFragment;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.pages.detail.InviteFragmentPageObject.*;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.containerId;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static org.mockito.Mockito.when;

@SmallTest
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class InviteFragmentTest {
  private static final String LAO_NAME = "LAO";
  private static final KeyPair KEY_PAIR = Base64DataUtils.generateKeyPair();
  private static final PublicKey PK = KEY_PAIR.getPublicKey();

  private static final Lao LAO = new Lao(LAO_NAME, PK, 44444444);

  @Inject LAORepository laoRepository;

  @BindValue @Mock KeyManager keyManager;

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

          laoRepository.updateLao(LAO);

          when(keyManager.getMainKeyPair()).thenReturn(KEY_PAIR);
          when(keyManager.getMainPublicKey()).thenReturn(PK);
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, InviteFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(laoIdExtra(), LAO.getId()).build(),
          containerId(),
          InviteFragment.class,
          InviteFragment::newInstance);

  @Test
  public void displayedInfoIsCorrect() {
    roleText().check(matches(withText("Organizer")));
    laoNameText().check(matches(withText(LAO_NAME)));
    identifierText().check(matches(withText(PK.getEncoded())));
  }
}
