package com.github.dedis.popstellar.ui.digitalcash;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.digitalcash.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.testutils.*;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.*;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.fragmentContainer;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.*;
import static com.github.dedis.popstellar.ui.pages.digitalcash.HistoryPageObject.fragmentDigitalCashHistoryId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.IssuePageObject.fragmentDigitalCashIssueId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.ReceiptPageObject.fragmentDigitalCashReceiptId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.ReceivePageObject.fragmentDigitalCashReceiveId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.SendPageObject.fragmentDigitalCashSendId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.SendPageObject.sendButtonToReceipt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class DigitalCashActivityTest {

  private static final PoPToken POP_TOKEN = Base64DataUtils.generatePoPToken();
  private static final String LAO_NAME = "LAO";
  private static final Lao LAO = new Lao(LAO_NAME, POP_TOKEN.getPublicKey(), 10223421);
  private static final String LAO_ID = LAO.getId();
  private static final String RC_TITLE = "Roll-Call Title";

  @Inject Gson gson;

  @BindValue @Mock GlobalNetworkManager networkManager;
  @BindValue @Mock LAORepository repository;
  @BindValue @Mock MessageSender messageSender;
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
        protected void before() throws KeyException, GeneralSecurityException {

          RollCall rc = new RollCall(RC_TITLE);
          rc.setAttendees(Collections.singleton(POP_TOKEN.getPublicKey()));
          rc.setState(EventState.CLOSED);
          LAO.setRollCalls(Collections.singletonMap(RC_TITLE, rc));

          TransactionObjectBuilder builder = new TransactionObjectBuilder();
          builder.setVersion(1);
          builder.setLockTime(0);
          builder.setChannel(Channel.fromString("/root/laoId/coin/myChannel"));

          ScriptOutput so = new ScriptOutput("P2PKH", POP_TOKEN.getPublicKey().computeHash());
          ScriptOutputObject soo =
              new ScriptOutputObject("P2PKH", POP_TOKEN.getPublicKey().computeHash());

          OutputObject oo = new OutputObject(10, soo);
          Output out = new Output(10, so);

          Signature sig =
              POP_TOKEN
                  .getPrivateKey()
                  .sign(
                      new Base64URLData(
                          Transaction.computeSigOutputsPairTxOutHashAndIndex(
                                  Collections.singletonList(out),
                                  Collections.singletonMap(
                                      "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", 0))
                              .getBytes(StandardCharsets.UTF_8)));

          ScriptInputObject sio = new ScriptInputObject("P2PKH", POP_TOKEN.getPublicKey(), sig);

          InputObject io = new InputObject("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", 0, sio);

          builder.setOutputs(Collections.singletonList(oo));
          builder.setInputs(Collections.singletonList(io));
          builder.setTransactionId("some id");

          LAO.updateTransactionMaps(builder.build());

          hiltRule.inject();
          when(repository.getLaoObservable(anyString()))
              .thenReturn(BehaviorSubject.createDefault(new LaoView(LAO)));
          when(keyManager.getValidPoPToken(any())).thenReturn(POP_TOKEN);
          when(keyManager.getMainPublicKey()).thenReturn(POP_TOKEN.getPublicKey());
          when(networkManager.getMessageSender()).thenReturn(messageSender);
          when(messageSender.subscribe(any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any(), any())).then(args -> Completable.complete());
        }
      };

  @Rule(order = 3)
  public ActivityScenarioRule<DigitalCashActivity> activityScenarioRule =
      new ActivityScenarioRule<>(
          IntentUtils.createIntent(
              DigitalCashActivity.class,
              new BundleBuilder()
                  .putString(laoIdExtra(), LAO_ID)
                  .putString(fragmentToOpenExtra(), laoDetailValue())
                  .build()));

  @Test
  public void homeButtonStaysHome() {
    homeButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(digitalCashFragmentId()))));
  }

  @Test
  public void sendButtonGoesToSendThenToReceipt() {
    sendButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashSendId()))));
    sendButtonToReceipt().perform(click());
    fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashReceiptId()))));
  }

  @Test
  public void historyButtonGoesToHistory() {
    historyButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashHistoryId()))));
  }

  @Test
  public void issueButtonGoesToIssue() {
    issueButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashIssueId()))));
  }

  @Test
  public void receiveButtonGoesToReceive() {
    receiveButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashReceiveId()))));
  }
}
