package com.github.dedis.popstellar.model.objects.event

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

class EventTypeTest {
  @Test
  fun suffixTest() {
    MatcherAssert.assertThat(EventType.MEETING.suffix, CoreMatchers.`is`("M"))
    MatcherAssert.assertThat(EventType.ROLL_CALL.suffix, CoreMatchers.`is`("R"))
    MatcherAssert.assertThat(EventType.POLL.suffix, CoreMatchers.`is`("P"))
    MatcherAssert.assertThat(EventType.DISCUSSION.suffix, CoreMatchers.`is`("D"))
  }
}
