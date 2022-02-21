package com.github.dedis.popstellar.model.objects.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class EventCategoryTest {

  @Test
  public void toStringTest() {
    assertThat(EventCategory.PAST.toString(), is("Past Events"));
    assertThat(EventCategory.PRESENT.toString(), is("Present Events"));
    assertThat(EventCategory.FUTURE.toString(), is("Future Events"));
  }
}
