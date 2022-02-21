package com.github.dedis.popstellar.model.objects.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class EventTypeTest {

  @Test
  public void getSuffixTest() {
    assertThat(EventType.MEETING.getSuffix(), is("M"));
    assertThat(EventType.ROLL_CALL.getSuffix(), is("R"));
    assertThat(EventType.POLL.getSuffix(), is("P"));
    assertThat(EventType.DISCUSSION.getSuffix(), is("D"));
  }
}
