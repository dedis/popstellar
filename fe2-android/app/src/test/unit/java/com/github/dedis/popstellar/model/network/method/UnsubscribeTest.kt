package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import org.junit.Assert
import org.junit.Test

class UnsubscribeTest {
  @Test
  fun method() {
    val unsubscribe = Unsubscribe(fromString("root/stuff"), 3)
    Assert.assertEquals("unsubscribe", unsubscribe.method)
  }
}
