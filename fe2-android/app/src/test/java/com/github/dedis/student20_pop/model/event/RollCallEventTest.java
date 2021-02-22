package com.github.dedis.student20_pop.model.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import androidx.databinding.ObservableArrayList;
import com.github.dedis.student20_pop.model.Keys;
import java.time.Instant;
import org.junit.Test;

public class RollCallEventTest {

  private final String name1 = "Meeting 1";
  private final String name2 = "Meeting 2";
  private final long startTime = Instant.now().getEpochSecond();
  private final long endTime = Instant.now().getEpochSecond();
  private final String lao = new Keys().getPublicKey();
  private final ObservableArrayList<String> attendees = new ObservableArrayList<>();
  private final String location = "EPFL";
  private final String description = "Important information";
  private final RollCallEvent event1 =
      new RollCallEvent(name1, startTime, endTime, lao, attendees, location, description);
  private final RollCallEvent event2 =
      new RollCallEvent(name2, startTime, endTime, lao, attendees, location, description);

  @Test
  public void createEventWithNullParametersTest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RollCallEvent(name2, startTime, endTime, lao, null, location, description));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RollCallEvent(name2, startTime, endTime, lao, attendees, location, null));
  }

  @Test
  public void getStartTimeTest() {
    assertThat(event1.getStartTime(), is(startTime));
  }

  @Test
  public void getEndTimeTest() {
    assertThat(event1.getEndTime(), is(endTime));
  }

  @Test
  public void getDescriptionTest() {
    assertThat(event1.getDescription(), is(description));
  }

  @Test
  public void addAttendeeTest() {
    String attendee = new Keys().getPublicKey();
    event1.addAttendee(attendee);
    attendees.add(attendee);
    assertThat(event1.getAttendees(), is(attendees));
    assertThat(
        event1.addAttendee(attendee),
        is(RollCallEvent.AddAttendeeResult.ADD_ATTENDEE_ALREADY_EXISTS));
  }

  @Test
  public void equalsTest() {
    assertEquals(event1, event1);
    assertNotEquals(event1, event2);
  }

  @Test
  public void hashCodeTest() {
    assertEquals(event1.hashCode(), event1.hashCode());
    assertNotEquals(event1.hashCode(), event2.hashCode());
  }
}
