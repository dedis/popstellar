package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import org.junit.Assert
import org.junit.Test

class SubscribeTest {
  @Test
  fun methodTest() {
    val subscribe = Subscribe(fromString("root/stuff"), 2)
    Assert.assertEquals("subscribe", subscribe.method)
  }
}
