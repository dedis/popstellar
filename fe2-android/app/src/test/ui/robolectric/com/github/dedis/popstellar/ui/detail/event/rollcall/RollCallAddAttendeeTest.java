package com.github.dedis.popstellar.ui.detail.event.rollcall;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.UITestUtils.forceTypeText;
import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.*;
import static com.github.dedis.popstellar.testutils.pages.scanning.QrScanningPageObject.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class RollCallAddAttendeeTest {

  private static final String LAO_NAME = "lao";
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, SENDER, 10223421);
  private static final String LAO_ID = LAO.getId();
  private static final String POP_TOKEN =
      Base64DataUtils.generatePoPToken().getPublicKey().getEncoded();
  private static final String VALID_RC_MANUAL_INPUT = "{\"pop_token\": \"" + POP_TOKEN + "\"}";
  public static final String JSON_INVALID_INPUT = "{pop_token:" + POP_TOKEN;
  public static final String VALID_WITNESS_MANUAL_INPUT =
      "{\"main_public_key\": \"" + POP_TOKEN + "\"}";
  public static final String INVALID_KEY_FORMAT_INPUT = "{\"pop_token\": \"invalid_key\"}";

  private static final BehaviorSubject<LaoView> laoSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));

  @BindValue @Mock LAORepository repository;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() throws UnknownLaoException {
          hiltRule.inject();
          when(repository.getLaoObservable(anyString())).thenReturn(laoSubject);
          when(repository.getLaoView(any())).thenReturn(new LaoView(LAO));
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoDetailActivity, QRCodeScanningFragment>
      activityScenarioRule =
          ActivityFragmentScenarioRule.launchIn(
              LaoDetailActivity.class,
              new BundleBuilder()
                  .putString(laoIdExtra(), LAO_ID)
                  .putString(fragmentToOpenExtra(), laoDetailValue())
                  .build(),
              containerId(),
              QRCodeScanningFragment.class,
              QRCodeScanningFragment::new);

  @Test
  public void addButtonIsDisplayed() {
    manualAddConfirm().check(matches(isDisplayed()));
  }

  @Test
  public void addingAttendeeManuallyUpdatesCount() {
    setupViewModel();
    manualAddEditText().perform(forceTypeText(VALID_RC_MANUAL_INPUT));
    manualAddConfirm().perform(click());

    // Since we haven't mocked for the viewModel to fetch the organizer token, adding an attendee
    // should result in a total of one attendee
    attendeeCount().check(matches(withText("1")));
  }

  @Test
  public void addingInvalidJsonFormatDoesNotAddAttendees() {
    setupViewModel();
    manualAddEditText().perform(forceTypeText(JSON_INVALID_INPUT));
    manualAddConfirm().perform(click());

    // Since we opened the scanner directly (without going through the openRollCall of the
    // viewModel), the count was never updated to 0. Here we checked that after submission it is not
    // updated
    attendeeCount().check(matches(withText("")));
  }

  @Test
  public void addingValidNonRcFormatDoesNotAddAttendees() {
    setupViewModel();
    manualAddEditText().perform(forceTypeText(VALID_WITNESS_MANUAL_INPUT));
    manualAddConfirm().perform(click());

    // Since we opened the scanner directly (without going through the openRollCall of the
    // viewModel), the count was never updated to 0. Here we checked that after submission it is not
    // updated
    attendeeCount().check(matches(withText("")));
  }

  @Test
  public void addingKeyFormatDoesNotAddAttendees() {
    setupViewModel();
    manualAddEditText().perform(forceTypeText(INVALID_KEY_FORMAT_INPUT));
    manualAddConfirm().perform(click());

    // Since we opened the scanner directly (without going through the openRollCall of the
    // viewModel), the count was never updated to 0. Here we checked that after submission it is not
    // updated
    attendeeCount().check(matches(withText("")));
  }

  private void setupViewModel() {
    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
              laoDetailViewModel.setScanningAction(ScanningAction.ADD_ROLL_CALL_ATTENDEE);
            });
    activityScenarioRule.getScenario().recreate();
  }
}
