package com.github.dedis.popstellar.model.objects.event;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EventTypeTest {

  @Test
  public void getSuffixTest() {
    assertThat(EventType.MEETING.suffix, is("M"));
    assertThat(EventType.ROLL_CALL.suffix, is("R"));
    assertThat(EventType.POLL.suffix, is("P"));
    assertThat(EventType.DISCUSSION.suffix, is("D"));
  }
}
