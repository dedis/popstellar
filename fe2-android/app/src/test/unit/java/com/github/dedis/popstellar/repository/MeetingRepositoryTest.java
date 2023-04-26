package com.github.dedis.popstellar.repository;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Meeting;
import com.github.dedis.popstellar.utility.error.UnknownMeetingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;

import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MeetingRepositoryTest {
  private static final MeetingRepository meetingRepository = new MeetingRepository();
  private static final String LAO_ID = "LAO_ID";
  private static final String ID = "ID";
  private static final String NAME = "MEETING_NAME";
  private static final String LOCATION = "Test Location";
  private static final long CREATION = System.currentTimeMillis();
  private static final long START = CREATION + 1000;
  private static final long END = START + 1000;
  private static final long LAST_MODIFIED = CREATION;
  private static final String MODIFICATION_ID = "MOD_ID";
  private static final List<String> MODIFICATION_SIGNATURES = new ArrayList<>();
  private static final Meeting meeting =
      new Meeting(
          ID,
          NAME,
          CREATION,
          START,
          END,
          LOCATION,
          LAST_MODIFIED,
          MODIFICATION_ID,
          MODIFICATION_SIGNATURES);

  @Before
  public void setUp() {
    meetingRepository.updateMeeting(LAO_ID, meeting);
  }

  @Test
  public void addMeetingAddsMeetingToRepository() throws UnknownMeetingException {
    Meeting retrievedMeeting = meetingRepository.getMeetingWithId(LAO_ID, ID);
    assertEquals(meeting, retrievedMeeting);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addMeetingThrowsExceptionWhenLaoIdIsNull() {
    meetingRepository.updateMeeting(null, meeting);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addMeetingThrowsExceptionWhenMeetingIsNull() {
    meetingRepository.updateMeeting(LAO_ID, null);
  }

  @Test(expected = UnknownMeetingException.class)
  public void getMeetingWithIdThrowsExceptionWhenMeetingDoesNotExist()
      throws UnknownMeetingException {
    meetingRepository.getMeetingWithId(LAO_ID, ID + "2");
  }

  @Test
  public void getMeetingsObservableInLaoReturnsObservableOfMeetings() {
    TestObserver<Set<Meeting>> observer = new TestObserver<>();
    meetingRepository.getMeetingsObservableInLao(LAO_ID).subscribe(observer);

    observer.awaitCount(1);
    Set<Meeting> meetings = observer.values().get(0);
    assertTrue(meetings.contains(meeting));
  }
}
