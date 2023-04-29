package com.github.dedis.popstellar.model.network.method.message.data.meeting;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.junit.Assert.*;

public class StateMeetingTest {
  private static final String LAO_ID =
      Lao.generateLaoId(generatePublicKey(), Instant.now().getEpochSecond(), "Lao name");
  private static final String NAME = "name";
  private static final String LOCATION = "location";
  private static final String MODIFICATION_ID = "modif id";
  private static final long CREATION = Instant.now().getEpochSecond();
  private static final long START = CREATION + 1;
  private static final long LAST_MODIFIED = START + 1;
  private static final long END = START + 5;
  private static final String SIGNATURE_1 = "signature 1";
  private static final String SIGNATURE_2 = "signature 2";
  private static final String ID =
      Hash.hash(EventType.MEETING.getSuffix(), LAO_ID, Long.toString(CREATION), NAME);

  private static final List<String> MODIFICATION_SIGNATURES =
      Arrays.asList(SIGNATURE_1, SIGNATURE_2);
  private static final StateMeeting STATE_MEETING =
      new StateMeeting(
          LAO_ID,
          ID,
          NAME,
          CREATION,
          LAST_MODIFIED,
          LOCATION,
          START,
          END,
          MODIFICATION_ID,
          MODIFICATION_SIGNATURES);

  @Test
  public void getId() {
    assertEquals(ID, STATE_MEETING.getId());
  }

  @Test
  public void getName() {
    assertEquals(NAME, STATE_MEETING.getName());
  }

  @Test
  public void getCreation() {
    assertEquals(CREATION, STATE_MEETING.getCreation());
  }

  @Test
  public void getLastModified() {
    assertEquals(LAST_MODIFIED, STATE_MEETING.getLastModified());
  }

  @Test
  public void getLocation() {
    assertEquals(LOCATION, STATE_MEETING.getLocation().orElse(""));
  }

  @Test
  public void getStart() {
    assertEquals(START, STATE_MEETING.getStart());
  }

  @Test
  public void getEnd() {
    assertEquals(END, STATE_MEETING.getEnd());
  }

  @Test
  public void getModificationId() {
    assertEquals(MODIFICATION_ID, STATE_MEETING.getModificationId());
  }

  @Test
  public void getModificationSignatures() {
    assertEquals(MODIFICATION_SIGNATURES, STATE_MEETING.getModificationSignatures());
  }

  @Test
  public void getObject() {
    assertEquals("meeting", STATE_MEETING.getObject());
  }

  @Test
  public void getAction() {
    assertEquals(Action.STATE.getAction(), STATE_MEETING.getAction());
  }

  @Test
  public void testEquals() {
    assertEquals(STATE_MEETING, STATE_MEETING);
    assertNotEquals(null, STATE_MEETING);

    StateMeeting stateMeeting =
        new StateMeeting(
            LAO_ID,
            ID,
            NAME,
            CREATION,
            LAST_MODIFIED,
            LOCATION,
            START,
            END,
            MODIFICATION_ID,
            MODIFICATION_SIGNATURES);
    assertEquals(STATE_MEETING, stateMeeting);
  }

  @Test
  public void testHashCode() {
    assertEquals(
        Objects.hash(
            ID,
            NAME,
            CREATION,
            LAST_MODIFIED,
            LOCATION,
            START,
            END,
            MODIFICATION_ID,
            MODIFICATION_SIGNATURES),
        STATE_MEETING.hashCode());
  }

  @Test
  public void testToString() {
    String expected =
        String.format(
            "StateMeeting{id='%s', name='%s', creation=%d, lastModified=%d, location='%s', start=%d, end=%d, modificationId='%s', modificationSignatures=%s}",
            ID,
            NAME,
            CREATION,
            LAST_MODIFIED,
            LOCATION,
            START,
            END,
            MODIFICATION_ID,
            MODIFICATION_SIGNATURES);
    assertEquals(expected, STATE_MEETING.toString());
  }

  @Test
  public void nonCoherentIdInConstructorThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new StateMeeting(
                LAO_ID,
                "random id",
                NAME,
                CREATION,
                LAST_MODIFIED,
                LOCATION,
                START,
                END,
                MODIFICATION_ID,
                MODIFICATION_SIGNATURES));
  }
}
