package com.github.dedis.popstellar.ui.lao.witness;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.*;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.containerId;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessMessageFragmentPageObject.*;
import static org.hamcrest.CoreMatchers.anything;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class WitnessMessageFragmentTest {

  private static final String LAO_NAME = "lao";
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, SENDER, Instant.now().getEpochSecond());
  private static final String LAO_ID = LAO.getId();

  private static final MessageID MESSAGE_ID1 = Base64DataUtils.generateMessageID();
  private static final WitnessMessage WITNESS_MESSAGE = new WitnessMessage(MESSAGE_ID1);
  private static final String TITLE = "Message title";
  private static final String DESCRIPTION = "roll call name: test roll call";
  private static final String DESCRIPTION_TITLE = "Description";
  private static final String SIGNATURES_TITLE = "Signatures";

  private static final PublicKey WITNESS1 = Base64DataUtils.generatePublicKey();
  private static final PublicKey WITNESS2 = Base64DataUtils.generatePublicKeyOtherThan(WITNESS1);

  private static final String SIGNATURES_TEXT =
      WITNESS2.getEncoded() + "\n" + WITNESS1.getEncoded();

  private static final BehaviorSubject<LaoView> laoSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));

  private static final PoPToken POP_TOKEN = Base64DataUtils.generatePoPToken();
  private WitnessingViewModel witnessingViewModel;
  private List<WitnessMessage> witnessMessages;

  @BindValue @Mock LAORepository laoRepo;
  @BindValue @Mock GlobalNetworkManager networkManager;
  @BindValue @Mock KeyManager keyManager;

  MessageSenderHelper messageSenderHelper = new MessageSenderHelper();

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
          when(laoRepo.getLaoObservable(anyString())).thenReturn(laoSubject);
          when(laoRepo.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          when(keyManager.getMainPublicKey()).thenReturn(SENDER);

          when(networkManager.getMessageSender()).thenReturn(messageSenderHelper.getMockedSender());
          messageSenderHelper.setupMock();

          when(keyManager.getPoPToken(any(), any())).thenReturn(POP_TOKEN);

          WITNESS_MESSAGE.setTitle(TITLE);
          WITNESS_MESSAGE.setDescription(DESCRIPTION);
          WITNESS_MESSAGE.addWitness(WITNESS1);
          WITNESS_MESSAGE.addWitness(WITNESS2);
          witnessMessages = Collections.singletonList(WITNESS_MESSAGE);
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, WitnessMessageFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(laoIdExtra(), LAO_ID).build(),
          containerId(),
          WitnessMessageFragment.class,
          WitnessMessageFragment::new);

  @Test
  public void testWitnessMessageListDisplaysMessageTitle() {
    witnessingViewModel = new ViewModelProvider(getLaoActivity()).get(WitnessingViewModel.class);
    witnessingViewModel.setWitnessMessages(witnessMessages);

    witnessMessageList().check(matches(isDisplayed()));
    onData(anything())
        .inAdapterView(witnessMessageListMatcher())
        .atPosition(0)
        .check(matches(hasDescendant(withText(TITLE))));
  }

  @Test
  public void testWitnessMessageDescriptionDropdown() {
    witnessingViewModel = new ViewModelProvider(getLaoActivity()).get(WitnessingViewModel.class);
    witnessingViewModel.setWitnessMessages(witnessMessages);

    // Check that the description title is displayed
    onData(anything())
        .inAdapterView(witnessMessageListMatcher())
        .atPosition(0)
        .check(matches(hasDescendant(withText(DESCRIPTION_TITLE))));

    // Check that the message description is not displayed by default
    messageDescriptionText().check(matches(withEffectiveVisibility(Visibility.GONE)));

    // Click on the arrow to expand the text
    onData(anything())
        .inAdapterView(witnessMessageListMatcher())
        .atPosition(0)
        .onChildView(messageDescriptionArrowMatcher())
        .perform(click());

    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    // Check that correct message description is displayed
    messageDescriptionText().check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    onData(anything())
        .inAdapterView(witnessMessageListMatcher())
        .atPosition(0)
        .check(matches(hasDescendant(withText(DESCRIPTION))));
  }

  @Test
  public void testWitnessMessageSignaturesDropdown() {
    witnessingViewModel = new ViewModelProvider(getLaoActivity()).get(WitnessingViewModel.class);
    witnessingViewModel.setWitnessMessages(witnessMessages);

    // Check that the signatures title is displayed
    onData(anything())
        .inAdapterView(witnessMessageListMatcher())
        .atPosition(0)
        .check(matches(hasDescendant(withText(SIGNATURES_TITLE))));

    // Check that the signatures are not displayed by default
    witnessSignaturesText().check(matches(withEffectiveVisibility(Visibility.GONE)));

    // Click on the arrow to expand the text
    onData(anything())
        .inAdapterView(witnessMessageListMatcher())
        .atPosition(0)
        .onChildView(messageSignaturesArrowMatcher())
        .perform(click());

    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    // Check that correct signatures are displayed
    witnessSignaturesText().check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    onData(anything())
        .inAdapterView(witnessMessageListMatcher())
        .atPosition(0)
        .check(matches(hasDescendant(withText(SIGNATURES_TEXT))));
  }

  private LaoActivity getLaoActivity() {
    AtomicReference<LaoActivity> ref = new AtomicReference<>();
    activityScenarioRule.getScenario().onActivity(ref::set);

    return ref.get();
  }
}
