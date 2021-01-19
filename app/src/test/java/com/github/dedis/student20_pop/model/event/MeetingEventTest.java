package com.github.dedis.student20_pop.model.event;

import com.github.dedis.student20_pop.model.Keys;
import org.junit.Test;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class MeetingEventTest {

  private final String name1 = "Meeting 1";
  private final String name2 = "Meeting 2";
  private final long startTime = Instant.now().getEpochSecond();
  private final long endTime = Instant.now().getEpochSecond();
  private final String lao = new Keys().getPublicKey();
  private final String location = "EPFL";
  private final String description = "Important information";
  private final MeetingEvent event1 =
      new MeetingEvent(name1, startTime, endTime, lao, location, description);
  private final MeetingEvent event2 =
      new MeetingEvent(name2, startTime, endTime, lao, location, description);

  @Test
  public void createEventWithNullParametersTest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeetingEvent(name1, startTime, endTime, lao, location, null));
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
