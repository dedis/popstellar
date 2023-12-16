package com.github.dedis.popstellar.ui.lao.token;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.containerId;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static com.github.dedis.popstellar.testutils.pages.lao.token.TokenListPageObject.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.RollCallBuilder;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.*;
import com.github.dedis.popstellar.utility.security.KeyManager;
import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class TokenListFragmentTest {

  private static final String LAO_NAME = "lao";
  private static final KeyPair USER_KEY_PAIR = generateKeyPair();
  private static final PublicKey USER = USER_KEY_PAIR.publicKey;
  private static final PoPToken USER_TOKEN = generatePoPToken();
  private static final Lao LAO = new Lao(LAO_NAME, generateKeyPair().publicKey, 10223421);
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
          EventState.CREATED,
          new HashSet<>(),
          LOCATION,
          ROLL_CALL_DESC);

  @Inject RollCallRepository rollCallRepo;

  @BindValue @Mock LAORepository repository;
  @BindValue @Mock KeyManager keyManager;

  @BindValue @Mock Wallet wallet;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before()
            throws UnknownLaoException,
                GeneralSecurityException,
                KeyGenerationException,
                UninitializedWalletException {
          hiltRule.inject();
          when(repository.getLaoObservable(anyString())).thenReturn(laoSubject);
          when(repository.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          when(keyManager.getMainPublicKey()).thenReturn(USER);

          when(wallet.exportSeed())
              .thenReturn(
                  new String[] {
                    "jar",
                    "together",
                    "minor",
                    "alley",
                    "glow",
                    "hybrid",
                    "village",
                    "creek",
                    "meadow",
                    "atom",
                    "travel",
                    "bracket"
                  });

          when(wallet.generatePoPToken(any(), any())).thenReturn(USER_TOKEN);
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, TokenListFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(laoIdExtra(), LAO_ID).build(),
          containerId(),
          TokenListFragment.class,
          TokenListFragment::newInstance);

  @Test
  public void noRollCallDisplaysExplanationMessage() {
    checkExplanationMessageIsDisplayed();
  }

  @Test
  public void noClosedRollCallDisplaysExplanationMessage() {
    setRollCalls(ROLL_CALL);
    checkExplanationMessageIsDisplayed();
  }

  @Test
  public void noRollCallAttendedDisplaysExplanationMessage() {
    RollCall closedRollCallWithoutUser = RollCall.closeRollCall(ROLL_CALL);
    setRollCalls(closedRollCallWithoutUser);
    checkExplanationMessageIsDisplayed();
  }

  @Test
  public void havingAttendedClosedRollCallDisplayValidToken() throws NoRollCallException {
    RollCall closedRollCallWithUser =
        new RollCallBuilder(ROLL_CALL)
            .setState(EventState.CLOSED)
            .setAttendees(Collections.singleton(USER_TOKEN.publicKey))
            .build();
    setRollCalls(closedRollCallWithUser);

    emptyTokenText().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    invalidTokensRv().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    validTokenCard().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
  }

  @Test
  public void havingAttendedTwoClosedRollCallDisplayValidToken() throws NoRollCallException {
    RollCall closedRollCallWithUser1 =
        new RollCallBuilder(ROLL_CALL)
            .setState(EventState.CLOSED)
            .setCreation(1000)
            .setStart(1000)
            .setEnd(1000)
            .setPersistentId("some ridiculous id")
            .setAttendees(Collections.singleton(USER_TOKEN.publicKey))
            .build();
    RollCall closedRollCallWithUser2 =
        new RollCallBuilder(ROLL_CALL)
            .setState(EventState.CLOSED)
            .setAttendees(Collections.emptySet())
            .build();

    setRollCalls(closedRollCallWithUser1, closedRollCallWithUser2);

    emptyTokenText().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    invalidTokensRv().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    validTokenCard().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
  }

  private void setRollCalls(RollCall... rollCalls) {
    for (RollCall rollCall : rollCalls) {
      rollCallRepo.updateRollCall(LAO_ID, rollCall);
    }
  }

  private void checkExplanationMessageIsDisplayed() {
    emptyTokenText().check(matches(withText(R.string.empty_tokens_text)));
    validTokenCard().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    invalidTokensRv().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
  }
}
