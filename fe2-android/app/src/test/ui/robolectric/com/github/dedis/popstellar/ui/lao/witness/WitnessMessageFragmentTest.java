package com.github.dedis.popstellar.ui.lao.witness;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.containerId;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessMessageFragmentPageObject.witnessMessageFragment;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessMessageFragmentPageObject.witnessMessageList;
import static org.hamcrest.CoreMatchers.anything;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.app.Activity;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.RollCallBuilder;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.MessageSenderHelper;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallFragment;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.subjects.BehaviorSubject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
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
public class WitnessMessageFragmentTest {

  private static final String LAO_NAME = "lao";
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, SENDER, Instant.now().getEpochSecond());
  private static final String LAO_ID = LAO.getId();
  private static final BehaviorSubject<LaoView> laoSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));

  private static final PoPToken POP_TOKEN = Base64DataUtils.generatePoPToken();
  private static WitnessingViewModel witnessingViewModel;

  @Inject RollCallRepository rollCallRepo;

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
  public void testWitnessMessageListIsDisplayed() {
    witnessMessageFragment().check(matches(isDisplayed()));
    witnessMessageList().check(matches(isDisplayed()));
  }

  @Test
  public void testWitnessMd() {
    witnessingViewModel = new ViewModelProvider(getLaoActivity()).get(WitnessingViewModel.class);

    WitnessMessage witnessMessage = new WitnessMessage(Base64DataUtils.generateMessageID());
    List<WitnessMessage> witnessMessages = Collections.singletonList(witnessMessage);

    witnessingViewModel.setWitnessMessages(witnessMessages);

    witnessMessageList().check(matches(isDisplayed()));
    onData(anything())
        .inAdapterView(withId(R.id.witness_message_list))
        .atPosition(0)
        .perform(click());
  }

  private LaoActivity getLaoActivity() {
    AtomicReference<LaoActivity> ref = new AtomicReference<>();
    activityScenarioRule.getScenario().onActivity(activity -> ref.set(activity));

    return ref.get();
  }
}
