package com.github.dedis.popstellar.model.objects;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MeetingTest {

  private static final String LAO_ID = "LAO_ID";
  private static final String ID = "ID";
  private static final String NAME = "MEETING_NAME";
  private static final String LOCATION = "Test Location";
  private static final long CREATION = System.currentTimeMillis() / 1000;
  private static final long START = CREATION + 1;
  private static final long END = START + 1;
  private static final long LAST_MODIFIED = CREATION;
  private static final String MODIFICATION_ID = "MOD_ID";
  private static final List<String> MODIFICATION_SIGNATURES = new ArrayList<>();

  @Test
  public void getCorrectId() {
    assertEquals(
        Hash.hash("M", LAO_ID, Long.toString(CREATION), NAME),
        Meeting.generateCreateMeetingId(LAO_ID, CREATION, NAME));
  }

  @Test
  public void testCreateState() {
    Meeting meeting =
        new Meeting(
            ID,
            NAME,
            CREATION,
            START + 10,
            END + 10,
            LOCATION,
            LAST_MODIFIED,
            MODIFICATION_ID,
            MODIFICATION_SIGNATURES);
    assertEquals(EventState.CREATED, meeting.getState());
  }

  @Test
  public void testOpenState() {
    Meeting meeting =
        new Meeting(
            ID,
            NAME,
            CREATION,
            CREATION,
            END,
            LOCATION,
            LAST_MODIFIED,
            MODIFICATION_ID,
            MODIFICATION_SIGNATURES);
    assertEquals(EventState.OPENED, meeting.getState());
  }

  public void testClosedState() {
    Meeting meeting =
        new Meeting(
            ID,
            NAME,
            CREATION,
            CREATION,
            CREATION,
            LOCATION,
            LAST_MODIFIED,
            MODIFICATION_ID,
            MODIFICATION_SIGNATURES);
    assertEquals(EventState.CLOSED, meeting.getState());
  }
}
