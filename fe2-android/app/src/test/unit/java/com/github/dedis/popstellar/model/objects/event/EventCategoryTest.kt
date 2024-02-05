package com.github.dedis.popstellar.model.objects.event

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

class EventCategoryTest {
  @Test
  fun toStringTest() {
    MatcherAssert.assertThat(EventCategory.PAST.toString(), CoreMatchers.`is`("Past Events"))
    MatcherAssert.assertThat(EventCategory.PRESENT.toString(), CoreMatchers.`is`("Present Events"))
    MatcherAssert.assertThat(EventCategory.FUTURE.toString(), CoreMatchers.`is`("Future Events"))
  }
}