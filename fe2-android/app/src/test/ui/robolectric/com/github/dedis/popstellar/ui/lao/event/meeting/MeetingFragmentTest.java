package com.github.dedis.popstellar.ui.lao.event.meeting;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.Meeting;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MeetingRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.MessageSenderHelper;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.containerId;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static com.github.dedis.popstellar.testutils.pages.lao.MeetingFragmentPageObject.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class MeetingFragmentTest {
  private static final String LAO_NAME = "lao";
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, SENDER, 10223421);
  private static final String LAO_ID = LAO.getId();
  private static final String MEETING_TITLE = "Title";
  private static final long CREATION = 10323411;
  private static final long START = CREATION + 10;
  private static final long END = START + 10;
  private static final String LOCATION = "EPFL";
  private static final String MODIFICATION_ID = "MOD_ID";
  private static final List<String> MODIFICATION_SIGNATURES = new ArrayList<>();
  private static final BehaviorSubject<LaoView> laoSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));

  private static final DateFormat DATE_FORMAT =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);

  private static final String MEETING_ID =
      Meeting.generateCreateMeetingId(LAO_ID, CREATION, MEETING_TITLE);

  private static final Meeting MEETING =
      new Meeting(
          MEETING_ID,
          MEETING_TITLE,
          CREATION,
          START,
          END,
          LOCATION,
          CREATION,
          MODIFICATION_ID,
          MODIFICATION_SIGNATURES);

  @Inject MeetingRepository meetingRepository;

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
        protected void before() throws UnknownLaoException {
          hiltRule.inject();
          when(laoRepo.getLaoObservable(anyString())).thenReturn(laoSubject);
          when(laoRepo.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          meetingRepository.updateMeeting(LAO_ID, MEETING);

          when(keyManager.getMainPublicKey()).thenReturn(SENDER);

          messageSenderHelper.setupMock();
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, MeetingFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(laoIdExtra(), LAO_ID).build(),
          containerId(),
          MeetingFragment.class,
          () -> MeetingFragment.newInstance(MEETING.getId()),
          new BundleBuilder().putString(Constants.MEETING_ID, MEETING.getId()).build());

  @Test
  public void rollCallTitleMatches() {
    meetingTitle().check(matches(withText(MEETING_TITLE)));
  }

  @Test
  public void datesDisplayedMatches() {
    Date startTime = new Date(MEETING.getStartTimestampInMillis());
    Date endTime = new Date(MEETING.getEndTimestampInMillis());
    String startTimeText = DATE_FORMAT.format(startTime);
    String endTimeText = DATE_FORMAT.format(endTime);

    meetingStartTime().check(matches(withText(startTimeText)));
    meetingEndTime().check(matches(withText(endTimeText)));
  }

  @Test
  public void statusCreatedTest() {
    long timeSec = System.currentTimeMillis() / 1000;
    Meeting meeting =
        new Meeting(
            MEETING_ID,
            MEETING_TITLE,
            timeSec,
            timeSec + 10,
            timeSec + 20,
            LOCATION,
            timeSec,
            MODIFICATION_ID,
            MODIFICATION_SIGNATURES);
    meetingRepository.updateMeeting(LAO_ID, meeting);
    meetingStatusText().check(matches(withText("Not yet opened")));
  }

  @Test
  public void statusOpenedTest() {
    long timeSec = System.currentTimeMillis() / 1000;
    Meeting meeting =
        new Meeting(
            MEETING_ID,
            MEETING_TITLE,
            timeSec,
            timeSec,
            timeSec + 10,
            LOCATION,
            timeSec,
            MODIFICATION_ID,
            MODIFICATION_SIGNATURES);
    meetingRepository.updateMeeting(LAO_ID, meeting);

    meetingStatusText().check(matches(withText("Open")));
  }

  @Test
  public void statusClosedTest() {
    long timeSec = System.currentTimeMillis() / 1000 - 10;
    Meeting meeting =
        new Meeting(
            MEETING_ID,
            MEETING_TITLE,
            timeSec,
            timeSec,
            timeSec + 5,
            LOCATION,
            timeSec,
            MODIFICATION_ID,
            MODIFICATION_SIGNATURES);
    meetingRepository.updateMeeting(LAO_ID, meeting);

    meetingStatusText().check(matches(withText("Closed")));
  }

  @Test
  public void locationVisibilityTest() {
    // Here the location text should be visible as non empty
    meetingLocationText().check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
  }
}
