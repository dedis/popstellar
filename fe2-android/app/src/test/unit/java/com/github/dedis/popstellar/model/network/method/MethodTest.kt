package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.network.method.Method.Companion.find
import org.junit.Assert
import org.junit.Test

class MethodTest {
  @Test
  fun dataClassTest() {
    Assert.assertEquals(Subscribe::class.java, Method.SUBSCRIBE.dataClass)
    Assert.assertEquals(Unsubscribe::class.java, Method.UNSUBSCRIBE.dataClass)
    Assert.assertEquals(Publish::class.java, Method.PUBLISH.dataClass)
    Assert.assertEquals(Broadcast::class.java, Method.MESSAGE.dataClass)
    Assert.assertEquals(Catchup::class.java, Method.CATCHUP.dataClass)
  }

  @Test
  fun expectResultTest() {
    Assert.assertTrue(Method.SUBSCRIBE.expectResult())
    Assert.assertTrue(Method.UNSUBSCRIBE.expectResult())
    Assert.assertTrue(Method.PUBLISH.expectResult())
    Assert.assertFalse(Method.MESSAGE.expectResult())
    Assert.assertTrue(Method.CATCHUP.expectResult())
  }

  @Test
  fun findTest() {
    Assert.assertNull(find("not a method"))
    Assert.assertEquals(Method.SUBSCRIBE, find("subscribe"))
  }
}
