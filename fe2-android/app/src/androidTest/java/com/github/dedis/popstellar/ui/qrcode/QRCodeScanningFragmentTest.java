package com.github.dedis.popstellar.ui.qrcode;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.github.dedis.popstellar.pages.qrcode.QRCodeScanningPageObject.addAttendeeButton;
import static com.github.dedis.popstellar.pages.qrcode.QRCodeScanningPageObject.addAttendeeTokenTextInput;
import static com.github.dedis.popstellar.pages.qrcode.QRCodeScanningPageObject.cancelButton;
import static com.github.dedis.popstellar.pages.qrcode.QRCodeScanningPageObject.closeRollCallButton;
import static com.github.dedis.popstellar.pages.qrcode.QRCodeScanningPageObject.confirmButton;
import static com.github.dedis.popstellar.pages.qrcode.QRCodeScanningPageObject.invalidTokenPopup;
import static com.github.dedis.popstellar.pages.qrcode.QRCodeScanningPageObject.okButton;
import static com.github.dedis.popstellar.pages.qrcode.QRCodeScanningPageObject.successPopup;
import static com.github.dedis.popstellar.pages.qrcode.QRCodeScanningPageObject.tokenAlreadyAddedPopup;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;

import android.Manifest.permission;
import android.os.Bundle;

import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.Publish;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAODataSource;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailFragment;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.ProdSchedulerProvider;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Observable;

/** Class handling connect fragment tests */
@LargeTest
@HiltAndroidTest
public class QRCodeScanningFragmentTest {

  @Inject KeyManager keyManager;
  @Inject MessageHandler messageHandler;
  @Inject Gson gson;

  @BindValue LAORepository laoRepository;

  @Mock LAODataSource.Remote remoteDataSource;
  @Mock LAODataSource.Local localDataSource;

  private static final ArgumentCaptor<Message> CAPTOR = ArgumentCaptor.forClass(Message.class);

  private static final String LAO_NAME = "aaaa";
  private static final String ROLL_CALL_NAME = "bbbb";
  private static final long creation = 946684800;
  private static final String ATTENDEE_VALID_1 = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=";
  private static final String ATTENDEE_VALID_2 = "ywWU4tMX8CR7CtXuutCZNyWBYRbtyk9NJqopvqSFgfI=";
  private static final String ATTENDEE_INVALID_1 = "M5ZychEi5rwm22FjwjNuljL1qMJ";
  private static final String ATTENDEE_INVALID_2 = "%&/()=?`";
  private static final String ATTENDEE_INVALID_3 = "";

  private final Bundle bundle = new Bundle();
  private String rollCallId;

  private final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  private final ActivityFragmentScenarioRule<LaoDetailActivity, LaoDetailFragment>
      activityFragmentScenarioRule =
          ActivityFragmentScenarioRule.launchIn(
              LaoDetailActivity.class,
              bundle,
              R.id.fragment_container_lao_detail,
              LaoDetailFragment.class,
              LaoDetailFragment::new);

