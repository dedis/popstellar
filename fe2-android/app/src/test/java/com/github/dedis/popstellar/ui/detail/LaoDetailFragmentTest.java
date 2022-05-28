package com.github.dedis.popstellar.ui.detail;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.fragmentContainer;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.fragmentToOpenExtra;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.laoDetailFragmentId;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.laoDetailValue;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.laoIdExtra;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailFragmentPageObject.addElectionButton;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailFragmentPageObject.addElectionText;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailFragmentPageObject.addEventButton;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailFragmentPageObject.addRollCallButton;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailFragmentPageObject.addRollCallText;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailFragmentPageObject.connectQrCode;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailFragmentPageObject.eventList;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailFragmentPageObject.qrCodeIcon;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailFragmentPageObject.qrCodeLayout;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailFragmentPageObject.titleTextView;
import static com.github.dedis.popstellar.ui.pages.detail.event.rollcall.RollCallCreatePageObject.rollCallCreateConfirmButton;
import static com.github.dedis.popstellar.ui.pages.detail.event.rollcall.RollCallCreatePageObject.rollCallCreateTitle;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.IntentUtils;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
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

import java.util.Collections;

import javax.inject.Inject;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LaoDetailFragmentTest {

  private static final String LAO_NAME = "LAO";
  private static final KeyPair KEY_PAIR = Base64DataUtils.generateKeyPair();
  private static final PublicKey PK = KEY_PAIR.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, PK, 10223421);
  private static final String LAO_ID = LAO.getId();
  private static final String RC_TITLE = "Roll-Call Title";

  // @Inject GlobalNetworkManager networkManager;
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
        protected void before() {
          hiltRule.inject();
          when(repository.getLaoObservable(anyString()))
              .thenReturn(BehaviorSubject.createDefault(LAO));
          when(repository.getAllLaos())
              .thenReturn(BehaviorSubject.createDefault(Collections.singletonList(LAO)));

          when(keyManager.getMainPublicKey()).thenReturn(PK);
          when(keyManager.getMainKeyPair()).thenReturn(KEY_PAIR);
          when(networkManager.getMessageSender()).thenReturn(messageSender);
          when(messageSender.subscribe(any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any(), any())).then(args -> Completable.complete());
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

  private void goToRollCallCreationAndEnterTitle() {
    addEventButton().perform(click());
    addRollCallButton().perform(click());
    rollCallCreateTitle().perform(typeText(RC_TITLE));
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
}
