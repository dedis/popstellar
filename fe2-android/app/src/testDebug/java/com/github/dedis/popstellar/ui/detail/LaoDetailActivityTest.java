package com.github.dedis.popstellar.ui.detail;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.connectQrCode;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.fragmentContainer;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.fragmentToOpenExtra;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.homeButton;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.identityButton;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.identityFragmentId;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.laoDetailValue;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.laoIdExtra;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.propertiesLayout;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.showPropertiesButton;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.witnessButton;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailPageObject.witnessFragmentId;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.LAORequestFactory;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.IntentUtils;
import com.github.dedis.popstellar.ui.home.HomeActivity;
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
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import javax.inject.Inject;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.subjects.BehaviorSubject;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LaoDetailActivityTest {

  private static final Lao LAO = new Lao("LAO", Base64DataUtils.generatePublicKey(), 10223421);
  private static final String LAO_ID = LAO.getId();

  @Inject LAORequestFactory requestFactory;
  @Inject Gson gson;

  @BindValue @Mock LAORepository laoRepository;

  // Hilt rule
  private final HiltAndroidRule hiltAndroidRule = new HiltAndroidRule(this);
  // Setup rule, used to setup things before the activity is started
  private final TestRule setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          hiltAndroidRule.inject();

          when(laoRepository.getLaoObservable(anyString()))
              .thenReturn(BehaviorSubject.createDefault(LAO));
        }
      };
  // Activity scenario rule that starts the activity.
  // It creates a LaoDetailActivity with set extras such that the LAO is used
  public ActivityScenarioRule<LaoDetailActivity> activityScenarioRule =
      new ActivityScenarioRule<>(
          IntentUtils.createIntent(
              LaoDetailActivity.class,
              new BundleBuilder()
                  .putString(laoIdExtra(), LAO_ID)
                  .putString(fragmentToOpenExtra(), laoDetailValue())
                  .build()));

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(MockitoJUnit.testRule(this))
          .around(hiltAndroidRule)
          .around(setupRule)
          .around(activityScenarioRule);

  @Test
  public void homeButtonOpensHome() {
    homeButton().perform(click());

    assertEquals(
        HomeActivity.class.getName(),
        activityScenarioRule
            .getScenario()
            .getResult()
            .getResultData()
            .getComponent()
            .getClassName());
  }

  @Test
  public void identityButtonOpensIdentityTab() {
    identityButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(identityFragmentId()))));
  }

  @Test
  public void showPropertyButtonShowsConnectQRCode() {
    showPropertiesButton().perform(click());

    propertiesLayout().check(matches(isDisplayed()));

    String expectedQRCode = gson.toJson(new ConnectToLao(requestFactory.getUrl(), LAO_ID));
    connectQrCode().check(matches(withQrCode(expectedQRCode)));
  }

  @Test
  public void witnessButtonShowsWitnessTab() {
    witnessButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(witnessFragmentId()))));
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
