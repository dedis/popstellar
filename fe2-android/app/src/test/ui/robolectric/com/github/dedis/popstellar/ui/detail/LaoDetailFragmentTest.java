package com.github.dedis.popstellar.ui.detail;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.testutils.*;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.*;
import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailFragmentPageObject.*;
import static com.github.dedis.popstellar.testutils.pages.detail.event.election.ElectionSetupPageObject.*;
import static com.github.dedis.popstellar.testutils.pages.detail.event.rollcall.RollCallCreatePageObject.*;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LaoDetailFragmentTest {

  private static final String LAO_NAME = "LAO";
  private static final KeyPair KEY_PAIR = Base64DataUtils.generateKeyPair();
  private static final PoPToken POP_TOKEN = Base64DataUtils.generatePoPToken();
  private static final PublicKey PK = KEY_PAIR.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, PK, 10223421);
  private static final String LAO_ID = LAO.getId();
  private static final String RC_NAME = "Roll-Call Title";
  private static final String ELECTION_NAME = "an election name";
  private static final String QUESTION = "question";
  private static final String BALLOT_1 = "ballot 1";
  private static final String BALLOT_2 = "ballot 2";
  private static final BehaviorSubject<LaoView> laoViewSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));

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
        protected void before() throws KeyException, UnknownLaoException {
          hiltRule.inject();
          when(repository.getLaoObservable(anyString())).thenReturn(laoViewSubject);
          when(repository.getLaoView(anyString())).thenAnswer(invocation -> new LaoView(LAO));
          when(keyManager.getMainPublicKey()).thenReturn(PK);
          when(keyManager.getPoPToken(any(), any())).thenReturn(POP_TOKEN);
          when(networkManager.getMessageSender()).thenReturn(messageSender);

          when(messageSender.publish(any(), any(), any()))
              .then(
                  args -> {
                    Object obj = args.getArgument(2);
                    if (obj instanceof CreateRollCall) {
                      CreateRollCall createRollCall = (CreateRollCall) obj;
                      LAO.updateRollCall(createRollCall.getId(), buildRcFromCreate(createRollCall));
                    }
                    return Completable.complete();
                  });
        }
      };

  @Rule(order = 3)
  public ActivityScenarioRule<LaoDetailActivity> activityScenarioRule =
      new ActivityScenarioRule<>(
          IntentUtils.createIntent(
              LaoDetailActivity.class,
              new BundleBuilder()
                  .putString(laoIdExtra(), LAO_ID)
                  .putString(fragmentToOpenExtra(), laoDetailValue())
                  .build()));

  @Test
  public void titleIsDisplayedAndMatches() {
    titleTextView().check(matches(isDisplayed()));
    titleTextView().check(matches(withText(LAO_NAME)));
  }

  @Test
  public void showPropertyButtonShowsConnectQRCode() {
    qrCodeIcon().perform(click());

    qrCodeLayout().check(matches(isDisplayed()));

    String expectedQRCode = gson.toJson(new ConnectToLao(networkManager.getCurrentUrl(), LAO_ID));
    connectQrCode().check(matches(withQrCode(expectedQRCode)));
  }

  @Test
  public void addEventButtonIsDisplayedForOrganizer() {
    when(keyManager.getMainPublicKey()).thenReturn(PK);
    addEventButton().check(matches(isDisplayed()));
  }

  @Test
  public void addRCAndElectionButtonsAndTextsAreNotDisplayed() {
    addElectionButton().check(matches(not(isDisplayed())));
    addRollCallButton().check(matches(not(isDisplayed())));
    addElectionText().check(matches(not(isDisplayed())));
    addRollCallText().check(matches(not(isDisplayed())));
  }

  @Test
  public void addRCAndElectionButtonsAndTextsAreDisplayedWhenAddIsClicked() {
    addEventButton().perform(click());
    addElectionButton().check(matches(isDisplayed()));
    addRollCallButton().check(matches(isDisplayed()));
    addElectionText().check(matches(isDisplayed()));
    addRollCallText().check(matches(isDisplayed()));
  }

  public void EventListIsDisplayed() {
    eventList().check(matches(isDisplayed()));
  }

  @Test
  public void confirmingRollCallOpensEventListScreen() {
    goToRollCallCreationAndEnterTitle();
    rollCallCreateConfirmButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(laoDetailFragmentId()))));
  }

  @Test
  public void openRollCallOpensPermission() {
    setupViewModel();
    goToRollCallCreationAndEnterTitle();
    rollCreateOpenButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(qrCodeFragmentId()))));
  }

  @Test
  public void submitElectionOpensEventList() {
    createElection();
    fragmentContainer().check(matches(withChild(withId(laoDetailFragmentId()))));
  }

  private void goToRollCallCreationAndEnterTitle() {
    addEventButton().perform(click());
    addRollCallButton().perform(click());
    rollCallCreateTitle().perform(typeText(RC_NAME));
  }

  private void createElection() {
    addEventButton().perform(click());
    addElectionButton().perform(click());
    electionName().perform(typeText(ELECTION_NAME));
    questionText().perform(typeText(QUESTION));
    ballotOptionAtPosition(0).perform(typeText(BALLOT_1));
    ballotOptionAtPosition(1).perform(typeText(BALLOT_2));
    submit().perform(click());
  }

  // Matches an ImageView containing a QRCode with expected content
  private Matcher<? super View> withQrCode(String expectedContent) {
    return new BoundedMatcher<View, ImageView>(ImageView.class) {
      @Override
      protected boolean matchesSafely(ImageView item) {
        String actualContent = extractContent(item);
        return expectedContent.equals(actualContent);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("QRCode('" + expectedContent + "')");
      }

      @Override
      public void describeMismatch(Object item, Description description) {
        if (super.matches(item)) {
          // The type is a match, so the mismatch came from the QRCode content
          String content = extractContent((ImageView) item);
          description.appendText("QRCode('" + content + "')");
        } else {
          // The mismatch is on the type, let BoundedMatcher handle it
          super.describeMismatch(item, description);
        }
      }

      private String extractContent(ImageView item) {
        Drawable drawable = item.getDrawable();
        if (!(drawable instanceof BitmapDrawable))
          throw new IllegalArgumentException("The provided ImageView does not contain a bitmap");

        BinaryBitmap binary = convertToBinary(((BitmapDrawable) drawable).getBitmap());

        try {
          // Parse the bitmap and check it against expected value
          return new QRCodeReader().decode(binary).getText();
        } catch (NotFoundException | ChecksumException | FormatException e) {
          throw new IllegalArgumentException("The provided image is not a valid QRCode", e);
        }
      }

      private BinaryBitmap convertToBinary(Bitmap qrcode) {
        // Convert the QRCode to something zxing understands
        int[] buffer = new int[qrcode.getWidth() * qrcode.getHeight()];
        qrcode.getPixels(buffer, 0, qrcode.getWidth(), 0, 0, qrcode.getWidth(), qrcode.getHeight());
        LuminanceSource source =
            new RGBLuminanceSource(qrcode.getWidth(), qrcode.getHeight(), buffer);
        return new BinaryBitmap(new HybridBinarizer(source));
      }
    };
  }

  private void setupViewModel() {
    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
              laoDetailViewModel.setCurrentLao(new LaoView(LAO));
            });
    // Recreate the fragment because the viewModel needed to be modified before start
    activityScenarioRule.getScenario().recreate();
  }

  private RollCall buildRcFromCreate(CreateRollCall createRollCall) {
    RollCall rollCall = new RollCall(createRollCall.getId());
    rollCall.setCreation(createRollCall.getCreation());
    rollCall.setState(EventState.CREATED);
    rollCall.setStart(createRollCall.getProposedStart());
    rollCall.setEnd(createRollCall.getProposedEnd());
    rollCall.setName(createRollCall.getName());
    rollCall.setLocation(createRollCall.getLocation());
    rollCall.setLocation(createRollCall.getLocation());
    rollCall.setDescription(createRollCall.getDescription().orElse(""));
    return rollCall;
  }
}
