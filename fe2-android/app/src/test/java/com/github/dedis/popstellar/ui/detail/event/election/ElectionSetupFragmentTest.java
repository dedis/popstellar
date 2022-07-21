package com.github.dedis.popstellar.ui.detail.event.election;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionSetupFragment;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Completable;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton;
import static com.github.dedis.popstellar.testutils.UITestUtils.getLastDialog;
import static com.github.dedis.popstellar.ui.pages.detail.event.EventCreationPageObject.endDateView;
import static com.github.dedis.popstellar.ui.pages.detail.event.EventCreationPageObject.endTimeView;
import static com.github.dedis.popstellar.ui.pages.detail.event.EventCreationPageObject.startDateView;
import static com.github.dedis.popstellar.ui.pages.detail.event.EventCreationPageObject.startTimeView;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.ElectionSetupPageObject.addBallot;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.ElectionSetupPageObject.addQuestion;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.ElectionSetupPageObject.ballotOptionAtPosition;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.ElectionSetupPageObject.electionName;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.ElectionSetupPageObject.questionText;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.ElectionSetupPageObject.submit;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.ElectionSetupPageObject.versionChoice;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.ElectionSetupPageObject.writeIn;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class ElectionSetupFragmentTest {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
  private static final String TIME_FORMAT = "%02d:%02d";

  private static final int YEAR;
  private static final int MONTH_OF_YEAR;
  private static final int DAY_OF_MONTH;
  private static final String DATE;

  private static final int HOURS = 12;
  private static final int MINUTES = 15;
  private static final String TIME = String.format(TIME_FORMAT, HOURS, MINUTES);

  private static final String ELECTION_NAME = "aaaaaaaaaaa";
  private static final String LAO_NAME = "bbbbbbbbbbbb";
  private static final PublicKey ORGANIZER =
      new PublicKey("5c2zk_5uCrrNmdUhQAloCDqYJAC2rD4KHo9gGNFVS9c");
  private static final Lao LAO = new Lao(LAO_NAME, ORGANIZER, 0);

  @Inject KeyManager keyManager;
  @Inject MessageHandler messageHandler;
  @Inject Gson gson;

  @BindValue @Mock GlobalNetworkManager globalNetworkManager;
  @Mock MessageSender messageSender;

  static {
    // Make sure the date is always in the future
    Calendar today = Calendar.getInstance();
    today.add(Calendar.MONTH, 13);

    YEAR = today.get(Calendar.YEAR);
    MONTH_OF_YEAR = today.get(Calendar.MONTH);
    DAY_OF_MONTH = today.get(Calendar.DAY_OF_MONTH);
    DATE = DATE_FORMAT.format(today.getTime());
  }

  private final FragmentScenarioRule<ElectionSetupFragment> fragmentRule =
      FragmentScenarioRule.launch(ElectionSetupFragment.class);

  private final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  private final TestRule setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          // Injection with hilt
          hiltRule.inject();

          when(globalNetworkManager.getMessageSender()).thenReturn(messageSender);

          when(messageSender.publish(any(), any(), any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any())).then(args -> Completable.complete());
          when(messageSender.subscribe(any())).then(args -> Completable.complete());
        }
      };

  @Rule
  public final RuleChain chain =
      RuleChain.outerRule(MockitoJUnit.testRule(this))
          .around(hiltRule)
          .around(setupRule)
          .around(fragmentRule);

  private ElectionSetup getInterceptedElectionSetupMsg() {
    ArgumentCaptor<ElectionSetup> captor = ArgumentCaptor.forClass(ElectionSetup.class);
    Mockito.verify(messageSender, atLeast(1)).publish(any(), any(), captor.capture());
    return captor.getValue();
  }

  private void setupViewModel() {
    fragmentRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
              laoDetailViewModel.setCurrentLao(LAO);
            });
  }

  @Test
  public void canLaunchDatePickerFragmentFromStartDateButton() {
    startDateView().perform(click());
    assertThat(getLastDialog(DatePickerDialog.class).getDatePicker(), isDisplayed());
  }

  @Test
  public void canLaunchDatePickerFragmentFromEndDateButton() {
    endDateView().perform(click());
    assertThat(getLastDialog(DatePickerDialog.class).getDatePicker(), isDisplayed());
  }

  @Test
  public void canChooseRandomDate() {
    startDateView().perform(click());

    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();

    startDateView().check(matches(withText(DATE)));
  }

  @Test
  public void datePickerChoosesTodayByDefault() {
    final Calendar currentCalendar = Calendar.getInstance();
    final String date = DATE_FORMAT.format(currentCalendar.getTime());

    startDateView().perform(click());
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(date)));
  }

  @Test
  public void startDateAndEndDateCanBothBeSameDay() {
    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(DATE)));

    endDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    endDateView().check(matches(withText(DATE)));
  }

  @Test
  public void canChooseRandomStartTimeWhenNoDate() {
    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomEndTimeWhenNoDate() {
    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    endTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomStartTimeWhenStartDateFilled() {
    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class)
        .getDatePicker()
        .updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(DATE)));

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomStartTimeWhenEndDateFilled() {
    endDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    endDateView().check(matches(withText(DATE)));

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomEndTimeWhenStartDateFilled() {
    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(DATE)));

    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    endTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomEndTimeWhenEndDateFilled() {
    endDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    endDateView().check(matches(withText(DATE)));

    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    endTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseStartTimeBeforeEndTimeWhenSameDayEvent() {
    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(DATE)));

    endDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    endDateView().check(matches(withText(DATE)));

    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    endTimeView().check(matches(withText(TIME)));

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS - 1, MINUTES);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText("" + (HOURS - 1) + ":" + MINUTES)));
  }

  @Test
  public void canChooseEndTimeBeforeStartTimeWhenSameDayEvent() {
    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(DATE)));

    endDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    endDateView().check(matches(withText(DATE)));

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText(TIME)));

    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS + 1, MINUTES);
    dialogPositiveButton().performClick();
    endTimeView().check(matches(withText(String.format(TIME_FORMAT, HOURS + 1, MINUTES))));
  }

  @Test
  public void cannotChooseStartTimeTooFarInPast() {
    Calendar today = Calendar.getInstance();
    today.add(Calendar.MINUTE, -10);
    int year = today.get(Calendar.YEAR);
    int monthOfYear = today.get(Calendar.MONTH);
    int dayOfMonth = today.get(Calendar.DAY_OF_MONTH);
    int hourOfDay = today.get(Calendar.HOUR_OF_DAY);
    int minutes = today.get(Calendar.MINUTE);

    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(year, monthOfYear, dayOfMonth);
    dialogPositiveButton().performClick();

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(hourOfDay, minutes);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText("")));
  }

  @Test
  @Ignore("Not implemented")
  public void choosingStartDateInvalidateAStartTimeInPast() {
    Calendar today = Calendar.getInstance();
    today.add(Calendar.MINUTE, -10);
    int year = today.get(Calendar.YEAR);
    int monthOfYear = today.get(Calendar.MONTH) + 1;
    int dayOfMonth = today.get(Calendar.DAY_OF_MONTH);
    int hourOfDay = today.get(Calendar.HOUR_OF_DAY);
    int minutes = today.get(Calendar.MINUTE);

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(hourOfDay, minutes);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText(String.format(TIME_FORMAT, hourOfDay, minutes))));

    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(year, monthOfYear, dayOfMonth);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText("")));
  }

  private void pickValidDateAndTime() {
    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();

    endDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();

    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS + 1, MINUTES);
    dialogPositiveButton().performClick();
  }

  @Test
  public void multiplePluralityQuestionsTest() {
    setupViewModel();

    electionName().perform(click(), typeText(ELECTION_NAME), closeSoftKeyboard());
    pickValidDateAndTime();

    // Add Question 1, with 3 ballots options, no write in
    questionText().perform(click(), typeText("Question 1"), closeSoftKeyboard());
    addBallot().perform(click());

    for (int i = 0; i < 3; ++i) {
      ballotOptionAtPosition(i).perform(click(), typeText("answer 1." + i), closeSoftKeyboard());
    }

    // Add Question 2, with 2 ballots options, with write in
    addQuestion().perform(click());
    questionText().perform(click(), typeText("Question 2"), closeSoftKeyboard());
    writeIn().perform(click());

    for (int i = 0; i < 2; ++i) {
      ballotOptionAtPosition(i).perform(click(), typeText("answer 2." + i), closeSoftKeyboard());
    }

    // Submit and intercept the ElectionSetup message
    long minCreation = Instant.now().getEpochSecond();
    submit().perform(click());
    ElectionSetup electionSetup = getInterceptedElectionSetupMsg();
    long maxCreation = Instant.now().getEpochSecond();

    // Check that the creation time was when we submit the ElectionSetup
    assertTrue(minCreation <= electionSetup.getCreation());
    assertTrue(maxCreation >= electionSetup.getCreation());

    // Check the start/end time
    Calendar calendar = Calendar.getInstance();
    calendar.set(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOURS, MINUTES, 0);
    long expectedStartTime = calendar.toInstant().getEpochSecond();
    calendar.set(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOURS + 1, MINUTES, 0);
    long expectedEndTime = calendar.toInstant().getEpochSecond();

    assertEquals(expectedStartTime, electionSetup.getStartTime());
    assertEquals(expectedEndTime, electionSetup.getEndTime());

    assertEquals(ELECTION_NAME, electionSetup.getName());
    assertEquals(LAO.getId(), electionSetup.getLao());

    List<ElectionQuestion> questionList = electionSetup.getQuestions();
    assertEquals(2, questionList.size());
    ElectionQuestion question1 = questionList.get(0);
    ElectionQuestion question2 = questionList.get(1);

    // Check the Questions 1
    assertEquals("Question 1", question1.getQuestion());
    assertFalse(question1.getWriteIn());
    List<String> ballotOptions1 = question1.getBallotOptions();
    assertEquals(3, ballotOptions1.size());
    for (int i = 0; i < 3; ++i) {
      assertEquals("answer 1." + i, ballotOptions1.get(i));
    }

    // Check the Questions 2
    assertEquals("Question 2", question2.getQuestion());
    // assertTrue(question2.getWriteIn());
    List<String> ballotOptions2 = question2.getBallotOptions();
    assertEquals(2, ballotOptions2.size());
    for (int i = 0; i < 2; ++i) {
      assertEquals("answer 2." + i, ballotOptions2.get(i));
    }
  }

  @Test
  public void cannotSubmitWithoutElectionNameElectionTest() {
    setupViewModel();

    pickValidDateAndTime();

    // Add Question 1, with 2 ballots options, no write in
    questionText().perform(click(), typeText("Question 1"), closeSoftKeyboard());
    for (int i = 0; i < 2; ++i) {
      ballotOptionAtPosition(i).perform(click(), typeText("answer 1." + i), closeSoftKeyboard());
    }

    submit().check(matches(isNotEnabled()));
    electionName().perform(click(), typeText(ELECTION_NAME), closeSoftKeyboard());
    submit().check(matches(isEnabled()));
  }

  @Test
  public void canWithoutDateAndTimeTest() {
    // Since now suggested start and end time are provided
    setupViewModel();

    electionName().perform(click(), typeText(ELECTION_NAME), closeSoftKeyboard());
    // Add Question 1, with 2 ballots options, no write in
    questionText().perform(click(), typeText("Question 1"), closeSoftKeyboard());
    for (int i = 0; i < 2; ++i) {
      ballotOptionAtPosition(i).perform(click(), typeText("answer 1." + i), closeSoftKeyboard());
    }
    submit().check(matches(isEnabled()));
  }

  @Test
  public void cannotSubmitWithoutQuestionTest() {
    setupViewModel();

    electionName().perform(click(), typeText(ELECTION_NAME), closeSoftKeyboard());
    pickValidDateAndTime();

    // add 2 ballots options
    for (int i = 0; i < 2; ++i) {
      ballotOptionAtPosition(i).perform(click(), typeText("answer 1." + i), closeSoftKeyboard());
    }

    submit().check(matches(isNotEnabled()));
    questionText().perform(click(), typeText("Question 1"), closeSoftKeyboard());
    submit().check(matches(isEnabled()));
  }

  @Test
  public void cannotSubmitWithoutAllBallotTest() {
    setupViewModel();

    electionName().perform(click(), typeText(ELECTION_NAME), closeSoftKeyboard());
    pickValidDateAndTime();

    questionText().perform(click(), typeText("Question 1"), closeSoftKeyboard());
    ballotOptionAtPosition(0).perform(click(), typeText("answer 1.0"), closeSoftKeyboard());

    submit().check(matches(isNotEnabled()));
    ballotOptionAtPosition(1).perform(click(), typeText("answer 1.1"), closeSoftKeyboard());
    submit().check(matches(isEnabled()));
  }

  @Test
  public void cannotSubmitWithIdenticalBallotTest() {
    setupViewModel();

    electionName().perform(click(), typeText(ELECTION_NAME), closeSoftKeyboard());
    pickValidDateAndTime();

    // Add Question 1, with 2 identical ballots options, no write in
    questionText().perform(click(), typeText("Question 1"), closeSoftKeyboard());
    for (int i = 0; i < 2; ++i) {
      ballotOptionAtPosition(i).perform(click(), typeText("answer 1.0"), closeSoftKeyboard());
    }

    submit().check(matches(isNotEnabled()));
  }

  /** Basic test for sanity of spinner content */
  @Test
  public void canChooseVotingVersion() {
    setupViewModel();

    // By default, the spinner is set to OPEN_BALLOT
    versionChoice().perform(click());
    onData(anything()).atPosition(0).perform(click());
    versionChoice()
        .check(
            matches(
                withSpinnerText(
                    containsString(ElectionVersion.OPEN_BALLOT.getStringBallotVersion()))));

    versionChoice().perform(click());
    onData(anything()).atPosition(1).perform(click());
    versionChoice()
        .check(
            matches(
                withSpinnerText(
                    containsString(ElectionVersion.SECRET_BALLOT.getStringBallotVersion()))));
  }
}