  private final TestRule setupRule =
      new ExternalResource() {
        @Override
        protected void before()
            throws IOException, GeneralSecurityException, DataHandlingException {
          // Injection with hilt
          hiltRule.inject();

          when(remoteDataSource.incrementAndGetRequestId()).thenReturn(42);
          when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());
          Observable<GenericMessage> upstream = Observable.fromArray(new Result(42));

          // Mock the remote data source to receive a response
          when(remoteDataSource.observeMessage()).thenReturn(upstream);

          laoRepository =
              new LAORepository(
                  remoteDataSource,
                  localDataSource,
                  keyManager,
                  messageHandler,
                  gson,
                  new ProdSchedulerProvider());

          Lao lao = new Lao(LAO_NAME, keyManager.getMainPublicKey(), creation);
          lao.setChannel("/root/" + lao.getId());
          RollCall rollCall = new RollCall(lao.getId(), creation, ROLL_CALL_NAME);
          rollCallId = rollCall.getId();
          lao.updateRollCall(rollCallId, rollCall);

          CreateLao createLao =
              new CreateLao(
                  lao.getId(),
                  LAO_NAME,
                  creation,
                  keyManager.getMainPublicKey(),
                  Collections.emptyList());

          CreateRollCall createRollCall =
              new CreateRollCall(
                  ROLL_CALL_NAME,
                  creation,
                  creation,
                  creation + 1846684800,
                  "Lausanne",
                  "desc",
                  lao.getId());

          OpenRollCall openRollCall =
              new OpenRollCall(lao.getId(), createRollCall.getId(), creation, EventState.OPENED);

          MessageGeneral createLaoMsg =
              new MessageGeneral(keyManager.getMainKeyPair(), createLao, gson);

          MessageGeneral createRollCallMsg =
              new MessageGeneral(keyManager.getMainKeyPair(), createRollCall, gson);

          MessageGeneral openRollCallMsg =
              new MessageGeneral(keyManager.getMainKeyPair(), openRollCall, gson);

          Map<Integer, String> createLaoRequests = new HashMap<>();
          createLaoRequests.put(1, lao.getChannel());
          messageHandler.handleCreateLao(laoRepository, 1, createLaoRequests);
          laoRepository.setAllLaoSubject();
          laoRepository.getLaoById().put(lao.getId(), new LAOState(lao));

          messageHandler.handleMessage(laoRepository, lao.getChannel(), createLaoMsg);
          messageHandler.handleMessage(laoRepository, lao.getChannel(), createRollCallMsg);
          messageHandler.handleMessage(laoRepository, lao.getChannel(), openRollCallMsg);

          bundle.putString("LAO_ID", lao.getId());
          bundle.putString("FRAGMENT_TO_OPEN", "LaoDetail");
        }
      };

  @Rule public GrantPermissionRule permissionRule = GrantPermissionRule.grant(permission.CAMERA);

  @Rule
  public final RuleChain chain =
      RuleChain.outerRule(MockitoJUnit.testRule(this))
          .around(hiltRule)
          .around(setupRule)
          .around(permissionRule)
          .around(activityFragmentScenarioRule);

  private void setupScanningFragment() {
    activityFragmentScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
              laoDetailViewModel.openRollCall(rollCallId);
              laoDetailViewModel.openQrCodeScanningRollCall();
            });
  }

  private List<PublicKey> getAttendeesFromInterceptedCloseRollCallMsg() {
    Mockito.verify(remoteDataSource, atLeast(1)).sendMessage(CAPTOR.capture());
    Publish publish = (Publish) CAPTOR.getValue();
    MessageGeneral closeRollCallMsg = publish.getMessage();
    CloseRollCall closeRollCall = (CloseRollCall) closeRollCallMsg.getData();
    return closeRollCall.getAttendees();
  }

  @Test
  public void addingAttendeesManuallyTest() throws InterruptedException {
    setupScanningFragment();

    // Add ATTENDEE_VALID_1
    addAttendeeButton().perform(click());
    addAttendeeTokenTextInput().perform(click(), typeText(ATTENDEE_VALID_1));
    confirmButton().perform(click());
    successPopup().check(matches(isDisplayed()));
    Thread.sleep(2200); // wait for the success popup to disappear

    // Add ATTENDEE_VALID_2
    addAttendeeButton().perform(click());
    addAttendeeTokenTextInput().perform(click(), typeText(ATTENDEE_VALID_2));
    confirmButton().perform(click());
    successPopup().check(matches(isDisplayed()));
    Thread.sleep(2200); // wait for the success popup to disappear

    // Try to add ATTENDEE_VALID_1 again, should show a warning popup
    addAttendeeButton().perform(click());
    addAttendeeTokenTextInput().perform(click(), typeText(ATTENDEE_VALID_1));
    confirmButton().perform(click());
    tokenAlreadyAddedPopup().check(matches(isDisplayed()));
    okButton().perform(click()); // close the warning popup

    // Close the roll call and get the attendees list
    closeRollCallButton().perform(click());
    confirmButton().perform(click());
    List<PublicKey> attendees = getAttendeesFromInterceptedCloseRollCallMsg();

    assertEquals(2, attendees.size());
    assertTrue(attendees.contains(new PublicKey(ATTENDEE_VALID_1)));
    assertTrue(attendees.contains(new PublicKey(ATTENDEE_VALID_2)));
  }

  @Test
  public void invalidTokenTest() {
    setupScanningFragment();

    // Try to add ATTENDEE_INVALID_1, should show an invalid token popup
    addAttendeeButton().perform(click());
    addAttendeeTokenTextInput().perform(click(), typeText(ATTENDEE_INVALID_1)); // Base64 wrong size
    confirmButton().perform(click());
    invalidTokenPopup().check(matches(isDisplayed()));
    okButton().perform(click()); // close the warning popup

    // Try to add ATTENDEE_INVALID_2, should show an invalid token popup
    addAttendeeButton().perform(click());
    addAttendeeTokenTextInput().perform(click(), typeText(ATTENDEE_INVALID_2)); // Not Base64
    confirmButton().perform(click());
    invalidTokenPopup().check(matches(isDisplayed()));
    okButton().perform(click()); // close the warning popup

    // Try to add ATTENDEE_INVALID_3, should show an invalid token popup
    addAttendeeButton().perform(click());
    addAttendeeTokenTextInput().perform(click(), typeText(ATTENDEE_INVALID_3)); // Empty token
    confirmButton().perform(click());
    invalidTokenPopup().check(matches(isDisplayed()));
    okButton().perform(click()); // close the warning popup

    // Close the roll call and get the attendees list
    closeRollCallButton().perform(click());
    confirmButton().perform(click());
    List<PublicKey> attendees = getAttendeesFromInterceptedCloseRollCallMsg();

    assertTrue(attendees.isEmpty());
  }

  @Test
  public void cancelShouldNotAddAttendeeTest() {
    setupScanningFragment();

    // Cancel with ATTENDEE_INVALID_1 in the text input, should not add it
    addAttendeeButton().perform(click());
    addAttendeeTokenTextInput().perform(click(), typeText(ATTENDEE_INVALID_1)); // Base64 wrong size
    cancelButton().perform(click());

    // Close the roll call and get the attendees list
    closeRollCallButton().perform(click());
    confirmButton().perform(click());
    List<PublicKey> attendees = getAttendeesFromInterceptedCloseRollCallMsg();

    assertTrue(attendees.isEmpty());
  }
}
