package com.github.dedis.popstellar.ui.lao.token;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.HashSet;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.containerId;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static com.github.dedis.popstellar.testutils.pages.lao.token.TokenPageObject.tokenTextView;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class TokenFragmentTest {

  private static final String LAO_NAME = "lao";
  private static final KeyPair USER_KEY_PAIR = generateKeyPair();
  private static final PublicKey USER = USER_KEY_PAIR.getPublicKey();
  private static final PoPToken USER_TOKEN = generatePoPToken();
  private static final Lao LAO = new Lao(LAO_NAME, generateKeyPair().getPublicKey(), 10223421);
  private static final String LAO_ID = LAO.getId();
  private static final String ROLL_CALL_TITLE = "RC title";
  private static final long CREATION = 10323411;
  private static final long ROLL_CALL_START = 10323421;
  private static final long ROLL_CALL_END = 10323431;
  private static final String ROLL_CALL_DESC = "";
  private static final String LOCATION = "EPFL";
  private static final BehaviorSubject<LaoView> laoSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));

  private final RollCall ROLL_CALL =
      new RollCall(
          LAO.getId(),
          LAO.getId(),
          ROLL_CALL_TITLE,
          CREATION,
          ROLL_CALL_START,
          ROLL_CALL_END,
          EventState.CLOSED,
          new HashSet<>(),
          LOCATION,
          ROLL_CALL_DESC);

  @Inject RollCallRepository rollCallRepo;

  @BindValue @Mock LAORepository repository;
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
        protected void before() throws UnknownLaoException, KeyException {
          hiltRule.inject();
          when(repository.getLaoObservable(anyString())).thenReturn(laoSubject);
          when(repository.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          when(keyManager.getMainPublicKey()).thenReturn(USER);

          rollCallRepo.updateRollCall(LAO_ID, ROLL_CALL);

          when(keyManager.getValidPoPToken(any(), any())).thenReturn(USER_TOKEN);
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, TokenFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(laoIdExtra(), LAO_ID).build(),
          containerId(),
          TokenFragment.class,
          () -> TokenFragment.newInstance(ROLL_CALL.getPersistentId()));

  @Test
  public void tokenIsDisplayed() {
    tokenTextView().check(matches(withText(USER_TOKEN.getPublicKey().getEncoded())));
  }

  @Test
  public void testBackButtonBehaviour() {
    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              pressBack();
              // Check that we are now in the expected fragment
              FragmentManager fragmentManager = activity.getSupportFragmentManager();
              Fragment currentFragment =
                  fragmentManager.findFragmentById(R.id.fragment_container_lao);
              assertTrue(currentFragment instanceof TokenListFragment);
            });
  }
}
